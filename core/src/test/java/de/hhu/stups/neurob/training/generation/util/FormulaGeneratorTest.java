package de.hhu.stups.neurob.training.generation.util;

import de.be4.classicalb.core.parser.BParser;
import de.hhu.stups.neurob.core.api.MachineType;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.PrimePredicateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.model.representation.AbstractModel;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FormulaGeneratorTest {
    private PredicateCollection pc;

    // Parts of the machine to test
    private List<BPredicate> invariants;
    private BPredicate invariantConcatenation;
    private List<String> operations;
    private Map<String, List<BPredicate>> preconditions;
    private List<BPredicate> properties;
    private BPredicate propertyConcatenation;
    private BPredicate propertyInvariantPre;
    private List<BPredicate> assertions;
    private Map<String, BPredicate> beforeAfterPredicates;
    private Map<String, Map<BPredicate, BPredicate>> weakestPreconditions;
    private Map<String, BPredicate> weakestFullPreconditions;
    private Map<BPredicate, BPredicate> primedInvariants;

    @BeforeAll
    public void stubPredicateCollection() {
        pc = mock(PredicateCollection.class);

        // Stub predicate collection
        invariants = new ArrayList<>();
        invariantConcatenation = BPredicate.of("(invariant1) & (invariant2)");
        invariants.add(BPredicate.of("invariant1"));
        invariants.add(BPredicate.of("invariant2"));
        when(pc.getInvariants()).thenReturn(invariants);

        operations = new ArrayList<>();
        operations.add("operation1");
        operations.add("operation2");
        when(pc.getOperationNames()).thenReturn(operations);

        preconditions = new HashMap<>();
        // of the form operationX-preconditionY - Y in {1,2}
        for (String operation : operations) {
            List<BPredicate> precond = new ArrayList<>();
            precond.add(BPredicate.of(operation + "-precondition1"));
            precond.add(BPredicate.of(operation + "-precondition2"));
            preconditions.put(operation, precond);
        }
        when(pc.getPreconditions()).thenReturn(preconditions);

        properties = new ArrayList<>();
        propertyConcatenation = BPredicate.of("properties");
        properties.add(BPredicate.of("properties"));
        when(pc.getProperties()).thenReturn(properties);
        propertyInvariantPre = BPredicate.of("(properties) & " + invariantConcatenation + " & ");

        assertions = new ArrayList<>();
        assertions.add(BPredicate.of("assertion1"));
        assertions.add(BPredicate.of("assertion2"));
        when(pc.getAssertions()).thenReturn(assertions);

        beforeAfterPredicates = new HashMap<>();
        // of the form operationX-beforeafter
        for (String operation : operations) {
            beforeAfterPredicates.put(operation, BPredicate.of(operation + "-beforeafter"));
        }
        when(pc.getBeforeAfterPredicates()).thenReturn(beforeAfterPredicates);

        weakestPreconditions = new HashMap<>();
        weakestFullPreconditions = new HashMap<>();
        // of the form operationX-invariantY-weakestpre (only one per op+inv)
        for (String operation : operations) {
            Map<BPredicate, BPredicate> wpcs = new HashMap<>();
            for (BPredicate invariant : invariants) {
                wpcs.put(invariant,
                        BPredicate.of(operation + "-" + invariant + "-weakestpre"));
            }
            weakestPreconditions.put(operation, wpcs);
            // Weakest precondition of invariant concatenation
            weakestFullPreconditions.put(operation,
                    BPredicate.of(operation + "-(" + invariantConcatenation + ")-weakestpre"));
        }
        when(pc.getWeakestPreConditions()).thenReturn(weakestPreconditions);
        when(pc.getWeakestFullPreconditions()).thenReturn(weakestFullPreconditions);

        primedInvariants = new HashMap<>();
        primedInvariants.put(BPredicate.of("invariant1"), BPredicate.of("invariant1'"));
        primedInvariants.put(BPredicate.of("invariant2"), BPredicate.of("invariant2'"));
//        primedInvariants.put(invariantConcatenation, BPredicate.of("primedinvariantconcat'"));
        when(pc.getPrimedInvariants()).thenReturn(primedInvariants);

        // use EventB formulae as well
        when(pc.getMachineType()).thenReturn(MachineType.EVENTB);

        // Mock StateSpace in PredicateCollector
        /*
         * Okay this is a bit tricky.
         * Calling FeatureGenerator.getPrimedPredicate(StateSpace, IBEvalElement)
         * executes a PrimePredicateCommand over the state space.
         *
         * For this to work properly, we need to mock the call to
         * ss.execute, where we feed mocked Prolog Bindings into, as we do not
         * want to query a real state space here.
         */
        StateSpace ss = mock(StateSpace.class);
        // Model queried for parsing
        AbstractModel model = mock(AbstractModel.class);
        when(model.parseFormula(
                "(operation1-precondition1) & (operation1-precondition2)"))
                .thenReturn(new EventB("primedop1"));
        when(model.parseFormula(
                "(operation2-precondition1) & (operation2-precondition2)"))
                .thenReturn(new EventB("primedop2"));
        when(ss.getModel()).thenReturn(model); // Stub returned model

        MachineAccess bMachine = mock(MachineAccess.class);
        when(bMachine.getStateSpace()).thenReturn(ss);
        doAnswer(invocation -> {
            ss.execute((AbstractCommand) invocation.getArgument(0));
            return null;
        }).when(bMachine).execute(any());

        when(pc.getBMachine()).thenReturn(bMachine); // to use state space mock
        /*
         * Okay this is a bit tricky.
         * Calling FeatureGenerator.getPrimedPredicate(StateSpace, IBEvalElement)
         * executes a PrimePredicateCommand over the state space.
         *
         * For this to work properly, we need to mock the call to
         * ss.execute, where we feed mocked Prolog Bindings into, as we do not
         * want to query a real state space here.
         */
        ISimplifiedROMap bindings = mock(ISimplifiedROMap.class);
        when(bindings.get(any()))
                .thenReturn(new CompoundPrologTerm("primed-operation"));
        doAnswer(invocation -> {
            PrimePredicateCommand ppc =
                    invocation.getArgument(0);
            ppc.processResult(bindings);
            return null;
        }).when(ss).execute(any(PrimePredicateCommand.class));

    }

    @Test
    public void shouldConcatenateInvariantsWithParenthesis() {
        List<String> stringInvariants =
                invariants.stream().map(BPredicate::toString).collect(Collectors.toList());
        assertEquals(invariantConcatenation,
                FormulaGenerator.getStringConjunction(stringInvariants),
                "Correct concatenation of list with Strings failed");
    }

    @Test
    public void shouldGiveEmptyPredicateWhenListIsEmpty() {
        assertEquals(BPredicate.of(""),
                FormulaGenerator.getPredicateConjunction(new ArrayList<>()),
                "Concatenated predicate should be empty");
    }

    @Test
    public void shouldGiveSinglePredicateWhenListContainsOnlyOnePredicate() {
        ArrayList<BPredicate> conjuncts = new ArrayList<>();
        conjuncts.add(BPredicate.of("predicate"));

        assertEquals(BPredicate.of("(predicate)"),
                FormulaGenerator.getPredicateConjunction(conjuncts),
                "Concatenated predicate should be the only one from list");
    }

    @Test
    public void shouldConcatenateInvariantPredicatesWithParenthesis() {
        assertEquals(invariantConcatenation,
                FormulaGenerator.getPredicateConjunction(invariants),
                "Correct concatenation of list with Strings failed");
    }

    @Test
    public void shouldGenerateCombinedFormulaeForEachOperationPreconditionsPair() {
        List<String> formulae =
                FormulaGenerator.multiPreconditionFormulae(pc).stream()
                        .map(BPredicate::toString)
                        .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();
        String pre1 = "(operation1-precondition1) & (operation1-precondition2)";
        String pre2 = "(operation2-precondition1) & (operation2-precondition2)";

        expected.add(propertyInvariantPre + pre2 + " & " + pre1);
        expected.add(propertyInvariantPre +
                     "(not(" + pre2 + ") => (" + pre1 + "))");
        expected.add(propertyInvariantPre + pre2 + " & not(" + pre1 + ")");
        expected.add(propertyInvariantPre +
                     "(" + pre2 + " => not(" + pre1 + "))");

        // Sort for equality comparison
        expected.sort(Comparator.naturalOrder());
        formulae.sort(Comparator.naturalOrder());

        assertEquals(expected, formulae,
                "Generated extended guard formulae do not match");
    }

    @Test
    public void shouldModelEnablingrelationShipsBetweenTwoOperations() {
        List<String> formulae = FormulaGenerator.enablingRelationships(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        String P = propertyInvariantPre.toString(); // for readability reasons shortened
        String op1 = "(operation1-precondition1) & (operation1-precondition2)"
                     + " & " + beforeAfterPredicates.get("operation1");
        String op2 = "(operation2-precondition1) & (operation2-precondition2)"
                     + " & " + beforeAfterPredicates.get("operation2");
        String primed1 =
                "primed-operation";
        String primed2 =
                "primed-operation";

        List<String> expected = new ArrayList<>();

        for (String op : new String[]{op1, op2}) {
            for (String primed : new String[]{primed1, primed2}) {
                expected.add(P + "(" + op + " & " + primed + ")");
                expected.add(P + "(" + op + " & not(" + primed + "))");

                expected.add(P + "not(" + op + ") & " + primed + "");
                expected.add(P + "not(" + op + ") & not(" + primed + ")");
                expected.add(P + "(not(" + op + ") => (" + primed + "))");
                expected.add(P + "(not(" + op + ") => not(" + primed + "))");
            }
        }

        // Sort for equality comparison
        expected.sort(Comparator.naturalOrder());
        formulae.sort(Comparator.naturalOrder());

        assertAll("Enabeling relationships",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of generated enabeling relations "
                        + "does not match"),
                () -> assertEquals(expected, formulae,
                        "Enabling relations not correct")
        );
    }

    @Test
    public void shouldNotGenerateEnablingRelationsWhenNoPreconditionsExist() {
        // need to mock personalised PredicateCollection
        PredicateCollection pc = mock(PredicateCollection.class);
        // ... with neither properties nor invariants
        when(pc.getProperties()).thenReturn(new ArrayList<>());
        List<BPredicate> invariants = new ArrayList<>();
        invariants.add(BPredicate.of("invariant")); // only one invariant
        when(pc.getInvariants()).thenReturn(invariants);
        when(pc.getOperationNames()).thenReturn(operations);
        // preconditions -- none
        Map<String, List<BPredicate>> preconditions = new HashMap<>();
        when(pc.getPreconditions()).thenReturn(preconditions);
        // before/after predicates
        Map<String, BPredicate> beforeAfter = new HashMap<>();
        beforeAfter.put("operation1", BPredicate.of("beforeAfter1"));
        beforeAfter.put("operation2", BPredicate.of("beforeAfter2"));
        when(pc.getBeforeAfterPredicates()).thenReturn(beforeAfter);
        // only makes sense for EventB
        when(pc.getMachineType()).thenReturn(MachineType.EVENTB);

        List<String> formulae = FormulaGenerator.enablingRelationships(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        assertEquals(0, formulae.size(),
                "Created predicates but should not have done so");
    }

    @Test
    @Disabled("Outdated")
    public void shouldGenerateInvariantPreservationPredicates() {
        List<String> formulae = FormulaGenerator.invariantPreservations(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        String P = "(" + propertyConcatenation + ")"; // shortened for readability

        // As it is expected to generate formulae for each invariant
        // individually and for the concatenation,
        // we shadow this.invariants with a version, that contains the
        // concatenation of invariants as well.
        List<BPredicate> invariants = new ArrayList<>();
        invariants.add(invariantConcatenation); // concatenation
        invariants.addAll(this.invariants); // each individually

        List<String> expected = new ArrayList<>();
        for (BPredicate inv : invariants) {
            for (String op : operations) {
                // Weakest preconditions of the operation
                Map<BPredicate, BPredicate> wps = weakestPreconditions.get(op);

                // For all B machines
                expected.add(P + " & " + inv + " & " + wps.get(inv));
                expected.add(P + " & " + inv + " & "
                             + "not(" + wps.get(inv) + ")");
                expected.add(P + " & (not(" + inv + ") => ("
                             + wps.get(inv) + "))");
                expected.add(P + " & (not(" + inv + ") => "
                             + "not(" + wps.get(inv) + "))");

                // For EventB only
                String precond = FormulaGenerator.getPredicateConjunction(
                        preconditions.get(op)).toString();

                // Shorthand for concatenation of invariant, precond, and
                // before/after predicate
                String invAndBA = inv + " & " + precond
                                  + " & " + beforeAfterPredicates.get(op);

                expected.add(P + " & " + invAndBA + " & "
                             + primedInvariants.get(inv));
                expected.add(P + " & " + invAndBA + " & "
                             + "not(" + primedInvariants.get(inv) + ")");
                expected.add(P + " & (not(" + invAndBA + ") => ("
                             + primedInvariants.get(inv) + "))");
                expected.add(P + " & (not(" + invAndBA + ") => "
                             + "not(" + primedInvariants.get(inv) + "))");

            }
        }

        // Sort for equality comparison
        expected.sort(Comparator.naturalOrder());
        formulae.sort(Comparator.naturalOrder());

        assertAll("Invariant preservations",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of generated preservation predicates "
                        + "does not match"),
                () -> assertEquals(expected, formulae,
                        "invariant preservation predicates are not correct")
        );

    }

    @Test
    void shouldGenerateInvariantPreservationFormulae() {
        List<String> formulae = FormulaGenerator.invariantPreservationFormulae(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();
        String commonPre = "(properties) & (invariant1) & (invariant2)";
        // operation1
        String op1pre = commonPre + " & ((operation1-precondition1) & (operation1-precondition2))";
        expected.add(op1pre + " & (operation1-beforeafter) & ((invariant1') & (invariant2'))");
        expected.add("(" + op1pre + " & (operation1-beforeafter)) => ((invariant1') & (invariant2'))");
        expected.add(op1pre + " & (operation1-beforeafter) & not((invariant1') & (invariant2'))");
        expected.add("(" + op1pre + " & (operation1-beforeafter)) => not((invariant1') & (invariant2'))");
        expected.add("(not(" + commonPre + ") & (operation1-beforeafter)) => ((invariant1') & (invariant2'))");
        expected.add("(not(" + commonPre + ") & (operation1-beforeafter) & ((operation1-precondition1) & (operation1-precondition2))) => ((invariant1') & (invariant2'))");
        // operation2
        String op2pre = commonPre + " & ((operation2-precondition1) & (operation2-precondition2))";
        expected.add(op2pre + " & (operation2-beforeafter) & ((invariant1') & (invariant2'))");
        expected.add("(" + op2pre + " & (operation2-beforeafter)) => ((invariant1') & (invariant2'))");
        expected.add(op2pre + " & (operation2-beforeafter) & not((invariant1') & (invariant2'))");
        expected.add("(" + op2pre + " & (operation2-beforeafter)) => not((invariant1') & (invariant2'))");
        expected.add("(not(" + commonPre + ") & (operation2-beforeafter)) => ((invariant1') & (invariant2'))");
        expected.add("(not(" + commonPre + ") & (operation2-beforeafter) & ((operation2-precondition1) & (operation2-precondition2))) => ((invariant1') & (invariant2'))");

        // Sort for equality comparison
        expected.sort(Comparator.naturalOrder());
        formulae.sort(Comparator.naturalOrder());

        assertAll("Assertions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of assertions does not match"),
                () -> assertEquals(expected, formulae,
                        "Assertion predicates are not correct")
        );
    }

    @Test
    void shouldHaveEmptyInvariantPreservationFormulaeWhenNoInvariantsExist() {
        when(pc.getInvariants()).thenReturn(new ArrayList<>());
        pc.getInvariants(); // Use up the initial call
        when(pc.getPrimedInvariants()).thenReturn(new HashMap<>());
        pc.getPrimedInvariants(); // Use up the initial call
        List<String> formulae = FormulaGenerator.invariantPreservationFormulae(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();
        assertAll("Assertions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of assertions does not match"),
                () -> assertEquals(expected, formulae,
                        "Assertion predicates are not correct")
        );
    }

    @Test
    void shouldGenerateEmptyInvariantPreservationFormulaeWhenNoPrimedInvariants() {
        when(pc.getPrimedInvariants()).thenReturn(new HashMap<>());
        pc.getPrimedInvariants(); // Use up the initial call
        List<String> formulae = FormulaGenerator.invariantPreservationFormulae(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();

        assertAll("Assertions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of assertions does not match"),
                () -> assertEquals(expected, formulae,
                        "Assertion predicates are not correct")
        );
    }

    @Test
    void shouldGenerateEmptyInvariantPreservationFormulaeWhenNoBeforeAfter() {
        when(pc.getBeforeAfterPredicates()).thenReturn(new HashMap<>());
        pc.getBeforeAfterPredicates(); // Use up the initial call
        List<String> formulae = FormulaGenerator.invariantPreservationFormulae(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();

        assertAll("Assertions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of assertions does not match"),
                () -> assertEquals(expected, formulae,
                        "Assertion predicates are not correct")
        );
    }

    @Test
    void shouldGenerateWeakestPreFormulae() {
        List<String> formulae = FormulaGenerator.weakestPreconditionFormulae(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();
        String commonPre = "(properties) & (invariant1) & (invariant2)";
        String op1precondition = "((operation1-precondition1) & (operation1-precondition2))";
        String op2precondition = "((operation2-precondition1) & (operation2-precondition2))";
        String op1ba = "(operation1-beforeafter)";
        String op2ba = "(operation2-beforeafter)";
        String pinv = "((invariant1') & (invariant2'))";
        // operation1 - invariant 1
        expected.add(commonPre + " & (operation1-invariant1-weakestpre)");
        expected.add(commonPre + " & " + op1precondition + " & (operation1-invariant1-weakestpre)");
        expected.add(commonPre + " & not(operation1-invariant1-weakestpre)");
        expected.add(commonPre + " & " + op1precondition + " & not(operation1-invariant1-weakestpre)");
        expected.add(commonPre + " & " + op1precondition + " & (operation1-invariant1-weakestpre) & "
                     + op1ba + " & " + pinv);
        expected.add(commonPre + " & (operation1-invariant1-weakestpre) & "
                     + op1ba + " & " + pinv);
        expected.add(commonPre + " & " + op1precondition + " & (operation1-invariant1-weakestpre) & "
                     + op1ba + " & not" + pinv);
        expected.add("(" + commonPre + " & " + op1precondition + ") => (operation1-invariant1-weakestpre)");
        expected.add("(" + commonPre + " & " + op1precondition + ") => not(operation1-invariant1-weakestpre)");
        expected.add("not(" + commonPre + ") & " + op1precondition + " & (operation1-invariant1-weakestpre)");
        // operation1 - invariant 2
        expected.add(commonPre + " & (operation1-invariant2-weakestpre)");
        expected.add(commonPre + " & " + op1precondition + " & (operation1-invariant2-weakestpre)");
        expected.add(commonPre + " & not(operation1-invariant2-weakestpre)");
        expected.add(commonPre + " & " + op1precondition + " & not(operation1-invariant2-weakestpre)");
        expected.add(commonPre + " & " + op1precondition + " & (operation1-invariant2-weakestpre) & "
                     + op1ba + " & " + pinv);
        expected.add(commonPre + " & (operation1-invariant2-weakestpre) & "
                     + op1ba + " & " + pinv);
        expected.add(commonPre + " & " + op1precondition + " & (operation1-invariant2-weakestpre) & "
                     + op1ba + " & not" + pinv);
        expected.add("(" + commonPre + " & " + op1precondition + ") => (operation1-invariant2-weakestpre)");
        expected.add("(" + commonPre + " & " + op1precondition + ") => not(operation1-invariant2-weakestpre)");
        expected.add("not(" + commonPre + ") & " + op1precondition + " & (operation1-invariant2-weakestpre)");
        // operation1 - full invariant
        String fwpc1 = "(operation1-((invariant1) & (invariant2))-weakestpre)";
        expected.add(commonPre + " & " + fwpc1);
        expected.add(commonPre + " & not" + fwpc1);
        expected.add(commonPre + " & " + fwpc1 + " & " + op1ba + " & " + pinv);
        expected.add(commonPre + " & " + fwpc1 + " & " + op1ba + " & not" + pinv);
        expected.add("(" + commonPre + ") => " + fwpc1);
        expected.add("(" + commonPre + ") => not" + fwpc1);
        expected.add("not(" + commonPre + ") & " + fwpc1);
        // operation1 - co-enabledness
        String fwpc2 = "(operation2-((invariant1) & (invariant2))-weakestpre)";
        expected.add(commonPre + " & " + fwpc1 + " & " + fwpc2);
        expected.add(commonPre + " & " + fwpc1 + " & not" + fwpc2 + "");
        expected.add(commonPre + " & not" + fwpc1 + " & " + fwpc2);
        expected.add("(" + commonPre + " & " + fwpc1 + ") => " + fwpc2);
        expected.add("(" + commonPre + " & " + fwpc2 + ") => " + fwpc1);
        expected.add("(" + commonPre + " & " + fwpc1 + ") => not" + fwpc2);
        expected.add("(" + commonPre + " & " + fwpc2 + ") => not" + fwpc1);


        // operation2 - invariant 1
        expected.add(commonPre + " & (operation2-invariant1-weakestpre)");
        expected.add(commonPre + " & " + op2precondition + " & (operation2-invariant1-weakestpre)");
        expected.add(commonPre + " & not(operation2-invariant1-weakestpre)");
        expected.add(commonPre + " & " + op2precondition + " & not(operation2-invariant1-weakestpre)");
        expected.add(commonPre + " & " + op2precondition + " & (operation2-invariant1-weakestpre) & "
                     + op2ba + " & " + pinv);
        expected.add(commonPre + " & (operation2-invariant1-weakestpre) & "
                     + op2ba + " & " + pinv);
        expected.add(commonPre + " & " + op2precondition + " & (operation2-invariant1-weakestpre) & "
                     + op2ba + " & not" + pinv);
        expected.add("(" + commonPre + " & " + op2precondition + ") => (operation2-invariant1-weakestpre)");
        expected.add("(" + commonPre + " & " + op2precondition + ") => not(operation2-invariant1-weakestpre)");
        expected.add("not(" + commonPre + ") & " + op2precondition + " & (operation2-invariant1-weakestpre)");
        // operation2 - invariant 2
        expected.add(commonPre + " & (operation2-invariant2-weakestpre)");
        expected.add(commonPre + " & " + op2precondition + " & (operation2-invariant2-weakestpre)");
        expected.add(commonPre + " & not(operation2-invariant2-weakestpre)");
        expected.add(commonPre + " & " + op2precondition + " & not(operation2-invariant2-weakestpre)");
        expected.add(commonPre + " & " + op2precondition + " & (operation2-invariant2-weakestpre) & "
                     + op2ba + " & " + pinv);
        expected.add(commonPre + " & (operation2-invariant2-weakestpre) & "
                     + op2ba + " & " + pinv);
        expected.add(commonPre + " & " + op2precondition + " & (operation2-invariant2-weakestpre) & "
                     + op2ba + " & not" + pinv + "");
        expected.add("(" + commonPre + " & " + op2precondition + ") => (operation2-invariant2-weakestpre)");
        expected.add("(" + commonPre + " & " + op2precondition + ") => not(operation2-invariant2-weakestpre)");
        expected.add("not(" + commonPre + ") & " + op2precondition + " & (operation2-invariant2-weakestpre)");
        // operation2 - full invariant
        expected.add(commonPre + " & " + fwpc2);
        expected.add(commonPre + " & not" + fwpc2);
        expected.add(commonPre + " & " + fwpc2 + " & " + op2ba + " & " + pinv);
        expected.add(commonPre + " & " + fwpc2 + " & " + op2ba + " & not" + pinv);
        expected.add("(" + commonPre + ") => " + fwpc2);
        expected.add("(" + commonPre + ") => not" + fwpc2);
        expected.add("not(" + commonPre + ") & " + fwpc2);


        expected.sort(Comparator.naturalOrder());
        formulae.sort(Comparator.naturalOrder());
        assertAll("Weakest preconditions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of weakest preconditions does not match"),
                () -> assertEquals(expected, formulae,
                        "Weakest precondition predicates are not correct")
        );
    }

    @Test
    void shouldGenerateEmptyWeakestPreFormulaeIfNoWeakestPre() {
        when(pc.getWeakestPreConditions()).thenReturn(new HashMap<>());
        pc.getWeakestPreConditions(); // Use up the initial call
        List<String> formulae = FormulaGenerator.weakestPreconditionFormulae(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();
        assertAll("Weakest preconditions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of weakest preconditions does not match"),
                () -> assertEquals(expected, formulae,
                        "Weakest precondition predicates are not correct")
        );
    }

    @Test
    void shouldGenerateWeakestPreFormulaeWhenNoInvariantNorProperties() {
        when(pc.getInvariants()).thenReturn(new ArrayList<>());
        pc.getInvariants(); // Use up the initial call
        when(pc.getProperties()).thenReturn(new ArrayList<>());
        pc.getProperties(); // Use up the initial call

        List<String> formulae = FormulaGenerator.weakestPreconditionFormulae(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();
        String op1precondition = "((operation1-precondition1) & (operation1-precondition2))";
        String op2precondition = "((operation2-precondition1) & (operation2-precondition2))";
        String op1ba = "(operation1-beforeafter)";
        String op2ba = "(operation2-beforeafter)";
        String pinv = "((invariant1') & (invariant2'))";
        // operation1 - invariant 1
        expected.add("(operation1-invariant1-weakestpre)");
        expected.add(op1precondition + " & (operation1-invariant1-weakestpre)");
        expected.add("not(operation1-invariant1-weakestpre)");
        expected.add(op1precondition + " & not(operation1-invariant1-weakestpre)");
        expected.add("(" + op1precondition + ") => (operation1-invariant1-weakestpre)");
        expected.add("(" + op1precondition + ") => not(operation1-invariant1-weakestpre)");
        // operation1 - invariant 2
        expected.add("(operation1-invariant2-weakestpre)");
        expected.add(op1precondition + " & (operation1-invariant2-weakestpre)");
        expected.add("not(operation1-invariant2-weakestpre)");
        expected.add(op1precondition + " & not(operation1-invariant2-weakestpre)");
        expected.add("(" + op1precondition + ") => (operation1-invariant2-weakestpre)");
        expected.add("(" + op1precondition + ") => not(operation1-invariant2-weakestpre)");
        // operation1 - full invariant
        String fwpc1 = "(operation1-((invariant1) & (invariant2))-weakestpre)";
        expected.add(fwpc1);
        expected.add("not" + fwpc1);
        // operation1 - co-enabledness
        String fwpc2 = "(operation2-((invariant1) & (invariant2))-weakestpre)";
        expected.add(fwpc1 + " & " + fwpc2);
        expected.add(fwpc1 + " & not" + fwpc2 + "");
        expected.add("not" + fwpc1 + " & " + fwpc2);
        expected.add("(" + fwpc1 + ") => " + fwpc2);
        expected.add("(" + fwpc2 + ") => " + fwpc1);
        expected.add("(" + fwpc1 + ") => not" + fwpc2);
        expected.add("(" + fwpc2 + ") => not" + fwpc1);


        // operation2 - invariant 1
        expected.add("(operation2-invariant1-weakestpre)");
        expected.add(op2precondition + " & (operation2-invariant1-weakestpre)");
        expected.add("not(operation2-invariant1-weakestpre)");
        expected.add(op2precondition + " & not(operation2-invariant1-weakestpre)");
        expected.add("(" + op2precondition + ") => (operation2-invariant1-weakestpre)");
        expected.add("(" + op2precondition + ") => not(operation2-invariant1-weakestpre)");
        // operation2 - invariant 2
        expected.add("(operation2-invariant2-weakestpre)");
        expected.add(op2precondition + " & (operation2-invariant2-weakestpre)");
        expected.add("not(operation2-invariant2-weakestpre)");
        expected.add(op2precondition + " & not(operation2-invariant2-weakestpre)");
        expected.add("(" + op2precondition + ") => (operation2-invariant2-weakestpre)");
        expected.add("(" + op2precondition + ") => not(operation2-invariant2-weakestpre)");
        // operation2 - full invariant
        expected.add(fwpc2);
        expected.add("not" + fwpc2);


        expected.sort(Comparator.naturalOrder());
        formulae.sort(Comparator.naturalOrder());
        assertAll("Weakest preconditions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of weakest preconditions does not match"),
                () -> assertEquals(expected, formulae,
                        "Weakest precondition predicates are not correct")
        );
    }

    @Test
    public void shouldPrependAssertionsWithPropertiesAndInvariants() {
        List<String> formulae = FormulaGenerator.assertions(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();
        String P = propertyInvariantPre.toString();
        expected.add(P + "assertion1");
        expected.add(P + "not(assertion1)");
        expected.add("not((" + propertyConcatenation + ") & "
                     + invariantConcatenation + ") => (assertion1)");
        expected.add(P + "assertion2");
        expected.add(P + "not(assertion2)");
        expected.add("not((" + propertyConcatenation + ") & "
                     + invariantConcatenation + ") => (assertion2)");
        expected.add(P + "(assertion1) & (assertion2)");
        expected.add(P + "not((assertion1) & (assertion2))");
        expected.add("not((" + propertyConcatenation + ") & "
                     + invariantConcatenation +
                     ") => ((assertion1) & (assertion2))");

        // Sort for equality comparison
        expected.sort(Comparator.naturalOrder());
        formulae.sort(Comparator.naturalOrder());

        assertAll("Assertions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of assertions does not match"),
                () -> assertEquals(expected, formulae,
                        "Assertion predicates are not correct")
        );
    }

    @Test
    public void shouldOnlyListAssertionsWhenNoPropertiesAndInvariantsDefined() {
        // need to mock personalised PredicateCollection
        PredicateCollection pc = mock(PredicateCollection.class);
        // ... with neither properties nor invariants
        when(pc.getProperties()).thenReturn(new ArrayList<>());
        when(pc.getInvariants()).thenReturn(new ArrayList<>());
        // ... but with assertions
        when(pc.getAssertions()).thenReturn(assertions);

        List<String> formulae = FormulaGenerator.assertions(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        List<String> expected = new ArrayList<>();
        expected.add("assertion1");
        expected.add("not(assertion1)");
        expected.add("assertion2");
        expected.add("not(assertion2)");
        expected.add("(assertion1) & (assertion2)");
        expected.add("not((assertion1) & (assertion2))");

        // Sort for equality comparison
        expected.sort(Comparator.naturalOrder());
        formulae.sort(Comparator.naturalOrder());

        assertAll("Assertions",
                () -> assertEquals(expected.size(), formulae.size(),
                        "Number of assertions does not match"),
                () -> assertEquals(expected, formulae,
                        "Assertion predicates are not correct")
        );
    }

    @Test
    public void shouldNotUseInvariantConcatenationAsWellWhenOnlyOneInvariantExists() {
        PredicateCollection pc = mock(PredicateCollection.class);
        List<BPredicate> invariant = new ArrayList<>();
        when(pc.getMachineType()).thenReturn(MachineType.EVENTB);
        // Invariant and primed invariant
        invariant.add(BPredicate.of("invariant"));
        when(pc.getInvariants()).thenReturn(invariant);
        List<String> primedInvariant = new ArrayList<>();
        primedInvariant.add("primed");
        Map<BPredicate, BPredicate> primedMap = new HashMap<>();
        primedMap.put(BPredicate.of("invariant"), BPredicate.of("primed"));
        primedMap.put(BPredicate.of("(invariant)"), BPredicate.of("primedconcat")); // primed concatenation
        when(pc.getPrimedInvariants()).thenReturn(primedMap);
        // Preconditions and before/after predicates
        List<BPredicate> precondition = new ArrayList<>();
        precondition.add(BPredicate.of("precondition"));
        Map<String, List<BPredicate>> operationPrecondition = new HashMap<>();
        operationPrecondition.put("operation", precondition);
        when(pc.getPreconditions()).thenReturn(operationPrecondition);
        Map<String, BPredicate> beforeAfter = new HashMap<>();
        beforeAfter.put("operation", BPredicate.of("beforeAfter"));
        when(pc.getBeforeAfterPredicates()).thenReturn(beforeAfter);
        // Necessary Stubs that are called but may be empty
        when(pc.getProperties()).thenReturn(new ArrayList<>());
        when(pc.getWeakestPreConditions()).thenReturn(new HashMap<>());

        List<String> expected = new ArrayList<>();
        expected.add("invariant & (precondition) & beforeAfter & primed");
        expected.add("invariant & (precondition) & beforeAfter & not(primed)");
        expected.add("(not(invariant & (precondition) & beforeAfter) => (primed))");
        expected.add("(not(invariant & (precondition) & beforeAfter) => not(primed))");

        List<String> actual = FormulaGenerator.invariantPreservations(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        expected.sort(Comparator.naturalOrder());
        actual.sort(Comparator.naturalOrder());

        assertEquals(expected, actual,
                "Generated invariant preservations do not match");
    }

    @Test
    public void shouldNotUseAssertionConcatenationWhenOnlyOneAssertionExists() {
        PredicateCollection pc = mock(PredicateCollection.class);
        List<BPredicate> assertions = new ArrayList<>();
        assertions.add(BPredicate.of("assertion"));
        when(pc.getAssertions()).thenReturn(assertions);
        when(pc.getProperties()).thenReturn(new ArrayList<>());
        when(pc.getInvariants()).thenReturn(new ArrayList<>());

        List<String> expected = new ArrayList<>();
        expected.add("assertion");
        expected.add("not(assertion)");
        List<String> actual = FormulaGenerator.assertions(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        assertEquals(expected, actual,
                "Should only have generated one assertion"
                + " and its negation");
    }

    @Test
    public void shouldGenerateAssertionsWithConcatenationWhenMoreThenOneExist() {
        PredicateCollection pc = mock(PredicateCollection.class);
        List<BPredicate> assertions = new ArrayList<>();
        assertions.add(BPredicate.of("assertion1"));
        assertions.add(BPredicate.of("assertion2"));
        when(pc.getAssertions()).thenReturn(assertions);
        when(pc.getProperties()).thenReturn(new ArrayList<>());
        when(pc.getInvariants()).thenReturn(new ArrayList<>());

        List<String> expected = new ArrayList<>();
        expected.add("assertion1");
        expected.add("not(assertion1)");
        expected.add("assertion2");
        expected.add("not(assertion2)");
        expected.add("(assertion1) & (assertion2)");
        expected.add("not((assertion1) & (assertion2))");
        List<String> actual = FormulaGenerator.assertions(pc)
                .stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        assertEquals(expected, actual,
                "Should generate formulae for each assertion and their "
                + "concatenation");
    }

    @Test
    void shouldCleanupAst() throws MachineAccessException, FormulaException {
        MachineAccess mch = new MachineAccess(Paths.get(getClass().getClassLoader().getResource("db/mch/bvm.mch").getPath()));
        ClassicalB pred = new ClassicalB("#x.(y>2 & x=y)", FormulaExpand.EXPAND);

        BPredicate cleanup = FormulaGenerator.cleanupAst(mch, pred);

        String expected = "y > 2";
        String actual = cleanup.getPredicate();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCleanupAst2() throws MachineAccessException, FormulaException {
        MachineAccess mch = new MachineAccess(Paths.get(getClass().getClassLoader().getResource("db/mch/bvm.mch").getPath()));
        ClassicalB pred = new ClassicalB("x:INTEGER & x>2", FormulaExpand.EXPAND);

        BPredicate cleanup = FormulaGenerator.cleanupAst(mch, pred);

        String expected = "x > 2";
        String actual = cleanup.getPredicate();

        assertEquals(expected, actual);
    }

}
