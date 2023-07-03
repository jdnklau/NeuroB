package de.hhu.stups.neurob.training.generation.util;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.prob.animator.command.BeforeAfterPredicateCommand;
import de.prob.animator.command.NQPrimePredicateCommand;
import de.prob.animator.command.PrimePredicateCommand;
import de.prob.animator.command.WeakestPreconditionCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.model.classicalb.Assertion;
import de.prob.model.classicalb.Property;
import de.prob.model.eventb.Context;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredicateCollectionTest {

    private MachineAccess bMachine;
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
        when(model.parseFormula(any(), any())).thenAnswer(invocation -> null);
        when(ss.getModel()).thenReturn(model);

        // mock machine access
        bMachine = mock(MachineAccess.class);
        when(bMachine.getStateSpace()).thenReturn(ss);

    }

    /**
     * Creates a list of operations, named Operation-1 .. Operation-n.
     * <p>
     * For each integer {@code m} given as parameter, an operation with
     * {@code m} preconditions will be generated.
     *
     * @param amountPreconditions
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
    public void shouldNotCreateConcatenationWhenOnlyOneInvariant() {
        ModelElementList invMock = generatePredicates(Invariant.class, 1);
        when(ss.getMainComponent().getChildrenOfType(Invariant.class))
                .thenReturn(invMock);

        PredicateCollection pc = new PredicateCollection(bMachine);

        List<BPredicate> invariants = new ArrayList<>();
        invariants.add(BPredicate.of("Invariant-1"));

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

        PredicateCollection pc = new PredicateCollection(bMachine);

        List<BPredicate> expected = new ArrayList<>();
        expected.add(BPredicate.of("Invariant-2"));

        assertEquals(expected, pc.getInvariants(),
                "Should only load one invariant");
    }

    @Test
    public void shouldLoadPreconditions() {
        ModelElementList<BEvent> opMock = generateOperations(3, 2);
        when(ss.getMainComponent().getChildrenOfType(BEvent.class))
                .thenReturn(opMock);

        PredicateCollection pc = new PredicateCollection(bMachine);

        Map<String, List<BPredicate>> pres = new HashMap<>();

        // First operation, three preconditions
        List<String> pre = new ArrayList<>();
        pre.add("Guard-1");
        pre.add("Guard-2");
        pre.add("Guard-3");
        pres.put("Operation-1", pre.stream().map(BPredicate::new).collect(Collectors.toList()));
        // Second operation, two preconditions
        pre = new ArrayList<>();
        pre.add("Guard-1");
        pre.add("Guard-2");
        pres.put("Operation-2", pre.stream().map(BPredicate::new).collect(Collectors.toList()));

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

        PredicateCollection pc = new PredicateCollection(bMachine);

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

        PredicateCollection pc = new PredicateCollection(bMachine);

        List<BPredicate> expected = new ArrayList<>();
        expected.add(BPredicate.of("Property-1"));
        expected.add(BPredicate.of("Property-2"));

        assertEquals(expected, pc.getProperties(),
                "Properties do not match");
    }

    @Test
    public void shouldLoadAssertions() {
        ModelElementList<Assertion> assertions =
                generatePredicates(Assertion.class, 3);
        when(ss.getMainComponent().getChildrenOfType(Assertion.class))
                .thenReturn(assertions);

        PredicateCollection pc = new PredicateCollection(bMachine);

        List<String> asserts = new ArrayList<>();
        asserts.add("Assertion-1");
        asserts.add("Assertion-2");
        asserts.add("Assertion-3");

        List<BPredicate> expected = asserts.stream()
                .map(BPredicate::new)
                .collect(Collectors.toList());

        assertEquals(expected, pc.getAssertions(),
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
        }).when(bMachine).execute(any(WeakestPreconditionCommand.class));

        PredicateCollection pc = new PredicateCollection(bMachine);

        Map<String, Map<BPredicate, BPredicate>> weakestPres = new HashMap<>();
        // for each operation, the weakest pre for each invariant is expected
        Map<BPredicate, BPredicate> opWeak;
        // first operation
        opWeak = new HashMap<>();
        opWeak.put(BPredicate.of("Invariant-1"), BPredicate.of("weakest-precondition"));
        opWeak.put(BPredicate.of("Invariant-2"), BPredicate.of("weakest-precondition"));
        weakestPres.put("Operation-1", opWeak);
        // second operation
        opWeak = new HashMap<>();
        opWeak.put(BPredicate.of("Invariant-1"), BPredicate.of("weakest-precondition"));
        opWeak.put(BPredicate.of("Invariant-2"), BPredicate.of("weakest-precondition"));
        weakestPres.put("Operation-2", opWeak);

        assertEquals(weakestPres, pc.getWeakestPreConditions(),
                "Weakest Preconditions do not match");
    }

    @Test
    public void shouldLoadFullWeakestPreConditions() {
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
                .thenReturn(new CompoundPrologTerm("weakest-full-precondition"));
        doAnswer(invocation -> {
            WeakestPreconditionCommand cmd =
                    invocation.getArgument(0);
            cmd.processResult(bindings);
            return null;
        }).when(bMachine).execute(any(WeakestPreconditionCommand.class));
        IBEvalElement evalMock = mock(IBEvalElement.class);
        when(bMachine.parseFormula(any())).thenReturn(evalMock);

        PredicateCollection pc = new PredicateCollection(bMachine);

        Map<String, BPredicate> weakestPres = new HashMap<>();
        weakestPres.put("Operation-1", BPredicate.of("weakest-full-precondition"));
        weakestPres.put("Operation-2", BPredicate.of("weakest-full-precondition"));

        assertEquals(weakestPres, pc.getWeakestFullPreconditions(),
                "Weakest Full Preconditions do not match");
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

        PredicateCollection pc = new PredicateCollection(bMachine);

        List<String> expected = new ArrayList<>();
        expected.add("Invariant-1");
        expected.add("Invariant-2");
        expected.add("Invariant-3");
        expected.add("Assertion-1");
        expected.add("Assertion-2");

        List<String> actual = pc.getAssertions().stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        assertEquals(expected, actual,
                "Assertions not loaded correctly");
    }

    @Test
    public void shouldLoadBeforeAfterPredicatesWhenEventB() {
        when(bMachine.getMachineType()).thenReturn(MachineType.EVENTB);

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
        }).when(bMachine).execute(any(BeforeAfterPredicateCommand.class));

        PredicateCollection pc = new PredicateCollection(bMachine);

        Map<String, BPredicate> expected = new HashMap<>();
        expected.put("Operation-1", BPredicate.of("before-after"));
        expected.put("Operation-2", BPredicate.of("before-after"));

        Map<String, BPredicate> actual = pc.getBeforeAfterPredicates();

        assertEquals(expected, actual,
                "Expected weakest preconditions do not match");
    }

    @Test
    public void shouldLoadPrimedInvariantsWhenEventB() {
        when(bMachine.getMachineType()).thenReturn(MachineType.EVENTB);

        ModelElementList<Invariant> invariants =
                generatePredicates(Invariant.class, 2);
        when(ss.getMainComponent().getChildrenOfType(Invariant.class))
                .thenReturn(invariants);
        when(ss.getModel().getChildrenOfType(Context.class))
                .thenReturn(new ModelElementList<>());

        // Stub stateSpace.execute call
        ISimplifiedROMap bindings = mock(ISimplifiedROMap.class);
        // Weakest Preconditions
        when(bindings.get("PrimedPredOut"))
                .thenReturn(new CompoundPrologTerm("primed-invariant"));
        doAnswer(invocation -> {
            NQPrimePredicateCommand cmd =
                    invocation.getArgument(0);
            cmd.processResult(bindings);
            return null;
        }).when(bMachine).execute(any(NQPrimePredicateCommand.class));

        PredicateCollection pc = new PredicateCollection(bMachine);

        Map<BPredicate, BPredicate> expected = new HashMap<>();
        expected.put(BPredicate.of("Invariant-1"), BPredicate.of("primed-invariant"));
        expected.put(BPredicate.of("Invariant-2"), BPredicate.of("primed-invariant"));
//        expected.put(BPredicate.of("(Invariant-1) & (Invariant-2)"), BPredicate.of("primed-invariant"));

        Map<BPredicate, BPredicate> actual = pc.getPrimedInvariants();

        assertEquals(expected, actual,
                "Expected weakest preconditions do not match");

    }

    @Test
    public void shouldLoadAxiomsAsProperties() {
        when(bMachine.getMachineType()).thenReturn(MachineType.EVENTB);

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

        PredicateCollection pc = new PredicateCollection(bMachine);

        List<String> expected = new ArrayList<>();
        expected.add("Axiom-1");
        expected.add("Axiom-2");

        List<String> actual = pc.getProperties().stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        assertEquals(expected, actual,
                "Properties not loaded correctly");
    }
}
