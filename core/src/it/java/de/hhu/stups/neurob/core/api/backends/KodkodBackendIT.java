package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.testharness.TestMachines;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class KodkodBackendIT {

    @Test
    public void shouldReturnProBEnum() {
        Backend kodkod = new KodkodBackend();

        assertEquals(CbcSolveCommand.Solvers.KODKOD, kodkod.toCbcEnum());
    }

    @Test
    public void shouldBeValidIfCbcSolveDoesIgnoreStateSpace()
            throws FormulaException, MachineAccessException {
        KodkodBackend kodkod = new KodkodBackend();
        MachineAccess bMachine = new MachineAccess(Paths.get(TestMachines.getMachinePath("cbc_solve_test.mch")));

        AnnotatedAnswer answer = kodkod.solvePredicateUntimed(BPredicate.of("x : 8..20"), bMachine);

        assertEquals(Answer.VALID, answer.getAnswer(),
                "Predicate is invalid but should be valid. Kodkod makes use of state.");
    }

}
