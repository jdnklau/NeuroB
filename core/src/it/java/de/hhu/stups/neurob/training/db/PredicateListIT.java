package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.testharness.TestMachines;
import de.hhu.stups.neurob.training.generation.PredicateTrainingGenerator;
import de.hhu.stups.neurob.training.generation.util.FormulaGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PredicateListIT {

    @Test
    void shouldReturnPredicatesAsExpected() throws IOException {
        Path mchPath = Paths.get(TestMachines.FORMULAE_GEN_MCH);
        Path targetDir = Files.createTempDirectory("neurob-it");

        PredicateList format = new PredicateList();
        PredicateTrainingGenerator generator = new PredicateTrainingGenerator(
                (p, ss) -> p,
                new PredDbEntry.Generator(1),
                format);
        generator.setGenerationRules(
                FormulaGenerator::assertions,
                FormulaGenerator::enablingAnalysis,
                FormulaGenerator::extendedPreconditionFormulae,
                FormulaGenerator::invariantConstrains,
                FormulaGenerator::invariantPreservationFormulae,
                FormulaGenerator::multiPreconditionFormulae,
                FormulaGenerator::weakestPreconditionFormulae
        );
        generator.setAstCleanup(true);
        generator.generateTrainingData(
                mchPath,
                targetDir,
                false);

        List<String> expected = TestMachines.loadExpectedPredicates("formulae_generation.predlist.txt");
        List<String> actual = Files.readAllLines(targetDir.resolve("formulae_generation.predlist.txt"));

        assertEquals(expected, actual);
    }
}
