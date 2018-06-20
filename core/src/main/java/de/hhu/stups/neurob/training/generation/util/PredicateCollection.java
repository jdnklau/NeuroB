package de.hhu.stups.neurob.training.generation.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
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
import de.prob.statespace.StateSpace;

/**
 * Collection of invariants, properties, preconditions, etc. found in a
 * {@link StateSpace}.
 */
public class PredicateCollection {
    private List<String> invariants;
    private List<String> operations;
    private Map<String, List<String>> preconditions;
    private List<String> properties; // also contains axioms of EventB
    private List<String> assertions; // also contains theorems of EventB
    private Map<String, String> beforeAfterPredicates;
    private Map<String, Map<String, String>> weakestPreconditions;
    private Map<String, String> primedInvariants;

    private StateSpace ss;

    private MachineType machineType;

    private static final Logger log =
            LoggerFactory.getLogger(PredicateCollection.class);

    public PredicateCollection(StateSpace ss) {
        this.ss = ss;
        machineType = MachineType.getTypeFromStateSpace(ss);

        invariants = new ArrayList<>();
        operations = new ArrayList<>();
        preconditions = new HashMap<>();
        properties = new ArrayList<>();
        assertions = new ArrayList<>();
        beforeAfterPredicates = new HashMap<>();
        weakestPreconditions = new HashMap<>();
        primedInvariants = new HashMap<>();

        collectPredicates();

    }

    private void collectPredicates() {
        AbstractElement comp = ss.getMainComponent();
        // properties
        log.trace("Collecting properties");
        for (Property x : comp.getChildrenOfType(Property.class)) {
            properties.add(x.getFormula().getCode());
        }

        // add invariants
        log.trace("Collecting invariants");
        for (Invariant x : comp.getChildrenOfType(Invariant.class)) {
            if (x.isTheorem())
                assertions.add(x.getFormula().getCode());
            else
                invariants.add(x.getFormula().getCode());
        }
        String invariantConcat =
                FormulaGenerator.getStringConjunction(invariants);

        log.trace("Collecting assertions");
        for (Assertion x : comp.getChildrenOfType(Assertion.class)) {
            assertions.add(x.getFormula().getCode());
        }

        // for each event collect preconditions
        log.trace("Collecting operations and preconditions");
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            operations.add(x.getName());

            log.trace("Collecting preconditions for {}", x.getName());
            ArrayList<String> event = new ArrayList<String>();
            for (Guard g : x.getChildrenOfType(Guard.class)) {
                event.add(g.getFormula().getCode());
            }
            if (!event.isEmpty())
                preconditions.put(x.getName(), event);
        }
        // axioms
        log.trace("Collecting axioms");
        for (Axiom x : comp.getChildrenOfType(Axiom.class)) {
            properties.add(x.getFormula().getCode());
        }

        // set up invariants as commands for below
        Map<String, IBEvalElement> invCmds = new HashMap<>();
        for (String inv : invariants) {
            try {
                invCmds.put(inv,
                        FormulaGenerator.generateBCommand(ss, inv));
            } catch (FormulaException e) {
                log.warn("Could not set up EvalElement from {} for "
                         + "weakest precondition calculation or priming",
                        inv, e);
            }
        }
        // command for concatenation of invariants
        try {
            invCmds.put(invariantConcat,
                    FormulaGenerator.generateBCommand(ss,
                            invariantConcat));
        } catch (FormulaException e) {
            log.warn("Could not set up weakest precondition command for "
                     + "concatenation of invariants {]",
                    invariantConcat, e);
        }

        // weakest preconditions for each invariant
        log.trace("Building weakest preconditions");
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            if (x.getName().equals("INITIALISATION"))
                continue; // None for initialisation

            Map<String, String> wpcs = new HashMap<>();
            for (String inv : invCmds.keySet()) {
                IBEvalElement invCmd = invCmds.get(inv);

                try {
                    WeakestPreconditionCommand wpcc =
                            new WeakestPreconditionCommand(x.getName(), invCmd);
                    ss.execute(wpcc);
                    wpcs.put(inv, wpcc.getWeakestPrecondition().getCode());
                } catch (Exception e) {
                    log.warn("Could not build weakest precondition"
                             + "for {} by operation {}.",
                            invCmd.getCode(), x.getName(), e);
                }

            }
            weakestPreconditions.put(x.getName(), wpcs);

            // build weakest precondition for invariant concatenation

        }

        if (machineType != MachineType.EVENTB)
            return; // FIXME: allow usage of classical B, too

        // Before/After predicates
        log.trace("Building before/after predicates");
        for (BEvent x : comp.getChildrenOfType(BEvent.class)) {
            if (x.getName().equals("INITIALISATION"))
                continue; // None for initialisation

            try {
                BeforeAfterPredicateCommand bapc =
                        new BeforeAfterPredicateCommand(x.getName());
                ss.execute(bapc);
                beforeAfterPredicates.put(x.getName(),
                        bapc.getBeforeAfterPredicate().getCode());
            } catch (Exception e) {
                log.warn("Could not build Before After Predicate for event {}.",
                        x.getName(), e);
            }

        }

        // primed invariants
        log.trace("Building primed invariants");
        for (String inv : invCmds.keySet()) {
            IBEvalElement invCmd = invCmds.get(inv);
            try {
                primedInvariants.put(inv,
                        FormulaGenerator.generatePrimedPredicate(ss, invCmd));
            } catch (Exception e) {
                log.warn("Could not build primed invariant from {}", inv, e);
            }
        }

    }

    /**
     * @return A Map, that pairs an event name (key) with a list of its
     *         respective preconditions
     */
    public Map<String, List<String>> getPreconditions() {
        return preconditions;
    }

    public List<String> getInvariants() {
        return invariants;
    }

    public List<String> getOperationNames() {
        return operations;
    }

    public List<String> getProperties() {
        return properties;
    }

    public List<String> getAssertions() {
        return assertions;
    }

    /**
     * @return A map, pairing each event (key) with its respective before/after
     *         predicate.
     */
    public Map<String, String> getBeforeAfterPredicates() {
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
    public Map<String, Map<String, String>> getWeakestPreConditions() {
        return weakestPreconditions;
    }

    /**
     * @return A map of invariants to their primed version
     */
    public Map<String, String> getPrimedInvariants() {
        return primedInvariants;
    }


    public MachineType getMachineType() {
        return machineType;
    }

    public StateSpace accessStateSpace() {
        return ss;
    }

}
