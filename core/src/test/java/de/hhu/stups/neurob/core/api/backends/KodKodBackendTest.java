package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KodKodBackendTest {

    @Test
    public void shouldReturnProBEnum() {
        Backend kodkod = new KodKodBackend();

        assertEquals(CbcSolveCommand.Solvers.KODKOD, kodkod.toCbcEnum());
    }

}