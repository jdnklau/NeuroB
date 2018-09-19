package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.testharness.TestMachines;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class DecisionTimingsIT {

    private MachineAccess bMachine;

    @BeforeEach
    public void loadBMachine() throws MachineAccessException {
        bMachine = new MachineAccess(Paths.get(TestMachines.FORMULAE_GEN_MCH));
    }

    @Test
    public void shouldBeNonDecidableWhenProB() throws LabelCreationException {
        String pred = "x>y & y>x"; // most basic example that fails for ProB

        ProBBackend prob = new ProBBackend();

        DecisionTimings timings = new DecisionTimings(pred, 1, bMachine,
                prob);

        assertTrue(timings.getTiming(prob) < 0,
                "ProB was unexpectedly able to decide the predicate");
    }

    @Test
    public void shouldBeDecidableWhenZ3() throws LabelCreationException {
        String pred = "x>y & y>x"; // most basic example that fails for ProB

        Z3Backend backend = new Z3Backend();

        DecisionTimings timings = new DecisionTimings(pred, 1, bMachine,
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
        KodkodBackend kodkod = new KodkodBackend();

        DecisionTimings timings = new DecisionTimings(pred, 1, bMachine,
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
