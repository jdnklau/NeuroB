package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.testharness.TestMachines;
import de.hhu.stups.neurob.training.generation.util.FormulaGenerator;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FormulaGeneratorIT {

    @Test
    void shouldPrimePredicate() throws FormulaException, MachineAccessException {
        String pred = "x = y+1";

        MachineAccess mch = new MachineAccess(Paths.get(TestMachines.getMachinePath("cbc_solve_test.mch")));

        BPredicate expected = BPredicate.of("x′ = y′ + 1");
        BPredicate actual = FormulaGenerator.generatePrimedPredicate(mch, pred);

        assertEquals(expected, actual);
    }

    @Test
    void shouldPrimePredicateWhenEventB() throws FormulaException, MachineAccessException {
        String pred = "x = y+1";

        MachineAccess mch = new MachineAccess(Paths.get(TestMachines.getMachinePath("event-b/example/example.bcm")), MachineType.EVENTB);

        BPredicate expected = BPredicate.of("x' = y' + 1");
        BPredicate actual = FormulaGenerator.generatePrimedPredicate(mch, pred);

        assertEquals(expected, actual);
    }

}
