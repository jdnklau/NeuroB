package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CdcltBackendTest {
    @Test
    void shouldReturnCdcltEnum() {
        Backend cdclt = new CdcltBackend();

        assertEquals(CbcSolveCommand.Solvers.CDCLT, cdclt.toCbcEnum());
    }

}
