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

class ProBBackendIT {

    private StateSpace stateSpace;

    @BeforeEach
    public void loadStateSpace() throws IOException, ModelTranslationError {
        Api api = Main.getInjector().getInstance(Api.class);

        stateSpace = api.b_load(TestMachines.FORMULAE_GEN_MCH);
    }

    @Test
    public void shouldBeDecidable() throws FormulaException {
        String pred = "1 > 0";
        ProBBackend prob = new ProBBackend();

        Boolean isDecidable = prob.isDecidable(pred, stateSpace);

        assertTrue(isDecidable,
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeUndecidable() throws FormulaException {
        String pred = "x>y & y>x";
        ProBBackend prob = new ProBBackend();

        Boolean isDecidable = prob.isDecidable(pred, stateSpace);

        assertFalse(isDecidable,
                "ProB was unexpectedly able to decide " + pred);
    }

    @Test
    public void shouldBeNonNegativeTime() throws FormulaException {
        String pred = "1 > 0";
        ProBBackend prob = new ProBBackend();

        Long time = prob.measureEvalTime(pred, stateSpace);

        assertFalse(time < 0,
                "ProB could not decide trivial predicate");
    }

    @Test
    public void shouldBeNegativeTime() throws FormulaException {
        String pred = "x>y & y>x";
        ProBBackend prob = new ProBBackend();

        Long time = prob.measureEvalTime(pred, stateSpace);

        assertTrue(time < 0,
                "ProB was unexpectedly able to decide " + pred);
    }

}