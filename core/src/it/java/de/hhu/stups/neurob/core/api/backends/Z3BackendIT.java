package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.testharness.TestMachines;
import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class Z3BackendIT {

    private StateSpace ss;

    @BeforeEach
    public void loadStateSpace() throws IOException, ModelTranslationError {
        Api api = Main.getInjector().getInstance(Api.class);
        ss = api.b_load(TestMachines.FORMULAE_GEN_MCH);
    }

    @Test
    public void shouldBeDecidableWhenZ3CanBeCalledFromProB2()
            throws FormulaException {
        Z3Backend z3 = new Z3Backend();

        assertTrue(z3.isDecidable("TRUE = TRUE", ss),
                "Could not decide trivial predicate with Z3; "
                + "might indicate that  Z3 is not available "
                + "in executing system.");
    }

}