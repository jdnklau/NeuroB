package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProBBackendTest {

    @Test
    public void shouldReturnProBEnum() {
        Backend prob = new ProBBackend();

        assertEquals(CbcSolveCommand.Solvers.PROB, prob.toCbcEnum());
    }

}