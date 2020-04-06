package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmtBackendTest {

    @Test
    public void shouldReturnProBEnum() {
        Backend smt = new SmtBackend();

        assertEquals(CbcSolveCommand.Solvers.SMT_SUPPORTED_INTERPRETER,
                smt.toCbcEnum());
    }

}