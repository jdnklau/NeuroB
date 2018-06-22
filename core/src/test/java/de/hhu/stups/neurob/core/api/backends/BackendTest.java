package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackendTest {

    private StateSpace stateSpace;

    @BeforeEach
    public void mockStateSpace() {
        stateSpace = mock(StateSpace.class);
    }

    @Test
    public void shouldMeasureNonNegativeTimeWhenPredicateIsDecidable()
            throws FormulaException {
        Backend backend = mock(Backend.class);

        when(backend.isDecidable("predicate", stateSpace))
                .thenReturn(true); // decidable
        when(backend.measureEvalTime("predicate", stateSpace))
                .thenCallRealMethod();

        assertTrue(0 <= backend.measureEvalTime("predicate", stateSpace),
                "Measured time for decidable predicates needs non-negative");
    }

    @Test
    public void shouldMeasureNegativeTimeWhenPredicateIsUndecidable()
            throws FormulaException {
        Backend backend = mock(Backend.class);

        when(backend.isDecidable("predicate", stateSpace))
                .thenReturn(false); // undecidable
        when(backend.measureEvalTime("predicate", stateSpace))
                .thenCallRealMethod();

        Long expected = -1L;
        Long actual = backend.measureEvalTime("predicate", stateSpace);

        assertEquals(expected, actual,
                "Measured time for decidable predicates needs non-negative");
    }

    @Test
    public void shouldReturnTrueWhenPredicateIsDecidable() throws FormulaException {
        Backend backend = mock(Backend.class);

        when(backend.decidePredicate("predicate", stateSpace))
                .thenReturn(true); // decidable
        when(backend.isDecidable("predicate", stateSpace))
                .thenCallRealMethod();
        when(backend.getTimeOutValue()).thenReturn(2L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.SECONDS);

        assertTrue(backend.isDecidable("predicate", stateSpace),
                "Predicate was not detected as decidable.");
    }

    @Test
    public void shouldReturnFalseWhenPredicateIsUndecidable() throws FormulaException {
        Backend backend = mock(Backend.class);

        when(backend.decidePredicate("predicate", stateSpace))
                .thenReturn(false); // undecidable
        when(backend.isDecidable("predicate", stateSpace))
                .thenCallRealMethod();
        when(backend.getTimeOutValue()).thenReturn(2L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.SECONDS);

        assertFalse(backend.isDecidable("predicate", stateSpace),
                "Predicate was not detected as undecidable.");
    }

    @Test
    public void shouldReturnFalseWhenTimoutAtEvaluation() throws FormulaException {
        Backend backend = mock(Backend.class);

        when(backend.decidePredicate("predicate", stateSpace))
                .then(invocation -> {
                    Thread.sleep(100L); // pause for 100 ms
                    return true;
                });
        when(backend.isDecidable("predicate", stateSpace))
                .thenCallRealMethod();
        when(backend.getTimeOutValue()).thenReturn(0L); // unsatisfiable timeout
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.MILLISECONDS);

        assertFalse(backend.isDecidable("predicate", stateSpace),
                "Predicate was not detected as undecidable.");
    }
}