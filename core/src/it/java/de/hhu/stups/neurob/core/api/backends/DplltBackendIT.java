package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.testharness.TestMachines;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DplltBackendIT {

    private MachineAccess bMachine;

    @BeforeEach
    public void loadBMachine() throws MachineAccessException {
        bMachine = new MachineAccess(Paths.get(TestMachines.FORMULAE_GEN_MCH));
    }

    @Test
    public void shouldBeDecidableForDplltWithCLPFD()
            throws FormulaException {
        DplltBackend dpllt = new DplltBackend(BPreferences.set("CLPFD", "TRUE").assemble());

        final BPredicate pred = BPredicate.of("a:INTEGER & b:INTEGER & a>b & b>a");
        AnnotatedAnswer answer = dpllt.solvePredicateUntimed(pred, bMachine);

        assertEquals(Answer.INVALID, answer.getAnswer());

    }

    @Test
    public void shouldBeInvalid() throws FormulaException {
        BPredicate pred = BPredicate.of("1 < 0");

        DplltBackend dpllt = new DplltBackend();

        Answer expected = Answer.INVALID;
        Answer actual = dpllt.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual,
                "DPLLT could not decide trivial predicate");
    }

    @Test
    public void shouldBeValid() throws FormulaException {
        BPredicate pred = BPredicate.of("1 > 0");

        DplltBackend dpllt = new DplltBackend();

        Answer expected = Answer.VALID;
        Answer actual = dpllt.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual,
                "DPLLT could not decide trivial predicate");
    }

}
