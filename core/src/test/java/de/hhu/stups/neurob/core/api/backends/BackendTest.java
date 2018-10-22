package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IBEvalElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackendTest {

    private MachineAccess bMachine;

    @BeforeEach
    public void mockMachineAccess() {
        bMachine = mock(MachineAccess.class);
    }

    @Test
    public void shouldGenerateClassicalBInstanceForMachineTypeClassicalB()
            throws Exception {
        BPredicate pred = BPredicate.of("x>y & y>x");

        IBEvalElement evalElem = Backend.generateBFormula(pred,
                MachineType.CLASSICALB);

        assertEquals(ClassicalB.class, evalElem.getClass(),
                "Wrong object created!");
    }

    @Test
    public void shouldGenerateEventBInstanceForMachineTypeEventB()
            throws Exception {
        BPredicate pred = BPredicate.of("x>y & y>x");

        IBEvalElement evalElem = Backend.generateBFormula(pred,
                MachineType.EVENTB);

        assertEquals(EventB.class, evalElem.getClass(),
                "Wrong object created!");
    }

    @Test
    public void shouldMeasureNonNegativeTimeWhenPredicateIsDecidable()
            throws FormulaException {
        BPredicate predicate = BPredicate.of("predicate");

        Backend backend = mock(Backend.class);
        when(backend.isDecidable(predicate, bMachine))
                .thenReturn(true); // decidable
        when(backend.measureEvalTime(predicate, bMachine))
                .thenCallRealMethod();

        assertTrue(0 <= backend.measureEvalTime(predicate, bMachine),
                "Measured time for decidable predicates needs non-negative");
    }

    @Test
    public void shouldMeasureNegativeTimeWhenPredicateIsUndecidable()
            throws FormulaException {
        BPredicate predicate = BPredicate.of("predicate");

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate(predicate, bMachine))
                .thenReturn(false); // undecidable
        when(backend.measureEvalTime(predicate, bMachine))
                .thenCallRealMethod();
        when(backend.measureEvalTime(any(BPredicate.class), any(), any(), any()))
                .thenCallRealMethod();
        when(backend.getTimeOutValue()).thenReturn(20L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.SECONDS);

        Long actual = backend.measureEvalTime(predicate, bMachine);

        assertTrue(actual < 0,
                "Measured time for decidable predicates not negative");
    }

    @Test
    public void shouldReturnTrueWhenPredicateIsDecidable() throws FormulaException {
        BPredicate predicate = BPredicate.of("predicate");

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate(predicate, bMachine))
                .thenReturn(true); // decidable
        when(backend.isDecidable(predicate, bMachine))
                .thenCallRealMethod();
        when(backend.isDecidable(predicate, bMachine, 2L, TimeUnit.SECONDS))
                .thenCallRealMethod();
        when(backend.getTimeOutValue()).thenReturn(2L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.SECONDS);

        assertTrue(backend.isDecidable(predicate, bMachine),
                "Predicate was not detected as decidable.");
    }

    @Test
    public void shouldReturnFalseWhenPredicateIsUndecidable() throws FormulaException {
        Backend backend = mock(Backend.class);

        when(backend.decidePredicate("predicate", bMachine))
                .thenReturn(false); // undecidable
        when(backend.isDecidable("predicate", bMachine))
                .thenCallRealMethod();
        when(backend.getTimeOutValue()).thenReturn(2L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.SECONDS);

        assertFalse(backend.isDecidable("predicate", bMachine),
                "Predicate was not detected as undecidable.");
    }

    @Test
    public void shouldReturnFalseWhenTimoutAtEvaluation() throws FormulaException {
        Backend backend = mock(Backend.class);

        when(backend.decidePredicate("predicate", bMachine))
                .then(invocation -> {
                    Thread.sleep(100L); // pause for 100 ms
                    return true;
                });
        when(backend.isDecidable("predicate", bMachine))
                .thenCallRealMethod();
        when(backend.getTimeOutValue()).thenReturn(0L); // unsatisfiable timeout
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.MILLISECONDS);

        assertFalse(backend.isDecidable("predicate", bMachine),
                "Predicate was not detected as undecidable.");
    }

    @Test
    public void shouldReturnTimeoutWhenPredicateSolvingTakesTooLong() throws FormulaException {
        Backend backend = mock(Backend.class);

        BPredicate predicate = BPredicate.of("predicate");
        when(backend.solvePredicateUntimed(predicate, bMachine))
                .then(invocation -> {
                    Thread.sleep(100L); // pause for 100 ms
                    return Answer.VALID;
                });
        when(backend.solvePredicate(predicate, bMachine))
                .thenCallRealMethod();
        when(backend.getTimeOutValue()).thenReturn(0L); // unsatisfiable timeout
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.MILLISECONDS);

        Answer expected = Answer.TIMEOUT;
        Answer actual = backend.solvePredicate(predicate, bMachine).getAnswer();

        assertEquals(expected, actual, "Did not timeout.");

    }

    @Test
    public void shouldReturnTrueWhenCbcSolveCommandIsEvalResultTrue() throws Exception {
        CbcSolveCommand cmd = mock(CbcSolveCommand.class);
        when(cmd.getValue()).thenReturn(EvalResult.TRUE);

        BPredicate predicate = BPredicate.of("predicate");

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate(predicate, bMachine)).thenCallRealMethod();
        when(backend.solvePredicateUntimed(predicate, bMachine)).thenCallRealMethod();
        when(backend.createCbcSolveCommand(predicate, bMachine))
                .thenReturn(cmd);

        Boolean expected = true;
        Boolean actual = backend.decidePredicate(predicate, bMachine);

        assertEquals(true, actual,
                "Predicate was not detected as decidable");
    }

    @Test
    public void shouldReturnTrueWhenCbcSolveCommandIsEvalResultFalse() throws Exception {
        CbcSolveCommand cmd = mock(CbcSolveCommand.class);
        when(cmd.getValue()).thenReturn(EvalResult.FALSE);

        BPredicate predicate = BPredicate.of("predicate");

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate(predicate, bMachine)).thenCallRealMethod();
        when(backend.solvePredicateUntimed(predicate, bMachine)).thenCallRealMethod();
        when(backend.createCbcSolveCommand(predicate, bMachine))
                .thenReturn(cmd);

        Boolean expected = true;
        Boolean actual = backend.decidePredicate(predicate, bMachine);

        assertEquals(true, actual,
                "Predicate was not detected as decidable");
    }

    @Test
    public void shouldReturnFalseWhenCbcSolveCommandIsComputationNotCompleted()
            throws Exception {
        CbcSolveCommand cmd = mock(CbcSolveCommand.class);
        when(cmd.getValue()).thenReturn(mock(ComputationNotCompletedResult.class));

        BPredicate predicate = BPredicate.of("predicate");

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate(predicate, bMachine)).thenCallRealMethod();
        when(backend.solvePredicateUntimed(predicate, bMachine)).thenCallRealMethod();
        when(backend.createCbcSolveCommand(predicate, bMachine))
                .thenReturn(cmd);

        Boolean expected = false;
        Boolean actual = backend.decidePredicate(predicate, bMachine);

        assertEquals(expected, actual,
                "Predicate was not detected as undecidable");
    }

    @Test
    public void shouldNotUseConstructorTimeOutWhenParametersAreSupplied() throws FormulaException {
        Backend backend = mock(Backend.class);
        when(backend.getTimeOutValue()).thenReturn(20L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.SECONDS);

        // Decision runs for 10 seconds
        when(backend.decidePredicate("predicate", bMachine))
                .then(invocation -> {
                    Thread.sleep(10_000);
                    return true;
                });
        // Use real methods where needed
        when(backend.isDecidable("predicate", bMachine, 0L, TimeUnit.MILLISECONDS))
                .thenCallRealMethod();
        when(backend.measureEvalTime("predicate", bMachine, 0L, TimeUnit.MILLISECONDS))
                .thenCallRealMethod();

        Boolean isDecidable = backend.isDecidable("predicate", bMachine,
                0L, TimeUnit.MILLISECONDS);

        assertFalse(isDecidable,
                "Timeout did nothing");
    }
}
