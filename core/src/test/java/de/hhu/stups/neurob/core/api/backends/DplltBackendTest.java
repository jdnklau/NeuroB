package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DplltBackendTest {

    @Test
    void shouldUseDplltBackendToSolveConstraint() {

    }

    @Test
    void shouldReturnDplltEnum() {
        Backend dpllt = new DplltBackend();

        assertEquals(CbcSolveCommand.Solvers.CDCLT, dpllt.toCbcEnum());
    }

}
