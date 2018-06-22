package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Z3BackendTest {

    @Test
    public void shouldReturnProBEnum() {
        Backend z3 = new Z3Backend();

        assertEquals(CbcSolveCommand.Solvers.Z3, z3.toCbcEnum());
    }

}