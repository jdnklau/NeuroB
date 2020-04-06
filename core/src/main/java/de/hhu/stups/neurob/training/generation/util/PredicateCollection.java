package de.hhu.stups.neurob.training.generation.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.prob.model.eventb.Context;
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
    private Map<BPredicate, BPredicate> primedInvariants;

    private MachineAccess bMachine;

    private static final Logger log =
            LoggerFactory.getLogger(PredicateCollection.class);

    public PredicateCollection(MachineAccess bMachine) {
        this.bMachine = bMachine;

        invariants = new ArrayList<>();
        operations = new ArrayList<>();
        preconditions = new HashMap<>();
        properties = new ArrayList<>();
        assertions = new ArrayList<>();
        beforeAfterPredicates = new HashMap<>();
        weakestPreconditions = new HashMap<>();
        primedInvariants = new HashMap<>();

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
        log.trace("Collecting properties");
        for (Property x : comp.getChildrenOfType(Property.class)) {
            properties.add(BPredicate.of(x.getFormula().getCode()));
        }

        // add invariants
        log.trace("Collecting invariants");
        for (Invariant x : comp.getChildrenOfType(Invariant.class)) {
            if (x.isTheorem())
                assertions.add(BPredicate.of(x.getFormula().getCode()));
            else
                invariants.add(BPredicate.of(x.getFormula().getCode()));
        }
        // Conjunct invariants if more then one
        if (invariants.size() > 1) {
            BPredicate invariantConcat =
                    FormulaGenerator.getPredicateConjunction(invariants);
            invariants.add(invariantConcat);
        }

        log.trace("Collecting assertions");
        for (Assertion x : comp.getChildrenOfType(Assertion.class)) {
            assertions.add(BPredicate.of(x.getFormula().getCode()));
        }

        // for each event collect preconditions
        log.trace("Collecting operations and preconditions");
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            if (x.getName().equals("INITIALISATION"))
                continue; // None for initialisation
            operations.add(x.getName());

            log.trace("Collecting preconditions for {}", x.getName());
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
                invCmds.put(inv,
                        Backend.generateBFormula(inv, bMachine));
            } catch (FormulaException e) {
                log.warn("Could not set up EvalElement from {} for "
                         + "weakest precondition calculation or priming",
                        inv, e);
            }
        }

        // weakest preconditions for each invariant
        log.trace("Building weakest preconditions");
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
                    log.warn("Could not build weakest precondition"
                             + "for {} by operation {}.",
                            invCmd.getCode(), x.getName(), e);
                }

            }
            weakestPreconditions.put(x.getName(), wpcs);
        }

        if (bMachine.getMachineType() != MachineType.EVENTB)
            return; // FIXME: allow usage of classical B, too

        // Before/After predicates
        log.trace("Building before/after predicates");
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            if (x.getName().equals("INITIALISATION"))
                continue; // None for initialisation

            try {
                BeforeAfterPredicateCommand bapc =
                        new BeforeAfterPredicateCommand(x.getName());
                bMachine.execute(bapc);
                // FIXME: Erase comment, probably should not be returned by ProB to begin with
                String code = bapc.getBeforeAfterPredicate().getCode()
                        .replaceAll("/\\*.*\\*/ *", "");
                beforeAfterPredicates.put(x.getName(), BPredicate.of(code));
            } catch (Exception e) {
                log.warn("Could not build Before After Predicate for event {}.",
                        x.getName(), e);
            }

        }

        // primed invariants
        log.trace("Building primed invariants");
        for (BPredicate inv : invCmds.keySet()) {
            IBEvalElement invCmd = invCmds.get(inv);
            try {
                primedInvariants.put(inv,
                        FormulaGenerator.generatePrimedPredicate(bMachine, invCmd));
            } catch (Exception e) {
                log.warn("Could not build primed invariant from {}", inv, e);
            }
        }
    }

    private void collectFromContext(Context bcc) {
        // axioms
        log.trace("Collecting axioms");
        for (Axiom x : bcc.getChildrenOfType(Axiom.class)) {
            properties.add(BPredicate.of(x.getFormula().getCode()));
        }

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

}
