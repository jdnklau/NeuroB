package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KodkodBackendTest {

    @Test
    public void shouldReturnProBEnum() {
        Backend kodkod = new KodkodBackend();

        assertEquals(CbcSolveCommand.Solvers.KODKOD, kodkod.toCbcEnum());
    }

}
