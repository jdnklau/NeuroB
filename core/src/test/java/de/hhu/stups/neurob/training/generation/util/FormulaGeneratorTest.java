package de.hhu.stups.neurob.training.generation.util;

import de.hhu.stups.neurob.core.api.MachineType;

import de.prob.animator.command.PrimePredicateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.model.representation.AbstractModel;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FormulaGeneratorTest {
    private PredicateCollection pc;

    // Parts of the machine to test
    private List<String> invariants;
    private String invariantConcatenation;
    private List<String> operations;
    private Map<String, List<String>> preconditions;
    private List<String> properties;
    private String propertyConcatenation;
    private String propertyInvariantPre;
    private List<String> assertions;
    private Map<String, String> beforeAfterPredicates;
    private Map<String, Map<String, String>> weakestPreconditions;
    private Map<String, String> primedInvariants;

    @BeforeAll
    public void stubPredicateCollection() throws Exception {
        pc = mock(PredicateCollection.class);

        // Stub predicate collection
        invariants = new ArrayList<>();
        invariantConcatenation = "(invariant1) & (invariant2)";
        invariants.add("invariant1");
        invariants.add("invariant2");
        when(pc.getInvariants()).thenReturn(invariants);

        operations = new ArrayList<>();
        operations.add("operation1");
        operations.add("operation2");
        when(pc.getOperationNames()).thenReturn(operations);

        preconditions = new HashMap<>();
        // of the form operationX-preconditionY - Y in {1,2}
        for (String operation : operations) {
            List<String> precond = new ArrayList<>();
            precond.add(operation + "-precondition1");
            precond.add(operation + "-precondition2");
            preconditions.put(operation, precond);
        }
        when(pc.getPreconditions()).thenReturn(preconditions);

        properties = new ArrayList<>();
        propertyConcatenation = "properties";
        properties.add("properties");
        when(pc.getProperties()).thenReturn(properties);
        propertyInvariantPre = "(properties) & " + invariantConcatenation + " & ";

        assertions = new ArrayList<>();
        assertions.add("assertion1");
        assertions.add("assertion2");
        when(pc.getAssertions()).thenReturn(assertions);

        beforeAfterPredicates = new HashMap<>();
        // of the form operationX-beforeafter
        for (String operation : operations) {
            beforeAfterPredicates.put(operation, operation + "-beforeafter");
        }
        when(pc.getBeforeAfterPredicates()).thenReturn(beforeAfterPredicates);

        weakestPreconditions = new HashMap<>();
        // of the form operationX-invariantY-weakestpre (only one per op+inv)
        for (String operation : operations) {
            Map<String, String> wpcs = new HashMap<>();
            for (String invariant : invariants) {
                wpcs.put(invariant,
                        operation + "-" + invariant + "-weakestpre");
            }
            // Weakest precondition of invariant concatenation
            wpcs.put(invariantConcatenation,
                    operation + "-(" + invariantConcatenation + ")-weakestpre");
            weakestPreconditions.put(operation, wpcs);
        }
        when(pc.getWeakestPreConditions()).thenReturn(weakestPreconditions);

        primedInvariants = new HashMap<>();
        primedInvariants.put("invariant1", "invariant1'");
        primedInvariants.put("invariant2", "invariant2'");
        primedInvariants.put(invariantConcatenation, "primedinvariantconcat'");
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
        when(pc.accessStateSpace()).thenReturn(ss); // to use state space mock
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
        doAnswer((Answer) invocation -> {
            PrimePredicateCommand ppc =
                    (PrimePredicateCommand) invocation.getArgument(0);
            ppc.processResult(bindings);
            return null;
        }).when(ss).execute(any(PrimePredicateCommand.class));

    }

    @Test
    public void shouldGenerateClassicalBInstanceForMachineTypeClassicalB()
            throws Exception {
        String pred = "x>y & y>x";

        IBEvalElement evalElem = FormulaGenerator
                .generateBCommandByMachineType(MachineType.CLASSICALB, pred);

        assertEquals(ClassicalB.class, evalElem.getClass(),
                "Wrong object created!");
    }

    @Test
    public void shouldGenerateEventBInstanceForMachineTypeEventB()
            throws Exception {
        String pred = "x>y & y>x";

        IBEvalElement evalElem = FormulaGenerator
                .generateBCommandByMachineType(MachineType.EVENTB, pred);

        assertEquals(EventB.class, evalElem.getClass(),
                "Wrong object created!");
    }

    @Test
    public void shouldConcatenateInvariantsWithParenthesis() {
        assertEquals(invariantConcatenation,
                FormulaGenerator.getStringConjunction(invariants),
                "Correct concatenation of list with Strings failed");
    }

    @Test
    public void shouldGenerateCombinedFormulaeForEachOperationPreconditionsPair() {
        List<String> formulae = FormulaGenerator.multiPreconditionFormulae(pc);

        List<String> expected = new ArrayList<>();
        String pre1 = "(operation1-precondition1) & (operation1-precondition2)";
        String pre2 = "(operation2-precondition1) & (operation2-precondition2)";

        expected.add(propertyInvariantPre + pre2 + " & " + pre1);
        expected.add(propertyInvariantPre +
                     "(not(" + pre2 + ") => " + pre1 + ")");
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
        List<String> formulae = FormulaGenerator.enablingRelationships(pc);

        String P = propertyInvariantPre; // for readability reasons shortened
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
                expected.add(P + "(not(" + op + ") => " + primed + ")");
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
        List<String> invariants = new ArrayList<>();
        invariants.add("invariant"); // only one invariant
        when(pc.getInvariants()).thenReturn(invariants);
        when(pc.getOperationNames()).thenReturn(operations);
        // preconditions -- none
        Map<String, List<String>> preconditions = new HashMap<>();
        when(pc.getPreconditions()).thenReturn(preconditions);
        // before/after predicates
        Map<String, String> beforeAfter = new HashMap<>();
        beforeAfter.put("operation1", "beforeAfter1");
        beforeAfter.put("operation2", "beforeAfter2");
        when(pc.getBeforeAfterPredicates()).thenReturn(beforeAfter);
        // only makes sense for EventB
        when(pc.getMachineType()).thenReturn(MachineType.EVENTB);

        List<String> formulae = FormulaGenerator.enablingRelationships(pc);

        assertEquals(0, formulae.size(),
                "Created predicates but should not have done so");
    }

    @Test
    public void shouldGenerateInvariantPreservationPredicates() {
        List<String> formulae = FormulaGenerator.invariantPreservations(pc);

        String P = "(" + propertyConcatenation + ")"; // shortened for readability

        // As it is expected to generate formulae for each invariant
        // individually and for the concatenation,
        // we shadow this.invariants with a version, that contains the
        // concatenation of invariants as well.
        List<String> invariants = new ArrayList<>();
        invariants.add(invariantConcatenation); // concatenation
        invariants.addAll(this.invariants); // each individually

        List<String> expected = new ArrayList<>();
        for (String inv : invariants) {
            for (String op : operations) {
                // Weakest preconditions of the operation
                Map<String, String> wps = weakestPreconditions.get(op);

                // For all B machines
                expected.add(P + " & " + inv + " & " + wps.get(inv));
                expected.add(P + " & " + inv + " & "
                             + "not(" + wps.get(inv) + ")");
                expected.add(P + " & (not(" + inv + ") => "
                             + wps.get(inv) + ")");
                expected.add(P + " & (not(" + inv + ") => "
                             + "not(" + wps.get(inv) + "))");

                // For EventB only
                String precond = FormulaGenerator.getStringConjunction(
                        preconditions.get(op));

                // Shorthand for concatenation of invariant, precond, and
                // before/after predicate
                String invAndBA = inv + " & " + precond
                                  + " & " + beforeAfterPredicates.get(op);

                expected.add(P + " & " + invAndBA + " & "
                             + primedInvariants.get(inv));
                expected.add(P + " & " + invAndBA + " & "
                             + "not(" + primedInvariants.get(inv) + ")");
                expected.add(P + " & (not(" + invAndBA + ") => "
                             + primedInvariants.get(inv) + ")");
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
    public void shouldPrependAssertionsWithPropertiesAndInvariants() {
        List<String> formulae = FormulaGenerator.assertions(pc);

        List<String> expected = new ArrayList<>();
        String P = propertyInvariantPre;
        expected.add(P + "assertion1");
        expected.add(P + "not(assertion1)");
        expected.add("not((" + propertyConcatenation + ") & "
                     + invariantConcatenation + ") => assertion1");
        expected.add(P + "assertion2");
        expected.add(P + "not(assertion2)");
        expected.add("not((" + propertyConcatenation + ") & "
                     + invariantConcatenation + ") => assertion2");
        expected.add(P + "(assertion1) & (assertion2)");
        expected.add(P + "not((assertion1) & (assertion2))");
        expected.add("not((" + propertyConcatenation + ") & "
                     + invariantConcatenation +
                     ") => (assertion1) & (assertion2)");

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

        List<String> formulae = FormulaGenerator.assertions(pc);

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

}
