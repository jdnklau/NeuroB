package de.hhu.stups.neurob.training.generation.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.model.eventb.Context;
import de.prob.statespace.OperationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.animator.command.BeforeAfterPredicateCommand;
import de.prob.animator.command.WeakestPreconditionCommand;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.model.classicalb.Assertion;
import de.prob.model.classicalb.Property;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Axiom;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Guard;
import de.prob.model.representation.Invariant;

/**
 * Collection of invariants, properties, preconditions, etc. found in a
 * B machine.
 */
public class PredicateCollection {
    private List<BPredicate> invariants;
    private List<String> operations;
    private Map<String, List<BPredicate>> preconditions;
    private List<BPredicate> properties; // also contains axioms of EventB
    private List<BPredicate> assertions; // also contains theorems of EventB
    private Map<String, BPredicate> beforeAfterPredicates;
    private Map<String, Map<BPredicate, BPredicate>> weakestPreconditions;
    private Map<String, BPredicate> weakestFullPreconditions;
    private Map<BPredicate, BPredicate> primedInvariants;
    private Map<String, List<BPredicate>> primedPreconditions;

    private MachineAccess bMachine;

    private final boolean cleanAst;

    private static final Logger log =
            LoggerFactory.getLogger(PredicateCollection.class);

    public PredicateCollection(MachineAccess bMachine) {
        this(bMachine, false);
    }

    public PredicateCollection(MachineAccess bMachine, boolean cleanAst) {
        this.bMachine = bMachine;

        this.cleanAst = cleanAst;

        invariants = new ArrayList<>();
        operations = new ArrayList<>();
        preconditions = new HashMap<>();
        properties = new ArrayList<>();
        assertions = new ArrayList<>();
        beforeAfterPredicates = new HashMap<>();
        weakestPreconditions = new HashMap<>();
        weakestFullPreconditions = new HashMap<>();
        primedInvariants = new HashMap<>();
        primedPreconditions = new HashMap<>();

        collectFromMachine(bMachine);

        // for EventB, check Context as well
        // TODO: Check whether each EventB machine only has one context at max
        // maybe we do not need to loop over all;
        // also check how a state space behaves for refinements

        if (bMachine.getMachineType() == MachineType.EVENTB) {
            for (Context bcc : bMachine.getStateSpace().getModel().getChildrenOfType(Context.class)) {
                collectFromContext(bcc);
            }
        }

    }

    private void collectFromMachine(MachineAccess bMachine) {
        AbstractElement comp = bMachine.getStateSpace().getMainComponent();
        // properties
        log.trace("Collecting properties from {}", bMachine.getSource());
        for (Property x : comp.getChildrenOfType(Property.class)) {
            properties.add(BPredicate.of(x.getFormula().getCode()));
        }

        // add invariants
        log.trace("Collecting invariants from {}", bMachine.getSource());
        for (Invariant x : comp.getChildrenOfType(Invariant.class)) {
            if (x.isTheorem())
                assertions.add(BPredicate.of(x.getFormula().getCode()));
            else
                invariants.add(BPredicate.of(x.getFormula().getCode()));
        }
        invariants = cleanUpPredicates(bMachine, invariants);

        // Conjunct invariants if more than one
        BPredicate invariantConcat = null;
        if (invariants.size() > 1) {
            invariantConcat =
                    FormulaGenerator.getPredicateConjunction(invariants);
//            invariants.add(invariantConcat);
        }

        BPredicate fullInv = FormulaGenerator.getPredicateConjunction(invariants);

        log.trace("Collecting assertions from {}", bMachine.getSource());
        for (Assertion x : comp.getChildrenOfType(Assertion.class)) {
            assertions.add(BPredicate.of(x.getFormula().getCode()));
        }
        assertions = cleanUpPredicates(bMachine, assertions);

        // for each event collect preconditions
        log.trace("Collecting operations and preconditions from {}", bMachine.getSource());
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            if (x.getName().equals("INITIALISATION"))
                continue; // None for initialisation
            operations.add(x.getName());

            log.trace("Collecting preconditions for {} in {}", x.getName(), bMachine.getSource());
            ArrayList<BPredicate> event = new ArrayList<>();
            for (Guard g : x.getChildrenOfType(Guard.class)) {
                event.add(BPredicate.of(g.getFormula().getCode()));
            }
            if (!event.isEmpty())
                preconditions.put(x.getName(), event);
        }

        // set up invariants as commands for below
        Map<BPredicate, IBEvalElement> invCmds = new HashMap<>();
        for (BPredicate inv : invariants) {
            try {
                IBEvalElement cmd = Backend.generateBFormula(inv, bMachine);
                invCmds.put(inv, cmd);
            } catch (FormulaException e) {
                log.warn("Could not set up EvalElement from {} in {} for "
                         + "weakest precondition calculation or priming",
                        inv, bMachine.getSource(), e);
            }
        }
        IBEvalElement fullInvCmd = null;
        if (invariantConcat != null) {
            try {
                fullInvCmd = Backend.generateBFormula(invariantConcat, bMachine);
            } catch (FormulaException e) {
                log.warn("Could not set up EvalElement from invariant concatenation for "
                         + "weakest precondition calculation or priming in {}",
                        bMachine.getSource(), e);
            }
        }

        // weakest preconditions for each invariant
        log.trace("Building weakest preconditions for {}", bMachine.getSource());
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            if (x.getName().equals("INITIALISATION"))
                continue; // None for initialisation

            Map<BPredicate, BPredicate> wpcs = new HashMap<>();
            for (BPredicate inv : invCmds.keySet()) {
                IBEvalElement invCmd = invCmds.get(inv);

                try {
                    WeakestPreconditionCommand wpcc =
                            new WeakestPreconditionCommand(x.getName(), invCmd);
                    bMachine.execute(wpcc);
                    // FIXME: Erase comment, probably should not be returned by ProB to begin with
                    String code = wpcc.getWeakestPrecondition().getCode()
                            .replaceAll("/\\*.*\\*/ *", "");
                    wpcs.put(inv, BPredicate.of(code));
                } catch (Exception e) {
                    log.warn("Could not build weakest precondition "
                             + "for {} by operation {} in {}.",
                            invCmd.getCode(), x.getName(), bMachine.getSource(), e);
                }

            }
            weakestPreconditions.put(x.getName(), wpcs);

            // Full precondition
            if (fullInvCmd != null) {
                try {
                    WeakestPreconditionCommand wpcc =
                            new WeakestPreconditionCommand(x.getName(), fullInvCmd);
                    bMachine.execute(wpcc);
                    // FIXME: Erase comment, probably should not be returned by ProB to begin with
                    String code = wpcc.getWeakestPrecondition().getCode()
                            .replaceAll("/\\*.*\\*/ *", "");
                    weakestFullPreconditions.put(x.getName(), BPredicate.of(code));
                } catch (Exception e) {
                    log.warn("Could not build weakest precondition"
                             + "for full invariant {} by operation {} in {}.",
                            fullInv, x.getName(), bMachine.getSource(), e);
                }
            }

        }

        // Before/After predicates
        OperationInfo.Type operationType = bMachine.getMachineType() == MachineType.CLASSICALB
                ? OperationInfo.Type.CLASSICAL_B
                : OperationInfo.Type.EVENTB;
        log.trace("Building before/after predicates");
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            if (x.getName().equals("INITIALISATION"))
                continue; // None for initialisation
            try {
                BeforeAfterPredicateCommand bapc =
                        new BeforeAfterPredicateCommand(x.getName(), operationType);
                bMachine.execute(bapc);
                // FIXME: Erase comment, probably should not be returned by ProB to begin with
                String code = bapc.getBeforeAfterPredicate().getCode()
                        .replaceAll("/\\*.*\\*/ *", "");
                beforeAfterPredicates.put(x.getName(), BPredicate.of(code));
            } catch (Exception e) {
                log.warn("Could not build Before After Predicate for event {} in {}",
                        x.getName(), bMachine.getSource(), e);
            }
        }

        log.trace("Priming preconditions");
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            if (x.getName().equals("INITIALISATION"))
                continue; // None for initialisation
            List<BPredicate> primedPrecs = new ArrayList<>();
            if (preconditions.containsKey(x.getName())) {
                for (BPredicate prec : preconditions.get(x.getName())) {
                    try {
                        IBEvalElement cmd = Backend.generateBFormula(prec, bMachine);
                        BPredicate code = FormulaGenerator.generatePrimedPredicate(bMachine, cmd);
                        primedPrecs.add(code);
                    } catch (Exception e) {
                        log.warn("Could not prime precondition for event {} in {}",
                                x.getName(), bMachine.getSource(), e);
                    }
                }
            }
            primedPreconditions.put(x.getName(), primedPrecs);
        }

        // primed invariants
        log.trace("Building primed invariants for {}", bMachine.getMachineType());
        for (BPredicate inv : invCmds.keySet()) {
            IBEvalElement invCmd = invCmds.get(inv);
            try {
                BPredicate primedInv = FormulaGenerator.generatePrimedPredicate(bMachine, invCmd);
                primedInvariants.put(inv, primedInv);
            } catch (Exception e) {
                log.warn("Could not build primed invariant for {} from {}", inv, bMachine.getSource(), e);
            }
        }

//        // One fully primed invariant, please
//        try {
//            BPredicate primedInv = FormulaGenerator.generatePrimedPredicate(bMachine, fullInvCmd);
//            primedInvariants.put(fullInv, primedInv);
//        } catch (Exception e) {
//            log.warn("Could not build primed invariant from {}", fullInv, e);
//        }

    }

    private void collectFromContext(Context bcc) {
        // axioms
        log.trace("Collecting axioms");
        for (Axiom x : bcc.getChildrenOfType(Axiom.class)) {
            properties.add(BPredicate.of(x.getFormula().getCode()));
        }

    }

    /**
     * Cleans the AST of the given Predicates.
     *
     * @param mch
     * @param predicates
     *
     * @return
     */
    private List<BPredicate> cleanUpPredicates(MachineAccess mch, List<BPredicate> predicates) {
        if (!cleanAst) {
            return predicates;
        }

        List<BPredicate> cleaned = new ArrayList<>();

        for (BPredicate p : predicates) {
            try {
                cleaned.add(FormulaGenerator.cleanupAst(bMachine, p));
            } catch (FormulaException e) {
                log.warn("Unable to cleanup ast of {}", p, e);
            }
        }

        return cleaned;
    }

    /**
     * @return A Map, that pairs an event name (key) with a list of its
     *         respective preconditions
     */
    public Map<String, List<BPredicate>> getPreconditions() {
        return preconditions;
    }

    public List<BPredicate> getInvariants() {
        return invariants;
    }

    public List<String> getOperationNames() {
        return operations;
    }

    public List<BPredicate> getProperties() {
        return properties;
    }

    public List<BPredicate> getAssertions() {
        return assertions;
    }

    /**
     * @return A map, pairing each event (key) with its respective before/after
     *         predicate.
     */
    public Map<String, BPredicate> getBeforeAfterPredicates() {
        return beforeAfterPredicates;
    }

    /**
     * Returns a map, that pairs an event name with a map of invariants to
     * respective weakest precondition.
     * <p>
     * Each event name hereby references a map with invariants as keys and the
     * weakest precondition
     * of the invariant wrt the event as values.
     *
     * @return A map, that pairs an event with a map of invariants to weakest
     *         precondition.
     */
    public Map<String, Map<BPredicate, BPredicate>> getWeakestPreConditions() {
        return weakestPreconditions;
    }

    /**
     * @return A map of invariants to their primed version
     */
    public Map<BPredicate, BPredicate> getPrimedInvariants() {
        return primedInvariants;
    }


    public MachineType getMachineType() {
        return bMachine.getMachineType();
    }

    public MachineAccess getBMachine() {
        return bMachine;
    }

    public List<String> getOperations() {
        return operations;
    }

    public Map<String, List<BPredicate>> getPrimedPreconditions() {
        return primedPreconditions;
    }

    public Map<String, BPredicate> getWeakestFullPreconditions() {
        return weakestFullPreconditions;
    }
}
