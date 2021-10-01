package de.hhu.stups.neurob.training.generation.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.logback.core.util.ContextUtil;
import de.be4.classicalb.core.parser.node.Node;
import de.be4.classicalb.core.parser.util.PrettyPrinter;
import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.prob.animator.command.NQPrimePredicateCommand;
import de.prob.animator.command.PrettyPrintFormulaCommand;
import de.prob.animator.domainobjects.IEvalElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.animator.command.PrimePredicateCommand;
import de.prob.animator.domainobjects.IBEvalElement;

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
     * @param bMachine Access to the B machine the predicate belongs to
     * @param predicate
     *
     * @return
     *
     * @throws FormulaException
     */
    public static BPredicate generatePrimedPredicate(MachineAccess bMachine,
            String predicate) throws FormulaException {
        return generatePrimedPredicate(bMachine,
                Backend.generateBFormula(BPredicate.of(predicate), bMachine));
    }

    /**
     * Takes a given predicate and primes the identifiers.
     * <p>
     * Example: "x>y & y>x" will be translated into "x'>y' & y'>x'"
     * <p>
     * This is mainly useful for before-after predicates.
     *
     * @param bMachine Access to the B machine the predicate belongs to
     * @param predicate
     *
     * @return
     *
     * @throws FormulaException
     */
    public static BPredicate generatePrimedPredicate(MachineAccess bMachine,
            BPredicate predicate) throws FormulaException {
        return generatePrimedPredicate(bMachine,
                Backend.generateBFormula(predicate, bMachine));
    }

    /**
     * Takes a given {@link IBEvalElement} and primes the identifiers.
     * <p>
     * Example: "x>y & y>x" will be translated into "x'>y' & y'>x'"
     * <p>
     * This is mainly useful for before-after predicates.
     *
     * @param bMachine Access to the B machine the predicate belongs to
     * @param evalElement
     *
     * @return
     *
     * @throws FormulaException
     */
    public static BPredicate generatePrimedPredicate(MachineAccess bMachine,
            IBEvalElement evalElement) throws FormulaException {
        try {
            NQPrimePredicateCommand ppc = new NQPrimePredicateCommand(evalElement);
            bMachine.execute(ppc);

            String primed = ppc.getPrimedPredicate();
            if (MachineType.CLASSICALB.equals(bMachine.getMachineType())) {
                primed = primeClassicalB(primed);
            }

            return BPredicate.of(primed);
        } catch (Exception e) {
            throw new FormulaException("Could not build primed predicate from "
                                       + evalElement.getCode(), e);
        }
    }

    static String primeClassicalB(String code) {
        return code.replaceAll("[']", "′"); // FIXME: What about strings using '?
    }

    static BPredicate primeClassicalB(BPredicate code) {
        return BPredicate.of(primeClassicalB(code.getPredicate()));
    }


    public static BPredicate cleanupAst(MachineAccess bMachine,
            BPredicate predicate) throws FormulaException {
        return cleanupAst(bMachine, Backend.generateBFormula(predicate, bMachine));
    }

    public static BPredicate cleanupAst(MachineAccess bMachine,
            IBEvalElement evalElement) throws FormulaException {
        try {
            PrettyPrintFormulaCommand cleanup = new PrettyPrintFormulaCommand(evalElement, PrettyPrintFormulaCommand.Mode.ASCII);
            cleanup.setOptimize(true);
            bMachine.execute(cleanup);
            // NOTE: The PrettyPrintFormulaCommand sometimes returns comments as well.
            // We pretty print again to get rid of them.
            BPredicate result = BPredicate.of(cleanup.getPrettyPrint());
            cleanup = new PrettyPrintFormulaCommand(bMachine.parseFormula(result), PrettyPrintFormulaCommand.Mode.ASCII);
            cleanup.setOptimize(true);
            bMachine.execute(cleanup);
            return BPredicate.of(cleanup.getPrettyPrint());

        } catch (Exception e) {
            throw new FormulaException("Could not create cleaned up AST from "
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
    public static List<BPredicate> extendedPreconditionFormulae(
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
    public static List<BPredicate> multiPreconditionFormulae(
            PredicateCollection predicateCollection) {
        String propsAndInvsPre = getPropsAndInvsPre(predicateCollection);

        List<BPredicate> allPreconditions =
                predicateCollection.getPreconditions().entrySet()
                        .stream()
                        .map(Entry::getValue)
                        .map(FormulaGenerator::getPredicateConjunction)
                        .collect(Collectors.toList());

        int preconditionCount = allPreconditions.size();

        // pairwise iterate over preconditions
        ArrayList<BPredicate> formulae = new ArrayList<>();
        for (int i = 0; i < preconditionCount; i++) {
            for (int j = i + 1; j < preconditionCount; j++) {
                String g1 = allPreconditions.get(i).toString();
                String g2 = allPreconditions.get(j).toString();

                formulae.add(BPredicate.of(propsAndInvsPre + g1 + " & " + g2));
                formulae.add(BPredicate.of(propsAndInvsPre + "(not(" + g1 + ") => (" + g2 + "))"));
                formulae.add(BPredicate.of(propsAndInvsPre + g1 + " & not(" + g2 + ")"));
                formulae.add(BPredicate.of(propsAndInvsPre + "(" + g1 + " => not(" + g2 + "))"));
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
     *
     * @deprecated Use {@link #enablingAnalysis(PredicateCollection)} instead.
     */
    public static List<BPredicate> enablingRelationships(
            PredicateCollection predicateCollection) {

        // unsupported for non-EventB
        if (predicateCollection.getMachineType() != MachineType.EVENTB)
            return new ArrayList<>();

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
        Map<String, List<BPredicate>> preconditionConjuncts =
                predicateCollection.getPreconditions();

        // get conjuncted preconditions
        Map<String, BPredicate> preconditions = new HashMap<>();
        for (String operation : operations) {
            preconditions.put(operation, getPredicateConjunction(preconditionConjuncts.get(operation)));
        }

        // before after predicates
        Map<String, BPredicate> beforeAfter =
                predicateCollection.getBeforeAfterPredicates();

        // get primed preconditions of operations we also got before/after predicates for
        Map<String, BPredicate> primedPreconditions = new HashMap<>();
        for (String operation : new ArrayList<>(beforeAfter.keySet())) {
            if (!preconditions.containsKey(operation)) {
                // some operations have no precondition but before/after predicate
                continue;
            }
            try {
                primedPreconditions.put(operation, generatePrimedPredicate(
                        predicateCollection.getBMachine(),
                        getPredicateConjunction(preconditionConjuncts.get(operation))));
            } catch (FormulaException e) {
                log.warn("{}", e.getMessage(), e);
            }
        }


        // set up formulae
        List<BPredicate> formulae = new ArrayList<>();
        for (String operation : beforeAfter.keySet()) {

            String g1;
            if (preconditions.containsKey(operation)) {
                g1 = preconditions.get(operation) + " & ";
            } else {
                g1 = "";
            }
            String g1AndBa = g1 + beforeAfter.get(operation);

            for (String primedOperation : primedPreconditions.keySet()) {
                String g2 = primedPreconditions.get(primedOperation).toString();

                formulae.add(BPredicate.of(PropsAndInvsPre + "(" + g1AndBa + " & " + g2 + ")"));
                formulae.add(BPredicate.of(PropsAndInvsPre + "(" + g1AndBa + " & not(" + g2 + "))"));

                formulae.add(BPredicate.of(PropsAndInvsPre + "not(" + g1AndBa + ") & " + g2 + ""));
                formulae.add(BPredicate.of(PropsAndInvsPre + "not(" + g1AndBa + ") & not(" + g2 + ")"));
                formulae.add(BPredicate.of(PropsAndInvsPre + "(not(" + g1AndBa + ") => (" + g2 + "))"));
                formulae.add(BPredicate.of(PropsAndInvsPre + "(not(" + g1AndBa + ") => not(" + g2 + "))"));
            }
        }

        return formulae;
    }

    /**
     * Generates a list of predicates inspired by enabling analysis [1].
     * <p>
     * Paper [1] defines conditional event-feasibility (~>_e) as follows:
     * P ~>_e Q
     * if there exists a state s with
     * s |= P
     * s |= BAP & [v=v']Q
     * with the before-after-predicate BAP. Thus, an event is feasible if P is
     * established and after execution Q is established.
     * <p>
     * * [1] Dobrikov & Leuschel, 2018, "Enabling Analysis for Event-B"
     *
     * @param pc
     *
     * @return
     */
    public static List<BPredicate> enablingAnalysis(PredicateCollection pc) {
        List<String> formulae = new ArrayList<>();

        String PropsAndInvsPre = getPropsAndInvsPre(pc);

        List<String> operations = pc.getOperationNames();
        Map<String, List<BPredicate>> preconditionLists = pc.getPreconditions();

        Map<String, BPredicate> preconditions = new HashMap<>();
        for (String operation : operations) {
            if (preconditionLists.containsKey(operation)) {
                BPredicate precond = getPredicateConjunction(preconditionLists.get(operation));
                preconditions.put(operation, precond);
            }
        }

        Map<String, BPredicate> beforeAfterPreds = pc.getBeforeAfterPredicates();

        String pinv = getPredicateConjunction(
                new ArrayList<>(pc.getPrimedInvariants().values())).getPredicate();

        // Prime preconditions for use with before/after predicates (BAPs)
        Map<String, BPredicate> primedPreconds = new HashMap<>();
        for (String operation : operations) {
            if (preconditions.containsKey(operation)) {
                try {
                    BPredicate primedPre = generatePrimedPredicate(
                            pc.getBMachine(), preconditions.get(operation));
                    primedPreconds.put(operation, primedPre);
                } catch (FormulaException e) {
                    log.warn("Unable to prime precondition: {}", e.getMessage(), e);
                }
            }
        }

        for (String op1 : operations) {
            boolean hasP1 = preconditions.containsKey(op1);
            // Get "& precondition" of operation 1
            String g1 = "(" + preconditions.getOrDefault(op1, new BPredicate("btrue")).getPredicate() + ")";
            String andG1 = " & " + g1;

            // event feasibility
            if (hasP1) {
                formulae.add(PropsAndInvsPre + g1); // feasible: exists s.(s |= g1)
                formulae.add(PropsAndInvsPre + "not" + g1); // not guaranteed: not all s.(s |= g1)
            }



            boolean hasBA = beforeAfterPreds.containsKey(op1);
            if (!hasBA) {
                continue; // We need a bap for the set of upcoming data.
            }
            String bap = "(" + beforeAfterPreds.get(op1).getPredicate() + ")";
            String andBap1 = " & " + bap;
            for (String op2 : operations) {
                boolean hasP2 = preconditions.containsKey(op2);

                String g2 = "(" + preconditions.getOrDefault(op2, new BPredicate("btrue")).getPredicate() + ")";
                String andG2 = " & " + g2;

                // negated version
                String ng2 = "not" + g2;
                String andNG2 = " & " + ng2;


                // primed version
                if (!primedPreconds.containsKey(op2)) {
                    continue;
                }
                String pg2 = "(" + primedPreconds.getOrDefault(op2, new BPredicate("btrue")).getPredicate() + ")";
                String andPG2 = " & " + pg2;

                // negated  prime version
                String png2 = "not" + pg2;
                String andPNG2 = " & " + png2;

                if (!op2.equals(op1) && hasP1) { // Else it'd be generated twice (below, 2. and 3.)
                    // op2 possible after op1
                    formulae.add(PropsAndInvsPre + g1 + andBap1 + andPG2);
                    // op2 not feasible after op1
                    formulae.add(PropsAndInvsPre + g1 + andBap1 + andPNG2);
                    // Note: ^ those are counter example definitions of impossible/feasible
                }

                // The four conditions from Definition 6 in the paper are as follows
                // ("Note that [...] we do not require that [op1] is feasible")
                // 1. can op1 enable op2
                formulae.add(PropsAndInvsPre + ng2 + andBap1 + andPG2);
                //                             ^ disabl ^ op1     ^ enabl
                // 2. can op1 disable op2
                formulae.add(PropsAndInvsPre + g2 + andBap1 + andPNG2);
                // 3. can op1 keep op2 enabled
                formulae.add(PropsAndInvsPre + g2 + andBap1 + andPG2);
                // 4. can op1 keep op2 disabled
                formulae.add(PropsAndInvsPre + ng2 + andBap1 + andPNG2);

                // Extended Enabling Relation (Definition 8 [1])
                // Basically just adds the preservation of the invariant
                if (!pinv.isEmpty()) {
                    String andPInv = " & " + pinv;

                    // 1. can op1 enable op2
                    formulae.add(PropsAndInvsPre + ng2 + andBap1 + andPInv + andPG2);
                    // 2. can op1 disable op2
                    formulae.add(PropsAndInvsPre + g2 + andBap1 + andPInv + andPNG2);
                    // 3. can op1 keep op2 enabled
                    formulae.add(PropsAndInvsPre + g2 + andBap1 + andPInv + andPG2);
                    // 4. can op1 keep op2 disabled
                    formulae.add(PropsAndInvsPre + ng2 + andBap1 + andPInv + andPNG2);

                }

            }


        }


        return formulae.stream().map(BPredicate::new).collect(Collectors.toList());
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
     * before/after predicate.
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
     * @deprecated Use {@link #invariantPreservationFormulae(PredicateCollection)}
     *             and {@link #weakestPreconditionFormulae(PredicateCollection)}
     *             instead.
     *
     * @return
     */
    public static List<BPredicate> invariantPreservations(
            PredicateCollection predicateCollection) {

        String PropsPre = getPropertyPre(predicateCollection);
        String Invs = getInvariantString(predicateCollection);

        /*
         * Generate invariants preservation strings:
         * - Inv => weakestPre
         * - Inv & Precondition & before/after => Inv'
         */

        // Classical B: weakest precondition
        Map<String, Map<BPredicate, BPredicate>> weakestPreMap =
                predicateCollection.getWeakestPreConditions();

        // - for each operation
        List<BPredicate> formulae = new ArrayList<>();
        for (Entry<String, Map<BPredicate, BPredicate>> opEntry : weakestPreMap.entrySet()) {
            // - for each invariant
            for (Entry<BPredicate, BPredicate> invEntry : opEntry.getValue().entrySet()) {
                String inv = invEntry.getKey().toString();
                String wpc = invEntry.getValue().toString();

                formulae.add(BPredicate.of(PropsPre + inv + " & " + wpc));
                formulae.add(BPredicate.of(PropsPre + "(not(" + inv + ") => (" + wpc + "))"));
                formulae.add(BPredicate.of(PropsPre + inv + " & not(" + wpc + ")"));
                formulae.add(BPredicate.of(PropsPre + "(not(" + inv + ") => not(" + wpc + "))"));
            }
        }


        // Event B: before/after predicate
        if (predicateCollection.getMachineType() != MachineType.EVENTB)
            return formulae; // the following is for EVENTB only FIXME

        Map<BPredicate, BPredicate> primedInvsMap =
                predicateCollection.getPrimedInvariants();

        if (!primedInvsMap.isEmpty()) { // do only if the map is not empty
            // Collect all invariants plus their concatenation if more than 1
            List<BPredicate> invariants = new ArrayList<>(predicateCollection.getInvariants());
            if (invariants.size() > 1) {
                invariants.add(BPredicate.of(Invs));
            }
            for (BPredicate unprimedInv : invariants) {

                BPredicate primedInv = primedInvsMap.get(unprimedInv);
                // Skip if primed invariant was not properly collected
                if (primedInv == null) {
                    continue;
                }

                Map<String, List<BPredicate>> preconditions =
                        predicateCollection.getPreconditions();
                Map<String, BPredicate> beforeAfter =
                        predicateCollection.getBeforeAfterPredicates();

                for (String operation : beforeAfter.keySet()) {
                    String g; // the precondition of the operation (may be empty)

                    if (preconditions.containsKey(operation)) {
                        g = getPredicateConjunction(preconditions.get(operation)) + " & ";
                    } else {
                        g = "";
                    }

                    String gAndBa = g + beforeAfter.get(operation);

                    formulae.add(BPredicate.of(PropsPre + unprimedInv + " & " + gAndBa
                                               + " & " + primedInv));
                    formulae.add(BPredicate.of(PropsPre + "(not(" + unprimedInv + " & "
                                               + gAndBa + ") => (" + primedInv + "))"));
                    formulae.add(BPredicate.of(PropsPre + unprimedInv + " & " + gAndBa
                                               + " & not(" + primedInv + ")"));
                    formulae.add(BPredicate.of(PropsPre + "(not(" + unprimedInv + " & "
                                               + gAndBa + ") => not(" + primedInv + "))"));
                }

            }
        }

        return formulae;
    }

    public static List<BPredicate> preconditionConstraints(PredicateCollection pc) {
        List<String> formulae = new ArrayList<>();

        String PropsAndInvs = getPropertyAndInvariantString(pc);
        String PropsAndNInvs = getPropertyAndNegatedInvariantString(pc);
        boolean hasInvariant = pc.getInvariants().size() > 0;

        List<String> operations = pc.getOperationNames();
        Map<String, List<BPredicate>> preconditionLists = pc.getPreconditions();

        Map<String, BPredicate> preconditions = new HashMap<>();
        for (String operation : operations) {
            if (preconditionLists.containsKey(operation)) {
                BPredicate precond = getPredicateConjunction(preconditionLists.get(operation));
                preconditions.put(operation, precond);
            }
        }

        String PropsAndInvsPre = (PropsAndInvs.isEmpty())
                        ? ""
                        : PropsAndInvs + " & ";
        String PropsAndNInvsPre = (PropsAndInvs.isEmpty())
                        ? ""
                        : PropsAndNInvs + " & ";

        for (String op : preconditions.keySet()) {
            String g = "(" + preconditions.getOrDefault(op, new BPredicate("btrue")).getPredicate() + ")";
            String notG = "not" + g;
            String andG = " & " + g;
            String andNG = " & not" + g;

            // Preconditions that are satisfiable given the invariant is satisfied
            formulae.add(PropsAndInvsPre + g);
            // Preconditions that can be disabled given the invariant is satisfied
            formulae.add(PropsAndInvsPre + notG);

            if (!PropsAndInvs.isEmpty()) {
                // Precondition is always satisfied if invariant is satisfied
                formulae.add("(" + PropsAndInvs + ") => " + g);
                // Precondition can never be satisfied if invariant is satisfied
                formulae.add("(" + PropsAndInvs + ") => " + notG);
            }

            if (hasInvariant) {
                // Preconditions that are satisfiable given the invariant is violated
                formulae.add(PropsAndNInvsPre + g);
                // Preconditions that can be disabled given the invariant is violated
                formulae.add(PropsAndNInvsPre + notG);
                // Precondition is always satisfied if invariant is violated
                formulae.add("(" + PropsAndNInvs  + ") => " + g);
                // Precondition can never be satisfied if invariant is violated
                formulae.add("(" + PropsAndNInvs  + ") => " + notG);
            }
        }

        return formulae.stream().map(BPredicate::new).collect(Collectors.toList());

    }

    public static List<BPredicate> invariantConstrains(PredicateCollection pc) {
        List<String> invariants = pc.getInvariants().stream()
                .map(BPredicate::toString).collect(Collectors.toList());
        boolean hasInvariant = invariants.size() > 0;

        if (!hasInvariant) {
            // No sensible constraints to generate anymore.
            return new ArrayList<>();
        }

        List<String> formulae = new ArrayList<>();

        String PropertyPre = getPropertyPre(pc);
        String PropsAndInvs = getPropertyAndInvariantString(pc);
        String PropsAndNInvs = getPropertyAndNegatedInvariantString(pc);


        formulae.add(PropsAndInvs); // Satisfy invariant?
        formulae.add(PropsAndNInvs); // Violate invariant?

        if (invariants.size() == 1) {
            // No sensible constraints to generate anymore.
            return formulae.stream().map(BPredicate::new)
                    .collect(Collectors.toList());
        }

        for (int i = 0; i<invariants.size(); i++) {
            // we want to negate the ith conjunct
            String invWithNegation = "not(" + invariants.get(i) + ")";

            if (i > 0) {
                // Has a pre
                String pre = getStringConjunction(invariants.subList(0, i));
                invWithNegation = pre + " & " + invWithNegation;
            }
            if (i+1 < invariants.size()) {
                // Has a post
                String post = getStringConjunction(
                        invariants.subList(i+1, invariants.size()));
                invWithNegation += " & " + post;
            }

            formulae.add(PropertyPre + invWithNegation);
        }

        return formulae.stream().map(BPredicate::new).collect(Collectors.toList());

    }

    /**
     * Returns a list of invariant preservation proof obligations and other
     * formulae inspired by those.
     * <p>
     * The following formulae are generated:
     * <br>Let P be the properties. Let i be the conjunction of all invariants.
     * Further, let g be the precondition of an operation and ba the operations
     * before/after predicate.
     * Let j be the invariant after the operation (j:=i')
     * <ul>
     * <li> P & i & g & ba & j
     * <li> (P & i & g & ba) => j
     * <li> P & i & g & ba & ~j
     * <li> (P & i & g & ba) => ~j
     * <li> (not(P & i) & ba) => j
     * <li> (not(P & i) & ba & g) => j
     * </ul>
     *
     * @param pc
     *
     * @return
     */
    public static List<BPredicate> invariantPreservationFormulae(
            PredicateCollection pc) {
        if (pc.getInvariants().size() == 0 || pc.getPrimedInvariants().size() == 0) {
            // We don't have any sensible constraints to generate.
            return  new ArrayList<>();
        }

        List<String> formulae = new ArrayList<>();

        String PropsAndInvs = getPropertyAndInvariantString(pc);

        List<String> operations = pc.getOperationNames();
        Map<String, List<BPredicate>> preconditionLists = pc.getPreconditions();

        Map<String, BPredicate> preconditions = new HashMap<>();
        for (String operation : operations) {
            if (preconditionLists.containsKey(operation)) {
                BPredicate precond = getPredicateConjunction(preconditionLists.get(operation));
                preconditions.put(operation, precond);
            }
        }

        Map<String, BPredicate> beforeAfterPreds = pc.getBeforeAfterPredicates();

        String pinv = getPredicateConjunction(pc.getPrimedInvariants().values()).getPredicate();
        pinv = "(" + pinv + ")";
        String andPInv = " & " + pinv;
        String npinv = "not" + pinv;
        String andNPInv = " & not" + pinv;

        for (String op : beforeAfterPreds.keySet()) {
            String ba = "(" + beforeAfterPreds.get(op).getPredicate() + ")";
            String andBa = " & " + ba;

            String g = "(" + preconditions.getOrDefault(op, new BPredicate("btrue")).getPredicate() + ")";
            String andG = " & " + g;

            // Invariant preservation
            formulae.add(PropsAndInvs + andG + andBa + andPInv);
            formulae.add("(" + PropsAndInvs + andG + andBa + ") => " + pinv);
            // Invariant breaks
            formulae.add(PropsAndInvs + andG + andBa + andNPInv);
            formulae.add("(" + PropsAndInvs + andG + andBa + ") => " + npinv);

            // Operation repairs invariant
            formulae.add("(not(" + PropsAndInvs +")" + andBa + ") => " + pinv);
            // Enabled operation repairs invariant
            formulae.add("(not(" + PropsAndInvs +")" + andBa + andG + ") => " + pinv);
        }

        return formulae.stream().map(BPredicate::new).collect(Collectors.toList());
    }

    /**
     * Returns a list of formulae inspired by proof obligations based on
     * weakest preconditions.
     * <p>
     * The following formulae are generated:
     * <br>Let P be the properties. Let i be an invariant.
     * Let w be the weakest precondition of the respective invariant for an
     * operation in the machine.
     * Let ba be the before-after predicate of the respective operation and j the primed invariant.
     * Let g be the precondition of the respective operation.
     * <ul>
     * <li> P & i & g & w & ba & j
     * <li> P & i & & w & ba & j
     * <li> P & i & g & w & ba & ~j
     * <li> (P & i & g) => w
     * <li> (P & i & g) => ~w
     * <li> ~(P & i) & g & w
     * </ul>
     * Further, let W be the weakest precondition for the full invariant.
     * <ul>
     * <li> P & i & W & ba & j
     * <li> P & i & W & ba & ~j
     * <li> (P & i) => W
     * <li> (P & i) => ~W
     * <li> ~(P & i) & W
     * </ul>
     * Lastly, for two weakest preconditions W1 and W2:
     * <ul>
     * <li> P & i & W1 & W2
     * <li> P & i & W1 & ~W2
     * <li> P & i & ~W1 & W2
     * <li> (P & i & W1) => W2
     * <li> (P & i & W2) => W1
     * <li> (P & i & W1) => ~W2
     * <li> (P & i & W2) => ~W1
     * </ul>
     *
     *
     * @param pc
     *
     * @return
     */
    public static List<BPredicate> weakestPreconditionFormulae(
            PredicateCollection pc) {

        if (pc.getWeakestPreConditions().size() == 0) {
            // we cannot generate any sensible constraints
            return new ArrayList<>();
        }


        Set<String> formulae = new HashSet<>();

        String PropsAndInvs = getPropertyAndInvariantString(pc);
        String PropsAndInvsAnd = (PropsAndInvs.isEmpty() ? "" : PropsAndInvs + " & ");

        List<String> operations = pc.getOperationNames();
        Map<String, Map<BPredicate, BPredicate>> weakestPreConditions = pc.getWeakestPreConditions();
        Map<String, BPredicate> weakestFullPreConditions = pc.getWeakestFullPreconditions();

        Map<String, List<BPredicate>> preconditionLists = pc.getPreconditions();
        Map<String, BPredicate> preconditions = new HashMap<>();
        for (String operation : operations) {
            if (preconditionLists.containsKey(operation)) {
                BPredicate precond = getPredicateConjunction(preconditionLists.get(operation));
                preconditions.put(operation, precond);
            }
        }

        Map<String, BPredicate> beforeAfterPreds = pc.getBeforeAfterPredicates();

        String pinv = getPredicateConjunction(
                new ArrayList<>(pc.getPrimedInvariants().values())).getPredicate();
        String andPInv = " & (" + pinv + ")";
        String andNPInv = " & not(" + pinv + ")";
        String npinv = "not(" + pinv + ")";

        final String and = " & ";

        for(int i=0; i<operations.size(); i++) {
            String op = operations.get(i);
            String ba = "(" + beforeAfterPreds.get(op).getPredicate() + ")";

            String g = "(" + preconditions.getOrDefault(op, new BPredicate("btrue")).getPredicate() + ")";

            // Weakest preconditions for each invariant part
            for (BPredicate wp : weakestPreConditions.get(op).values()) {
                String wpc = "(" + wp.getPredicate() + ")";
                String nWpc = "not" + wpc;

                // WPC constraint itself
                formulae.add(PropsAndInvsAnd + wpc);
                // WPC constraint itself with precondition
                formulae.add(PropsAndInvsAnd + g + and + wpc);
                // WPC constraint itself negated
                formulae.add(PropsAndInvsAnd + nWpc);
                // WPC constraint itself negated with precondition
                formulae.add(PropsAndInvsAnd + g + and + nWpc);

                // invariant implies weakest precondition
                formulae.add("(" + PropsAndInvsAnd + g + ") => " + wpc);
                // invariant forbids weakest precondition
                formulae.add("(" + PropsAndInvsAnd + g + ") => not" + wpc);

                if (!PropsAndInvs.isEmpty()) {
                    // weakest precondition possible with broken invariant
                    formulae.add("not(" + PropsAndInvs + ") & " + g + and + wpc);
                }

                if (pc.getInvariants().size() > 0) {
                    // following formulae only make sense if we have a non-empty invariant

                    // weakest precondition holds and kills invariant
                    formulae.add(PropsAndInvsAnd + g + and + wpc + and + ba + andNPInv);
                    // Event enabled and part of WPC holds
                    formulae.add(PropsAndInvsAnd + g + and + wpc + and + ba + andPInv);
                    // Event undefined and part of WPC holds - might result in WD problems
                    formulae.add(PropsAndInvsAnd + wpc + and + ba + andPInv);
                }

            }

            // weakest precondition over full invariant
            String fwpc = "(" + weakestFullPreConditions.get(op) + ")";

            // weakest precondition constraint itself
            formulae.add(PropsAndInvsAnd + fwpc);
            // weakest precondition constraint itself
            formulae.add(PropsAndInvsAnd + "not" + fwpc);

            if (!PropsAndInvs.isEmpty()) {
                // invariant implies weakest precondition
                formulae.add("(" + PropsAndInvs + ") => " + fwpc);
                // invariant forbids weakest precondition
                formulae.add("(" + PropsAndInvs + ") => not" + fwpc);
                // weakest precondition possible with broken invariant
                formulae.add("not(" + PropsAndInvs + ")" + and + fwpc);
            }

            if (pc.getInvariants().size() > 0) {
                // weakest precondition holds and preserves invariant
                formulae.add(PropsAndInvsAnd + fwpc + and + ba + andPInv);
                // weakest precondition holds and kills invariant
                formulae.add(PropsAndInvsAnd + fwpc + and + ba + andNPInv);
            }

            // which two weakest full preconditions can be co-enabled?
            for (int j = i+1; j<operations.size(); j++) {
                String op2 = operations.get(j);

                // weakest precondition over full invariant
                String fwpc2 = "(" + weakestFullPreConditions.get(op2) + ")";

                // Both
                formulae.add(PropsAndInvsAnd + fwpc + and + fwpc2);
                // first not second
                formulae.add(PropsAndInvsAnd + fwpc + " & not" + fwpc2);
                // second not first
                formulae.add(PropsAndInvsAnd + "not" + fwpc + and + fwpc2);
                // first implies second
                formulae.add("(" + PropsAndInvsAnd + fwpc + ") => " + fwpc2);
                // second implies first
                formulae.add("(" + PropsAndInvsAnd + fwpc2 + ") => " + fwpc);
                // first denies second
                formulae.add("(" + PropsAndInvsAnd + fwpc + ") => not" + fwpc2);
                // second denies first
                formulae.add("(" + PropsAndInvsAnd + fwpc2 + ") => not" + fwpc);

            }
        }

        return formulae.stream().map(BPredicate::new).collect(Collectors.toList());
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
    public static List<BPredicate> assertions(PredicateCollection predicateCollection) {
        String propsAndInv = getPropertyAndInvariantString(predicateCollection);
        ArrayList<BPredicate> formulae = new ArrayList<>();

        List<BPredicate> assertionsList = new ArrayList<>(predicateCollection.getAssertions());
        // If no assertions, then return empty list
        if (assertionsList.isEmpty()) {
            return formulae;
        }

        // If more than one assertion, add conjunction to list as well
        if (assertionsList.size() > 1) {
            assertionsList.add(getPredicateConjunction(assertionsList));
        }

        if (propsAndInv.isEmpty()) {
            for (BPredicate a : assertionsList) {
                formulae.add(a);
                formulae.add(BPredicate.of("not(" + a + ")"));
            }
        } else {
            // proof assertions
            for (BPredicate a : assertionsList) {
                formulae.add(BPredicate.of(propsAndInv + " & " + a));
                formulae.add(BPredicate.of(propsAndInv + " & not(" + a + ")"));
                formulae.add(BPredicate.of("not(" + propsAndInv + ") => (" + a + ")"));
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

    public static BPredicate getPredicateConjunction(Collection<BPredicate> conjuncts) {
        if (conjuncts.size() == 0) {
            return BPredicate.of("");
        }

        String conjunction = conjuncts.stream()
                .map(BPredicate::toString)
                .collect(Collectors.joining(") & ("));

        return BPredicate.of("(" + conjunction + ")");
    }

    private static String getPropertyString(PredicateCollection predicateCollection) {
        return getPredicateConjunction(predicateCollection.getProperties()).toString();
    }

    private static String getInvariantString(PredicateCollection predicateCollection) {
        return getPredicateConjunction(predicateCollection.getInvariants()).toString();
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

    private static String getPropertyAndNegatedInvariantString(
            PredicateCollection predicateCollection) {
        String inv = getInvariantString(predicateCollection);

        if (inv.isEmpty())
            return getPropertyString(predicateCollection);


        return getPropertyPre(predicateCollection) + "not(" + inv + ")";
    }


    private static List<BPredicate> generateExtendedPreconditionFormulae(
            String properties, String invariants,
            Map<String, List<BPredicate>> allPreconditions) {
        List<BPredicate> formulae = new ArrayList<>();

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
            formulae.add(BPredicate.of(invariants)); // invariants
            invariantsPre = invariants + " & ";
            negInvariants = "not(" + invariants + ")";
        }


        String propsAndInvs = (invariants.isEmpty()) ? properties : propertyPre + invariants;
        String propsAndNegInvs = (invariants.isEmpty()) ? properties : propertyPre + negInvariants;


        // preconditions
        List<List<BPredicate>> allPreconditionsList = allPreconditions.entrySet()
                .stream()
                .map(Entry::getValue)
                .collect(Collectors.toList());

        for (List<BPredicate> preconditions : allPreconditionsList) {
            String precondition = getPredicateConjunction(preconditions).toString();

            // only continue if the preconditions are nonempty
            if (precondition.isEmpty()) {
                continue;
            }

            String negPrecondition = "not(" + precondition + ")";

            String propsAndPrecondition;
            propsAndPrecondition = propertyPre + precondition;
            String propsAndNegPrecondition = propertyPre + negPrecondition;

            // operations active w/o violating invariants
            formulae.add(BPredicate.of(propertyPre + invariantsPre + precondition));
            // following code only makes sense if invariants or properties
            // are not empty
            if (emptyInvariants && emptyProperties) {
                continue;
            }

            // operations usable with unviolated invariants
            formulae.add(BPredicate.of("not(" + propsAndInvs + ") => " + precondition));

            // operations not active w/o violating invariants
            formulae.add(BPredicate.of(propsAndInvs + " & " + negPrecondition));
            // operations not usable with unviolated invariants
            formulae.add(BPredicate.of("not(" + propsAndInvs + ") => " + negPrecondition));

            // operations only usable w/o invariant violation
            formulae.add(BPredicate.of("not(" + propsAndPrecondition + ") => " + invariants));
            // operations never usable w/o invariant violation
            formulae.add(BPredicate.of("not(" + propsAndNegPrecondition + ") => " + invariants));

            if (emptyInvariants) {
                // incoming formulae would be repetitive, so skip them
                continue;
            }

            // operations active despite invariant violation
            formulae.add(BPredicate.of(propsAndNegInvs + " & " + precondition));
            // operations usable despite invariant violation
            formulae.add(BPredicate.of("not(" + propsAndNegInvs + ") => " + precondition));

            // operations not active with invariant violation
            formulae.add(BPredicate.of(propsAndNegInvs + " & " + negPrecondition));
            // operations not usable with invariant violation
            formulae.add(BPredicate.of("not(" + propsAndNegInvs + ") => " + negPrecondition));

            // operations never usable with invariant violation
            formulae.add(BPredicate.of("not(" + propsAndNegPrecondition + ") => " + negInvariants));
            // operations only usable with invariant violation
            formulae.add(BPredicate.of("not(" + propsAndPrecondition + ") => " + negInvariants));
        }

        return formulae;
    }

}
