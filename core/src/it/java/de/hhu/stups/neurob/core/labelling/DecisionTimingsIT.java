package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.KodKodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.testharness.TestMachines;
import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DecisionTimingsIT {

    private StateSpace ss;

    @BeforeEach
    public void loadStateSpace() throws IOException, ModelTranslationError {
        Api api = Main.getInjector().getInstance(Api.class);
        ss = api.b_load(TestMachines.FORMULAE_GEN_MCH);
    }

    @Test
    public void shouldBeNonDecidableWhenProB() throws LabelCreationException {
        String pred = "x>y & y>x"; // most basic example that fails for ProB

        ProBBackend prob = new ProBBackend();

        DecisionTimings timings = new DecisionTimings(pred, 1, ss,
                prob);

        assertTrue(timings.getTiming(prob) < 0,
                "ProB was unexpectedly able to decide the predicate");
    }

    @Test
    public void shouldBeDecidableWhenZ3() throws LabelCreationException {
        String pred = "x>y & y>x"; // most basic example that fails for ProB

        Z3Backend backend = new Z3Backend();

        DecisionTimings timings = new DecisionTimings(pred, 1, ss,
                backend);

        assertFalse(timings.getTiming(backend) < 0,
                "Z3 was unexpectedly not able to decide the predicate, "
                + "maybe no Z3 available?");
    }

    @Test
    public void shouldBeNondecidableForProBAndKodKodButDecidableForZ3() throws LabelCreationException {
        String pred = "x>y & y>x"; // most basic example that fails for ProB

        ProBBackend prob = new ProBBackend();
        Z3Backend z3 = new Z3Backend();
        KodKodBackend kodkod = new KodKodBackend();

        DecisionTimings timings = new DecisionTimings(pred, 1, ss,
                prob, z3, kodkod);

        assertAll("Generating for multiple backends",
                () -> assertTrue(timings.getTiming(prob) < 0,
                        "ProB is expected to fail"),
                () -> assertFalse(timings.getTiming(z3) < 0,
                        "Z3 is expected to succeed"),
                () -> assertTrue(timings.getTiming(kodkod) < 0,
                        "Kodkod is expected to fail")
        );
    }
}