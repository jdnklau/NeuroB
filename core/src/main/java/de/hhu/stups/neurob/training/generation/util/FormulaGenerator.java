package de.hhu.stups.neurob.training.generation.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.animator.command.PrimePredicateCommand;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.statespace.StateSpace;

/**
 * This class provides access to reusable methods to get different formulas
 * from a specific input.
 * <p>
 * Mostly it rearranges an input formula to generate variations or extends
 * the input by recombining different parts of it.
 * </p>
 */
public class FormulaGenerator {

    private static final Logger log =
            LoggerFactory.getLogger(FormulaGenerator.class);

    /**
     * Takes a given predicate and primes the identifiers.
     * <p>
     * Example: "x>y & y>x" will be translated into "x'>y' & y'>x'"
     * <p>
     * This is mainly useful for before-after predicates.
     *
     * @param ss
     * @param predicate
     *
     * @return
     *
     * @throws FormulaException
     */
    public static String generatePrimedPredicate(StateSpace ss,
            String predicate) throws FormulaException {
        return generatePrimedPredicate(ss,
                Backend.generateBFormula(predicate, ss));
    }

    /**
     * Takes a given {@link IBEvalElement} and primes the identifiers.
     * <p>
     * Example: "x>y & y>x" will be translated into "x'>y' & y'>x'"
     * <p>
     * This is mainly useful for before-after predicates.
     *
     * @param ss
     * @param evalElement
     *
     * @return
     *
     * @throws FormulaException
     */
    public static String generatePrimedPredicate(StateSpace ss,
            IBEvalElement evalElement) throws FormulaException {
        try {
            PrimePredicateCommand ppc = new PrimePredicateCommand(evalElement);
            ss.execute(ppc);
            return ppc.getPrimedPredicate().getCode();
        } catch (Exception e) {
            throw new FormulaException("Could not build primed predicate from "
                                       + evalElement.getCode(), e);
        }
    }

    /**
     * <p>Generates multiple formulas for each operation found.</p>
     * <p>Expects a {@link PredicateCollection}, that already was used on a
     * machine to collect data.
     * The found predicates are then combined to generate multiple formulae
     * revolving around preconditions and operations.
     * <br>
     * Generated formulae are e.g.:
     * <ul>
     * <li>invariants</li>
     * <li>properties & invariants</li>
     * <li>properties & invariants & precondition</li>
     * <li>properties & invariants => precondition</li>
     * <li>properties & invariants & not precondition</li>
     * <li>properties & invariants => not precondition</li>
     * <li>properties & precondition => invariants</li>
     * <li>properties & precondition => not invariants</li>
     * <li>properties & not precondition => invariants</li>
     * <li>properties & not precondition => not invariants</li>
     * </ul></p>
     * <p>Usage example:
     * <pre>
     * {@code
     * // load machine and main component ...
     * PredicateCollection pc = new PredicateCollection(mainComponent);
     * List<String> formulae = FormulaGenerator.extendedPreconditionFormulae(pc);
     * for(String formula : formulae) {
     *     // do stuff
     * }
     * </pre></p>
     *
     * @param predicateCollection An already used {@link
     *         PredicateCollection }
     *
     * @return An ArrayList containing all formulae constructed from the
     *         predicate collector
     */
    public static List<String> extendedPreconditionFormulae(
            PredicateCollection predicateCollection) {
        String properties = getPropertyString(predicateCollection);
        String invariants = getInvariantString(predicateCollection);

        return generateExtendedPreconditionFormulae(properties, invariants,
                predicateCollection.getPreconditions());

    }

    /**
     * Generates multiple formulae containing each possible pairing of
     * preconditions
     *
     * @param predicateCollection
     *
     * @return
     */
    public static List<String> multiPreconditionFormulae(
            PredicateCollection predicateCollection) {
        ArrayList<String> formulae = new ArrayList<String>();

        String propsAndInvsPre = getPropsAndInvsPre(predicateCollection);

        List<String> allPreconditions = predicateCollection.getPreconditions().entrySet()
                .stream()
                .map(Entry::getValue)
                .map(FormulaGenerator::getStringConjunction)
                .collect(Collectors.toList());

        int preconditionCount = allPreconditions.size();

        // pairwise iterate over preconditions
        for (int i = 0; i < preconditionCount; i++) {
            for (int j = i + 1; j < preconditionCount; j++) {
                String g1 = allPreconditions.get(i);
                String g2 = allPreconditions.get(j);

                formulae.add(propsAndInvsPre + g1 + " & " + g2);
                formulae.add(propsAndInvsPre + "(not(" + g1 + ") => " + g2 + ")");
                formulae.add(propsAndInvsPre + g1 + " & not(" + g2 + ")");
                formulae.add(propsAndInvsPre + "(" + g1 + " => not(" + g2 + "))");
            }
        }


        return formulae;
    }

    /**
     * Returns a list of formulae that revolve around enabling analysis.
     * <p>
     * The formulae created:
     * <br> Let P be the concatenation of properties and invariants.
     * Let g1, g2 be preconditions of two operations,
     * Let ba be the before/after predicate for the operation of g1.
     * <ul>
     * <li> P & g1 & ba & g2 [Preconditions enabled after executing another
     * first]
     * <li> P & g1 & ba & ~g2 [Preconditions disabled after executing another
     * first]
     * <li> P & ~(g1 & ba) & g2
     * <li> P & ~(g1 & ba) & ~g2
     * <li> P & (~(g1 & ba) => g2)
     * <li> P & (~(g1 & ba) => ~g2)
     * </ul>
     *
     * @param predicateCollection
     *
     * @return
     */
    public static List<String> enablingRelationships(
            PredicateCollection predicateCollection) {
        List<String> formulae = new ArrayList<>();

        // unsupported for non-EventB
        if (predicateCollection.getMachineType() != MachineType.EVENTB)
            return formulae;

        String PropsAndInvsPre = getPropsAndInvsPre(predicateCollection);

        /*
         * Generate for each pair of operations formulae whether one
         * is enabled in the next state after executing the other
         *
         * For this we need the preconditions of all operations,
         * the operations for which we got before after predicates,
         * the respective before after predicates,
         * primed preconditions
         */

        List<String> operations = new ArrayList<>(
                predicateCollection.getPreconditions().keySet());
        Map<String, List<String>> preconditionConjuncts =
                predicateCollection.getPreconditions();

        // get conjuncted preconditions
        Map<String, String> preconditions = new HashMap<>();
        for (String operation : operations) {
            preconditions.put(operation, getStringConjunction(preconditionConjuncts.get(operation)));
        }

        // before after predicates
        Map<String, String> beforeAfter =
                predicateCollection.getBeforeAfterPredicates();

        // get primed preconditions of operations we also got before/after predicates for
        Map<String, String> primedPreconditions = new HashMap<>();
        for (String operation : new ArrayList<>(beforeAfter.keySet())) {
            if (!preconditions.containsKey(operation)) {
                // some operations have no precondition but before/after predicate
                continue;
            }
            try {
                primedPreconditions.put(operation, generatePrimedPredicate(
                        predicateCollection.accessStateSpace(),
                        getStringConjunction(preconditionConjuncts.get(operation))));
            } catch (FormulaException e) {
                log.warn("{}", e.getMessage(), e);
            }
        }


        // set up formulae
        for (String operation : beforeAfter.keySet()) {

            String g1;
            if (preconditions.containsKey(operation)) {
                g1 = preconditions.get(operation) + " & ";
            } else {
                g1 = "";
            }
            String g1AndBa = g1 + beforeAfter.get(operation);

            for (String primedOperation : primedPreconditions.keySet()) {
                String g2 = primedPreconditions.get(primedOperation);

                formulae.add(PropsAndInvsPre + "(" + g1AndBa + " & " + g2 + ")");
                formulae.add(PropsAndInvsPre + "(" + g1AndBa + " & not(" + g2 + "))");

                formulae.add(PropsAndInvsPre + "not(" + g1AndBa + ") & " + g2 + "");
                formulae.add(PropsAndInvsPre + "not(" + g1AndBa + ") & not(" + g2 + ")");
                formulae.add(PropsAndInvsPre + "(not(" + g1AndBa + ") => " + g2 + ")");
                formulae.add(PropsAndInvsPre + "(not(" + g1AndBa + ") => not(" + g2 + "))");
            }
        }

        return formulae;
    }

    /**
     * Returns a list of invariant preservation proof obligations and other
     * formulae inspired by those.
     * <p>
     * The following formulae are generated:
     * <br>Let P be the properties. Let i be an invariant or the conjunction of
     * all invariants.
     * Let W be the weakest precondition of the respective invariant for an
     * operation in the machine.
     * <ul>
     * <li> P & i & W
     * <li> P & i & ~W
     * <li> P & (~i => W)
     * <li> P & (~i => ~W)
     * </ul>
     * <p>
     * Further, let g be the precondition of an operation and ba the operations
     * before/after
     * predicate.
     * Let j be the invariant after the operation (j:=i')
     * <br>
     * The following additional formulae are generated for EventB machines:
     * <ul>
     * <li> P & i & g & ba & j
     * <li> P & i & g & ba & ~j
     * <li> P & (~(i & g & ba) => j)
     * <li> P & (~(i & g & ba) => ~j)
     * </ul>
     *
     * @param predicateCollection
     *
     * @return
     */
    public static List<String> invariantPreservations(
            PredicateCollection predicateCollection) {
        List<String> formulae = new ArrayList<>();

        String PropsPre = getPropertyPre(predicateCollection);
        String Invs = getInvariantString(predicateCollection);

        /*
         * Generate invariants preservation strings:
         * - Inv => weakestPre
         * - Inv & Precondition & before/after => Inv'
         */

        // Classical B: weakest precondition
        Map<String, Map<String, String>> weakestPreMap =
                predicateCollection.getWeakestPreConditions();

        // - for each operation
        for (Entry<String, Map<String, String>> opEntry : weakestPreMap.entrySet()) {
            // collect all for this operation to concatenate later
            List<String> weakestPres = new ArrayList<>();

            // - for each invariant
            for (Entry<String, String> invEntry : opEntry.getValue().entrySet()) {
                String inv = invEntry.getKey();
                String wpc = invEntry.getValue();

                formulae.add(PropsPre + inv + " & " + wpc);
                formulae.add(PropsPre + "(not(" + inv + ") => " + wpc + ")");
                formulae.add(PropsPre + inv + " & not(" + wpc + ")");
                formulae.add(PropsPre + "(not(" + inv + ") => not(" + wpc + "))");

                weakestPres.add(wpc);
            }
        }


        // Event B: before/after predicate
        if (predicateCollection.getMachineType() != MachineType.EVENTB)
            return formulae; // the following is for EVENTB only FIXME

        Map<String, String> primedInvsMap =
                predicateCollection.getPrimedInvariants();

        if (!primedInvsMap.isEmpty()) { // do only if the map is not empty
            // Collect all invariants plus their concatenation if more than 1
            List<String> invariants = new ArrayList<>();
            invariants.addAll(predicateCollection.getInvariants());
            if (invariants.size() > 1) {
                invariants.add(Invs);
            }
            for (String unprimedInv : invariants) {

                String primedInv = primedInvsMap.get(unprimedInv);
                // Skip if primed invariant was not properly collected
                if (primedInv == null) {
                    continue;
                }

                Map<String, List<String>> preconditions =
                        predicateCollection.getPreconditions();
                Map<String, String> beforeAfter =
                        predicateCollection.getBeforeAfterPredicates();

                for (String operation : beforeAfter.keySet()) {
                    String g; // the precondition of the operation (may be empty)

                    if (preconditions.containsKey(operation)) {
                        g = getStringConjunction(preconditions.get(operation)) + " & ";
                    } else {
                        g = "";
                    }

                    String gAndBa = g + beforeAfter.get(operation);

                    formulae.add(PropsPre + unprimedInv + " & " + gAndBa
                                 + " & " + primedInv);
                    formulae.add(PropsPre + "(not(" + unprimedInv + " & "
                                 + gAndBa + ") => " + primedInv + ")");
                    formulae.add(PropsPre + unprimedInv + " & " + gAndBa
                                 + " & not(" + primedInv + ")");
                    formulae.add(PropsPre + "(not(" + unprimedInv + " & "
                                 + gAndBa + ") => not(" + primedInv + "))");
                }

            }
        }


        return formulae;
    }

    /**
     * Generates a list of predicates from the assertions and theorems in the
     * machine.
     * <p>
     * Let P be the properties and invariant. Let A be an assertion or
     * or the concatenation of all assertions.
     * <br>
     * The formulae generated are:
     * <ul>
     * <li>P & A
     * <li>P & ~A
     * <li>~P => A
     * </ul>
     *
     * @param predicateCollection
     *
     * @return
     */
    public static List<String> assertions(PredicateCollection predicateCollection) {
        String propsAndInv = getPropertyAndInvariantString(predicateCollection);
        ArrayList<String> formulae = new ArrayList<>();

        List<String> assertionsList = new ArrayList<>();
        assertionsList.addAll(predicateCollection.getAssertions());
        // If no assertions, then return empty list
        if (assertionsList.isEmpty()) {
            return formulae;
        }

        // If more than one assertion, add conjunction to list as well
        if (assertionsList.size() > 1) {
            assertionsList.add(getStringConjunction(assertionsList));
        }

        if (propsAndInv.isEmpty()) {
            for (String a : assertionsList) {
                formulae.add(a);
                formulae.add("not(" + a + ")");
            }
        } else {
            // proof assertions
            for (String a : assertionsList) {
                formulae.add(propsAndInv + " & " + a);
                formulae.add(propsAndInv + " & not(" + a + ")");
                formulae.add("not(" + propsAndInv + ") => " + a);
            }
        }

        return formulae;
    }

    /**
     * Takes a list of Strings and joins them with " & " as delimiter.
     * Each conjunct will be wrapped in parenthesis
     *
     * @param conjuncts
     *
     * @return
     */
    public static String getStringConjunction(List<String> conjuncts) {
        String conj = String.join(") & (", conjuncts);
        return (conj.isEmpty()) ? "" : "(" + conj + ")";
    }

    private static String getPropertyString(PredicateCollection predicateCollection) {
        return getStringConjunction(predicateCollection.getProperties());
    }

    private static String getInvariantString(PredicateCollection predicateCollection) {
        return getStringConjunction(predicateCollection.getInvariants());
    }

    private static String getPropertyPre(PredicateCollection predicateCollection) {
        String properties = getPropertyString(predicateCollection);

        if (properties.isEmpty()) {
            return "";
        } else {
            return properties + " & ";
        }
    }

    private static String getInvariantsPre(PredicateCollection predicateCollection) {
        String invariants = getInvariantString(predicateCollection);

        if (invariants.isEmpty()) {
            return "";
        } else {
            return invariants + " & ";
        }
    }

    private static String getPropsAndInvsPre(PredicateCollection predicateCollection) {
        return getPropertyPre(predicateCollection) + getInvariantsPre(predicateCollection);
    }

    private static String getPropertyAndInvariantString(
            PredicateCollection predicateCollection) {
        String inv = getInvariantString(predicateCollection);

        if (inv.isEmpty())
            return getPropertyString(predicateCollection);


        return getPropertyPre(predicateCollection) + inv;
    }


    private static List<String> generateExtendedPreconditionFormulae(
            String properties, String invariants,
            Map<String, List<String>> allPreconditions) {
        List<String> formulae = new ArrayList<>();

        // check for empty formulas
        boolean emptyProperties = false;
        String propertyPre;
        if (properties.isEmpty()) {
            emptyProperties = true;
            propertyPre = "";
        } else {
            propertyPre = properties + " & ";
        }


        boolean emptyInvariants = false;
        String invariantsPre;
        String negInvariants;
        if (invariants.isEmpty()) {
            emptyInvariants = true;
            invariantsPre = "";
            negInvariants = "";
        } else {
            formulae.add(invariants); // invariants
            invariantsPre = invariants + " & ";
            negInvariants = "not(" + invariants + ")";
        }


        String propsAndInvs = (invariants.isEmpty()) ? properties : propertyPre + invariants;
        String propsAndNegInvs = (invariants.isEmpty()) ? properties : propertyPre + negInvariants;


        // preconditions
        List<List<String>> allPreconditionsList = allPreconditions.entrySet()
                .stream()
                .map(e -> e.getValue())
                .collect(Collectors.toList());

        for (List<String> preconditions : allPreconditionsList) {
            String precondition = getStringConjunction(preconditions);

            // only continue if the preconditions are nonempty
            if (precondition.isEmpty()) {
                continue;
            }

            String negPrecondition = "not(" + precondition + ")";

            String propsAndPrecondition;
            propsAndPrecondition = propertyPre + precondition;
            String propsAndNegPrecondition = propertyPre + negPrecondition;

            // operations active w/o violating invariants
            formulae.add(propertyPre + invariantsPre + precondition);
            // following code only makes sense if invariants or properties
            // are not empty
            if (emptyInvariants && emptyProperties) {
                continue;
            }

            // operations usable with unviolated invariants
            formulae.add("not(" + propsAndInvs + ") => " + precondition);

            // operations not active w/o violating invariants
            formulae.add(propsAndInvs + " & " + negPrecondition);
            // operations not usable with unviolated invariants
            formulae.add("not(" + propsAndInvs + ") => " + negPrecondition);

            // operations only usable w/o invariant violation
            formulae.add("not(" + propsAndPrecondition + ") => " + invariants);
            // operations never usable w/o invariant violation
            formulae.add("not(" + propsAndNegPrecondition + ") => " + invariants);

            if (emptyInvariants) {
                // incoming formulae would be repetitive, so skip them
                continue;
            }

            // operations active despite invariant violation
            formulae.add(propsAndNegInvs + " & " + precondition);
            // operations usable despite invariant violation
            formulae.add("not(" + propsAndNegInvs + ") => " + precondition);

            // operations not active with invariant violation
            formulae.add(propsAndNegInvs + " & " + negPrecondition);
            // operations not usable with invariant violation
            formulae.add("not(" + propsAndNegInvs + ") => " + negPrecondition);

            // operations never usable with invariant violation
            formulae.add("not(" + propsAndNegPrecondition + ") => " + negInvariants);
            // operations only usable with invariant violation
            formulae.add("not(" + propsAndPrecondition + ") => " + negInvariants);
        }

        return formulae;
    }

}
