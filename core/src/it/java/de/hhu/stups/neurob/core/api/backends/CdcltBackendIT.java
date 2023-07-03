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

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdcltBackendIT {

    private MachineAccess bMachine;

    @BeforeEach
    public void loadBMachine() throws MachineAccessException {
        bMachine = new MachineAccess(Paths.get(TestMachines.FORMULAE_GEN_MCH));
    }

    @Test
    public void shouldBeDecidableForCdcltWithCLPFD()
            throws FormulaException {
        CdcltBackend cdclt = new CdcltBackend(BPreferences.set("CLPFD", "TRUE").assemble());

        final BPredicate pred = BPredicate.of("a:INTEGER & b:INTEGER & a>b & b>a");
        AnnotatedAnswer answer = cdclt.solvePredicateUntimed(pred, bMachine);

        assertEquals(Answer.INVALID, answer.getAnswer());

    }

    @Test
    public void shouldBeInvalid() throws FormulaException {
        BPredicate pred = BPredicate.of("1 < 0");

        CdcltBackend cdclt = new CdcltBackend();

        Answer expected = Answer.INVALID;
        Answer actual = cdclt.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual,
                "CDCLT could not decide trivial predicate");
    }

    @Test
    public void shouldBeValid() throws FormulaException {
        BPredicate pred = BPredicate.of("1 > 0");

        CdcltBackend cdclt = new CdcltBackend();

        Answer expected = Answer.VALID;
        Answer actual = cdclt.solvePredicate(pred, bMachine).getAnswer();

        assertEquals(expected, actual,
                "CDCLT could not decide trivial predicate");
    }

}
