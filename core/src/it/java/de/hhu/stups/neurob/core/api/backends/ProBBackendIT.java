package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.testharness.TestMachines;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ProBBackendIT {

    private MachineAccess bMachine;

    @BeforeEach
    public void loadBMachine() throws MachineAccessException {
        bMachine = new MachineAccess(Paths.get(TestMachines.FORMULAE_GEN_MCH));
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

        System.out.println(actual.getMessage());
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
    public void shouldBeUnknown() throws FormulaException {
        BPredicate pred = BPredicate.of("x>y & y>x");

        ProBBackend prob = new ProBBackend();

        Answer expected = Answer.UNKNOWN;
        Answer actual = prob.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual,
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeUndecidable() throws FormulaException {
        String pred = "x>y & y>x";
        ProBBackend prob = new ProBBackend();

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
        ProBBackend prob = new ProBBackend();

        Long time = prob.measureEvalTime(pred, bMachine);

        assertTrue(time < 0,
                "ProB was unexpectedly able to decide " + pred);
    }

}
