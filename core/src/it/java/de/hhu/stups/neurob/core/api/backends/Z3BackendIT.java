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

class Z3BackendIT {

    private MachineAccess bMachine;

    @BeforeEach
    public void loadBMachine() throws MachineAccessException {
        bMachine = new MachineAccess(Paths.get(TestMachines.FORMULAE_GEN_MCH));
    }

    @Test
    public void shouldBeDecidableWhenZ3CanBeCalledFromProB2()
            throws FormulaException {
        Z3Backend z3 = new Z3Backend();

        AnnotatedAnswer answer = z3.solvePredicateUntimed(BPredicate.of("TRUE = TRUE"), bMachine);

        assertEquals(Answer.VALID, answer.getAnswer(),
                "Could not decide trivial predicate with Z3; "
                + "might indicate that  Z3 is not available "
                + "in executing system.");

    }

}
