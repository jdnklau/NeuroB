package de.hhu.stups.neurob.training.generation.util;

import de.prob.animator.command.BeforeAfterPredicateCommand;
import de.prob.animator.command.PrimePredicateCommand;
import de.prob.animator.command.WeakestPreconditionCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.model.classicalb.Assertion;
import de.prob.model.classicalb.Property;
import de.prob.model.eventb.Context;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.Axiom;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Guard;
import de.prob.model.representation.Invariant;
import de.prob.model.representation.ModelElementList;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredicateCollectionTest {

    private StateSpace ss;

    @BeforeEach
    public void mockStateSpace() {
        // Mock EventB StateSpace
        ss = mock(StateSpace.class);

        // Mock main component
        AbstractElement comp = mock(AbstractElement.class);
        when(ss.getMainComponent()).thenReturn(comp);

        when(ss.getMainComponent().getChildrenOfType(any()))
                .thenReturn(new ModelElementList<>());

        // For invariant command creation
        AbstractModel model = mock(AbstractModel.class);
        when(model.parseFormula(any())).thenAnswer(invocation -> null);
        when(ss.getModel()).thenReturn(model);

    }

    /**
     * Creates a list of operations, named Operation-1 .. Operation-n.
     * <p>
     * For each integer {@code m} given as parameter, an operation with
     * {@code m} preconditions will be generated.
     *
     * @param amountPreconditions
     *
     * @return
     */
    private ModelElementList<BEvent>
    generateOperations(int... amountPreconditions) {
        ModelElementList<BEvent> operations = new ModelElementList<>();
        for (int i = 0; i < amountPreconditions.length; i++) {
            // Set up operation
            BEvent operation = mock(BEvent.class);
            String opname = "Operation-" + (i + 1);
            when(operation.getName()).thenReturn(opname);

            // Set up preconditions
            ModelElementList<Guard> preconditions =
                    generatePredicates(Guard.class, amountPreconditions[i]);
            when(operation.getChildrenOfType(Guard.class)).thenReturn(
                    preconditions);

            operations = operations.addElement(operation);
        }
        return operations;
    }

    /**
     * Returns a list of {@code amount} mocked entries of specified type.
     * <p>
     * Calling {@code entry.getFormula().getCode()} returns a string of the form
     * {@code type-number}, with {@code 0 <= number < amount}.
     *
     * @param type
     * @param amount
     * @param <T>
     *
     * @return
     */
    private <T extends AbstractFormulaElement>
    ModelElementList<T>
    generatePredicates(Class<T> type, int amount) {
        ModelElementList<T> elements = new ModelElementList<>();
        for (int i = 0; i < amount; i++) {
            // Code of formula is type-i
            String code = type.getSimpleName() + "-" + (i + 1);
//            IBEvalElement formula = mock(IBEvalElement.class);
//            when(formula.getCode()).thenReturn(code);

            T elem = mock(type);
            when(elem.getFormula()).thenReturn(new ClassicalB(code));

            elements = elements.addElement(elem);
        }

        return elements;
    }

    @Test
    public void shouldLoadInvariantWithConcatenationWhenMoreThanOne() {
        ModelElementList<Invariant> invMock =
                generatePredicates(Invariant.class, 2);
        when(ss.getMainComponent().getChildrenOfType(Invariant.class))
                .thenReturn(invMock);

        PredicateCollection pc = new PredicateCollection(ss);

        List<String> expected = new ArrayList<>();
        expected.add("Invariant-1");
        expected.add("Invariant-2");
        expected.add("(Invariant-1) & (Invariant-2)");

        assertEquals(expected, pc.getInvariants());

    }

    @Test
    public void shouldNotCreateConcatenationWhenOnlyOneInvariant() {
        ModelElementList invMock = generatePredicates(Invariant.class, 1);
        when(ss.getMainComponent().getChildrenOfType(Invariant.class))
                .thenReturn(invMock);

        PredicateCollection pc = new PredicateCollection(ss);

        List<String> invariants = new ArrayList<>();
        invariants.add("Invariant-1");

        assertEquals(invariants, pc.getInvariants(),
                "Should only load one invariant");
    }

    @Test
    public void shouldNotConcatenateInvariantsWhenOnlyOneIsNoTheorem() {
        ModelElementList<Invariant> invMock =
                generatePredicates(Invariant.class, 3);

        // Invariant 1 and 3 are theorems
        when(invMock.get(0).isTheorem()).thenReturn(true);
        when(invMock.get(2).isTheorem()).thenReturn(true);
        // One invariant that is no theorem
        when(invMock.get(1).isTheorem()).thenReturn(false);

        when(ss.getMainComponent().getChildrenOfType(Invariant.class))
                .thenReturn(invMock);

        PredicateCollection pc = new PredicateCollection(ss);

        List<String> invariants = new ArrayList<>();
        invariants.add("Invariant-2");

        assertEquals(invariants, pc.getInvariants(),
                "Should only load one invariant");
    }

    @Test
    public void shouldLoadPreconditions() {
        ModelElementList<BEvent> opMock = generateOperations(3, 2);
        when(ss.getMainComponent().getChildrenOfType(BEvent.class))
                .thenReturn(opMock);

        PredicateCollection pc = new PredicateCollection(ss);

        Map<String, List<String>> pres = new HashMap<>();

        // First operation, three preconditions
        List<String> pre = new ArrayList<>();
        pre.add("Guard-1");
        pre.add("Guard-2");
        pre.add("Guard-3");
        pres.put("Operation-1", pre);
        // Second operation, two preconditions
        pre = new ArrayList<>();
        pre.add("Guard-1");
        pre.add("Guard-2");
        pres.put("Operation-2", pre);


        assertAll("Included preconditions",
                () -> assertEquals(pres.size(), pc.getPreconditions().size(),
                        "Number of preconditions does not match"),
                () -> assertEquals(pres, pc.getPreconditions(),
                        "Collected preconditions do not match")
        );
    }

    @Test
    public void shouldLoadOperationNamesWithoutInitialisationIncluded() {
        ModelElementList<BEvent> operations = new ModelElementList<>();

        BEvent initMock = mock(BEvent.class);
        when(initMock.getName()).thenReturn("INITIALISATION");
        when(initMock.getChildrenOfType(any())).thenReturn(new ModelElementList<>());
        BEvent op1Mock = mock(BEvent.class);
        when(op1Mock.getName()).thenReturn("Operation-1");
        when(op1Mock.getChildrenOfType(any())).thenReturn(new ModelElementList<>());
        BEvent op2Mock = mock(BEvent.class);
        when(op2Mock.getName()).thenReturn("Operation-2");
        when(op2Mock.getChildrenOfType(any())).thenReturn(new ModelElementList<>());
        BEvent op3Mock = mock(BEvent.class);
        when(op3Mock.getName()).thenReturn("Operation-3");
        when(op3Mock.getChildrenOfType(any())).thenReturn(new ModelElementList<>());

        operations = operations
                .addElement(initMock)
                .addElement(op1Mock)
                .addElement(op2Mock)
                .addElement(op3Mock);

        when(ss.getMainComponent().getChildrenOfType(BEvent.class))
                .thenReturn(operations);

        PredicateCollection pc = new PredicateCollection(ss);

        List<String> expected = new ArrayList<>();
        expected.add("Operation-1");
        expected.add("Operation-2");
        expected.add("Operation-3");

        assertEquals(expected, pc.getOperationNames(),
                "Operations do not match");
    }

    @Test
    public void shouldLoadProperties() {
        ModelElementList<Property> properties =
                generatePredicates(Property.class, 2);
        when(ss.getMainComponent().getChildrenOfType(Property.class))
                .thenReturn(properties);

        PredicateCollection pc = new PredicateCollection(ss);

        List<String> expected = new ArrayList<>();
        expected.add("Property-1");
        expected.add("Property-2");

        assertEquals(expected, pc.getProperties(),
                "Properties do not match");
    }

    @Test
    public void shouldLoadAssertions() {
        ModelElementList<Assertion> assertions =
                generatePredicates(Assertion.class, 3);
        when(ss.getMainComponent().getChildrenOfType(Assertion.class))
                .thenReturn(assertions);

        PredicateCollection pc = new PredicateCollection(ss);

        List<String> asserts = new ArrayList<>();
        asserts.add("Assertion-1");
        asserts.add("Assertion-2");
        asserts.add("Assertion-3");

        assertEquals(asserts, pc.getAssertions(),
                "Assertions do not match");
    }

    @Test
    public void shouldLoadWeakestPreConditions() {
        ModelElementList<Invariant> invariants =
                generatePredicates(Invariant.class, 2);
        when(ss.getMainComponent().getChildrenOfType(Invariant.class))
                .thenReturn(invariants);
        ModelElementList<BEvent> operations =
                generateOperations(2, 1);
        when(ss.getMainComponent().getChildrenOfType(BEvent.class))
                .thenReturn(operations);

        // Stub stateSpace.execute call
        ISimplifiedROMap bindings = mock(ISimplifiedROMap.class);
        // Weakest Preconditions
        when(bindings.get("WeakestPrecondition"))
                .thenReturn(new CompoundPrologTerm("weakest-precondition"));
        doAnswer(invocation -> {
            WeakestPreconditionCommand cmd =
                    invocation.getArgument(0);
            cmd.processResult(bindings);
            return null;
        }).when(ss).execute(any(WeakestPreconditionCommand.class));

        PredicateCollection pc = new PredicateCollection(ss);

        Map<String, Map<String, String>> weakestPres = new HashMap<>();
        // for each operation, the weakest pre for each invariant is expected
        Map<String, String> opWeak;
        // first operation
        opWeak = new HashMap<>();
        opWeak.put("Invariant-1", "weakest-precondition");
        opWeak.put("Invariant-2", "weakest-precondition");
        opWeak.put("(Invariant-1) & (Invariant-2)", "weakest-precondition");
        weakestPres.put("Operation-1", opWeak);
        // second operation
        opWeak = new HashMap<>();
        opWeak.put("Invariant-1", "weakest-precondition");
        opWeak.put("Invariant-2", "weakest-precondition");
        opWeak.put("(Invariant-1) & (Invariant-2)", "weakest-precondition");
        weakestPres.put("Operation-2", opWeak);

        assertEquals(weakestPres, pc.getWeakestPreConditions(),
                "Weakest Preconditions do not match");
    }

    @Test
    public void shouldLoadTheoremsAsAssertions() {
        ModelElementList<Invariant> theorems =
                generatePredicates(Invariant.class, 3);
        // Mark all invariants as theorems
        for (Invariant theorem : theorems) {
            when(theorem.isTheorem()).thenReturn(true);
        }

        ModelElementList<Assertion> assertions =
                generatePredicates(Assertion.class, 2);

        when(ss.getMainComponent().getChildrenOfType(Invariant.class))
                .thenReturn(theorems);
        when(ss.getMainComponent().getChildrenOfType(Assertion.class))
                .thenReturn(assertions);

        PredicateCollection pc = new PredicateCollection(ss);

        List<String> expected = new ArrayList<>();
        expected.add("Invariant-1");
        expected.add("Invariant-2");
        expected.add("Invariant-3");
        expected.add("Assertion-1");
        expected.add("Assertion-2");

        List<String> actual = pc.getAssertions();

        assertEquals(expected, actual,
                "Assertions not loaded correctly");
    }

    @Test
    public void shouldLoadBeforeAfterPredicatesWhenEventB() {
        EventBModel eventBMock = mock(EventBModel.class);
        when(ss.getModel()).thenReturn(eventBMock);

        ModelElementList<BEvent> operations = generateOperations(0, 0);
        when(ss.getMainComponent().getChildrenOfType(BEvent.class))
                .thenReturn(operations);
        when(ss.getModel().getChildrenOfType(Context.class))
                .thenReturn(new ModelElementList<>());

        // Stub stateSpace.execute call
        ISimplifiedROMap bindings = mock(ISimplifiedROMap.class);
        // Weakest Preconditions
        when(bindings.get("BAPredicate"))
                .thenReturn(new CompoundPrologTerm("before-after"));
        doAnswer(invocation -> {
            BeforeAfterPredicateCommand cmd =
                    invocation.getArgument(0);
            cmd.processResult(bindings);
            return null;
        }).when(ss).execute(any(BeforeAfterPredicateCommand.class));

        PredicateCollection pc = new PredicateCollection(ss);

        Map<String, String> expected = new HashMap<>();
        expected.put("Operation-1", "before-after");
        expected.put("Operation-2", "before-after");

        Map<String, String> actual = pc.getBeforeAfterPredicates();

        assertEquals(expected, actual,
                "Expected weakest preconditions do not match");
    }

    @Test
    public void shouldLoadPrimedInvariantsWhenEventB() {
        EventBModel eventBMock = mock(EventBModel.class);
        when(ss.getModel()).thenReturn(eventBMock);

        ModelElementList<Invariant> invariants =
                generatePredicates(Invariant.class, 2);
        when(ss.getMainComponent().getChildrenOfType(Invariant.class))
                .thenReturn(invariants);
        when(ss.getModel().getChildrenOfType(Context.class))
                .thenReturn(new ModelElementList<>());

        // Stub stateSpace.execute call
        ISimplifiedROMap bindings = mock(ISimplifiedROMap.class);
        // Weakest Preconditions
        when(bindings.get("PrimedPredicate"))
                .thenReturn(new CompoundPrologTerm("primed-invariant"));
        doAnswer(invocation -> {
            PrimePredicateCommand cmd =
                    invocation.getArgument(0);
            cmd.processResult(bindings);
            return null;
        }).when(ss).execute(any(PrimePredicateCommand.class));

        PredicateCollection pc = new PredicateCollection(ss);

        Map<String, String> expected = new HashMap<>();
        expected.put("Invariant-1", "primed-invariant");
        expected.put("Invariant-2", "primed-invariant");
        expected.put("(Invariant-1) & (Invariant-2)", "primed-invariant");

        Map<String, String> actual = pc.getPrimedInvariants();

        assertEquals(expected, actual,
                "Expected weakest preconditions do not match");

    }

    @Test
    public void shouldLoadAxiomsAsProperties() {
        EventBModel eventBMock = mock(EventBModel.class);
        when(ss.getModel()).thenReturn(eventBMock);

        // Set up context to return axioms
        Context contextMock = mock(Context.class);
        ModelElementList<Context> contexts =
                new ModelElementList<Context>().addElement(contextMock);
        when(ss.getModel().getChildrenOfType(Context.class))
                .thenReturn(contexts);
        ModelElementList<Axiom> axioms =
                generatePredicates(Axiom.class, 2);
        when(contextMock.getChildrenOfType(Axiom.class))
                .thenReturn(axioms);

        PredicateCollection pc = new PredicateCollection(ss);

        List<String> expected = new ArrayList<>();
        expected.add("Axiom-1");
        expected.add("Axiom-2");

        List<String> actual = pc.getProperties();

        assertEquals(expected, actual,
                "Properties not loaded correctly");
    }
}
