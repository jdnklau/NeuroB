package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.testharness.TestMachines;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ProBBackendIT {

    private MachineAccess bMachine;
    private BPreference noReals = new BPreference("ALLOW_REALS", "FALSE");
    // Need noSmt and noClpfd for ensuring vanilla ProB settings
    private BPreference noSmt = new BPreference("SMT", "FALSE");
    private BPreference noClpfd = new BPreference("CLPFD", "FALSE");

    @BeforeEach
    public void loadBMachine() throws MachineAccessException {
        bMachine = new MachineAccess(Paths.get(TestMachines.FORMULAE_GEN_MCH));
    }

    @AfterEach
    public void closeBMachine() {
        bMachine.close();
    }

    @Test
    public void shouldBeDecidable() throws FormulaException {
        String pred = "1 > 0";
        ProBBackend prob = new ProBBackend();

        Boolean isDecidable = prob.isDecidable(pred, bMachine);

        assertTrue(isDecidable,
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeValid() throws FormulaException {
        BPredicate pred = BPredicate.of("1 > 0");

        ProBBackend prob = new ProBBackend();

        Answer expected = Answer.VALID;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual,
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeValidWhenSolvableConstraintProblem() throws FormulaException {
        BPredicate pred = BPredicate.of("x : {1,2,3}");

        ProBBackend prob = new ProBBackend();

        Answer expected = Answer.VALID;
        TimedAnswer actual = prob.solvePredicate(pred, bMachine);

        assertEquals(expected, actual.getAnswer(),
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeInvalid() throws FormulaException {
        BPredicate pred = BPredicate.of("1 < 0");

        ProBBackend prob = new ProBBackend();

        Answer expected = Answer.INVALID;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual,
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeTimeout() throws FormulaException {
        BPredicate pred = BPredicate.of("x>y & y>x");

        ProBBackend prob = new ProBBackend(noReals, noSmt, noClpfd);

        Answer expected = Answer.TIMEOUT;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual,
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeUndecidable() throws FormulaException {
        String pred = "x>y & y>x";
        ProBBackend prob = new ProBBackend(noReals, noSmt, noClpfd);

        Boolean isDecidable = prob.isDecidable(pred, bMachine);

        assertFalse(isDecidable,
                "ProB was unexpectedly able to decide " + pred);
    }

    @Test
    public void shouldBeNonNegativeTime() throws FormulaException {
        String pred = "1 > 0";
        ProBBackend prob = new ProBBackend();

        Long time = prob.measureEvalTime(pred, bMachine);

        assertFalse(time < 0,
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeNegativeTime() throws FormulaException {
        String pred = "x>y & y>x";
        ProBBackend prob = new ProBBackend(noReals, noSmt, noClpfd);

        Long time = prob.measureEvalTime(pred, bMachine);

        assertTrue(time < 0,
                "ProB was unexpectedly able to decide " + pred + "; measured time: " + time);
    }

    @Test
    public void shouldAnswerTimeoutWhenTimeoutIsZero() throws FormulaException {
        BPredicate pred = BPredicate.of("x > 0");
        ProBBackend prob = new ProBBackend(0L, TimeUnit.NANOSECONDS);

        Answer expected = Answer.TIMEOUT;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldOverrideDefaultTimeOut() throws FormulaException {
        BPredicate pred = BPredicate.of("x > 0");
        ProBBackend prob = spy(new ProBBackend(3500000000L, TimeUnit.NANOSECONDS));

        when(prob.solvePredicateUntimed(pred, bMachine))
                .thenAnswer(invocation -> {
                    Thread.sleep(3000); // three seconds
                    return new AnnotatedAnswer(Answer.VALID, "x = 1");
                });

        Answer expected = Answer.VALID;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldOverrideDefaultTimeOutWithPreferenceTimeout() throws FormulaException {
        BPredicate pred = BPredicate.of("x > 0");
        BPreference timeout = new BPreference("TIME_OUT", "3500");
        ProBBackend prob = spy(new ProBBackend(Backend.defaultTimeOut, Backend.defaultTimeUnit, timeout));

        when(prob.solvePredicateUntimed(pred, bMachine))
                .thenAnswer(invocation -> {
                    Thread.sleep(3000); // three seconds
                    return new AnnotatedAnswer(Answer.VALID, "x = 1");
                });

        Answer expected = Answer.VALID;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldBeInvalidWhenMaxIntIsLessThanFive() throws FormulaException {
        BPredicate pred = BPredicate.of("5 : INT");
        BPreference maxInt = BPreference.set("MAXINT", "4");
        ProBBackend prob = new ProBBackend(Backend.defaultTimeOut, Backend.defaultTimeUnit);

        Answer expected = Answer.INVALID;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldBeInvalidWhenMaxIntIsGreaterOrEqualThanFive() throws FormulaException {
        BPredicate pred = BPredicate.of("5 : INT");
        BPreference maxInt = BPreference.set("MAXINT", "5");

        ProBBackend prob = new ProBBackend(Backend.defaultTimeOut, Backend.defaultTimeUnit, maxInt);

        Answer expected = Answer.VALID;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual);
    }
}
