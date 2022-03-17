package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.testharness.TestMachines;
import de.hhu.stups.neurob.testharness.TestResourceLoader;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.PredicateTrainingGenerator;
import de.hhu.stups.neurob.training.generation.util.FormulaGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


    @Test
    void shouldReadAllPredicatesWithAssociatedMachineSource() throws IOException {
        Path listFile = TestResourceLoader.getResourcePath("expected_predicates/formulae_generation.predlist.txt");
        PredicateList format = new PredicateList();

        BMachine mch = new BMachine("formulae_generation.mch");

        List<String> listFileRows = Files.readAllLines(listFile);
        List<String> predicates = listFileRows.subList(2, listFileRows.size()); // First two rows are metadata.

        List<TrainingSample<BPredicate, PredDbEntry>> expected =
                predicates.stream().map(BPredicate::new).map(b -> {
                    PredDbEntry label = new PredDbEntry(b, mch, new HashMap<>());
                    TrainingSample<BPredicate, PredDbEntry> sample = new TrainingSample<>(b, label);
                    return sample;
                }).collect(Collectors.toList());


        List<TrainingSample<BPredicate, PredDbEntry>> actual =
                format.loadSamples(listFile).collect(Collectors.toList());

        assertEquals(expected, actual);
    }
}
