package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.features.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.features.TheoryFeatures;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.testharness.TestMachines;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.CsvFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class PredicateTrainingGeneratorIT {

    private
    PredicateTrainingGenerator<PredicateFeatures, PredicateLabelling> generator;

    private TrainingDataFormat formatMock;

    @BeforeEach
    public void setUpGenerator() {
        PredicateFeatureGenerating<PredicateFeatures> featureGen =
                (pred, ss) -> generateMockedFeatures(pred);
        PredicateLabelGenerating<PredicateLabelling> labelGen =
                (pred, ss) -> generateMockedLabels(pred);
        formatMock = mock(TrainingDataFormat.class);

        generator = new PredicateTrainingGenerator<>(
                featureGen, labelGen, formatMock);
    }

    private PredicateFeatures generateMockedFeatures(String pred) {
        PredicateFeatures f = mock(PredicateFeatures.class);
        when(f.getFeatureArray()).thenReturn(new Double[]{1., 2., 3.});
        when(f.getPredicate()).thenReturn(pred);
        return f;
    }

    private PredicateLabelling generateMockedLabels(String pred) {
        PredicateLabelling f = mock(PredicateLabelling.class);
        when(f.getLabellingArray()).thenReturn(new Double[]{1., 2., 3.});
        when(f.getPredicate()).thenReturn(pred);
        return f;
    }

    @Test
    public void shouldStreamSamplesWithSourcePredicateWhenStreamingFromFile()
            throws IOException {
        List<String> expected = TestMachines.loadExpectedPredicates(
                TestMachines.FORMULAE_GEN_MCH_PREDICATE_FILE);

        List<String> actual = generator.streamSamplesFromFile(
                Paths.get(TestMachines.FORMULAE_GEN_MCH))
                .map(sample -> sample.getFeatures().getPredicate())
                .collect(Collectors.toList());

        expected.sort(Comparator.naturalOrder());
        actual.sort(Comparator.naturalOrder());

        assertEquals(expected, actual,
                "Streamed predicates do not match");
    }

    @Test
    public void shouldStreamSamplesWithSourceFileWhenStreamingFromFile() {
        Path srcPath = Paths.get(TestMachines.FORMULAE_GEN_MCH);


        Stream<Path> actuals = generator.streamSamplesFromFile(srcPath)
                .map(TrainingSample::getSourceFile);

        assertAll("All samples have path as " + srcPath,
                actuals.map(sampleSrc ->
                        () -> assertEquals(srcPath, sampleSrc,
                                "Unexpected source file")));
    }

    @Test
    public void shouldWriteSamplesToSourceMatchingFileWhenFromRecursiveDirectory()
            throws IOException {

        // Set up source directory and target directory
        Path srcDirectory = Files.createTempDirectory("tmpSource");
        Path targetDir = Paths.get("non/existent/path");

        // Create file hierarchy
        /*
         * tmpSource/
         * +- recursion/
         * | +- third.mch
         * +- first.mch
         * +- second.mch
         */
        Files.createDirectory(srcDirectory.resolve("subdir"));
        Path original = Paths.get(TestMachines.FORMULAE_GEN_MCH);
        Path first = srcDirectory.resolve("first.mch");
        Path second = srcDirectory.resolve("second.mch");
        Path third = srcDirectory.resolve("subdir/third.mch");
        Files.copy(original, first).toFile().deleteOnExit();
        Files.copy(original, second).toFile().deleteOnExit();
        Files.copy(original, third).toFile().deleteOnExit();

        List<TrainingSample<PredicateFeatures, PredicateLabelling>> samples =
                new ArrayList<>();

        doAnswer(invocation -> {
            TrainingData<PredicateFeatures, PredicateLabelling> in =
                    invocation.getArgument(0);
            return samples.addAll(in.getSamples().collect(Collectors.toList()));
        }).when(formatMock).writeSamples(any(TrainingData.class), any());

        generator.generateTrainingData(srcDirectory, targetDir, false);


        List<Path> firstPaths = samples.stream()
                .map(TrainingSample::getSourceFile)
                .filter(src -> src.equals(first))
                .collect(Collectors.toList());
        List<Path> secondPaths = samples.stream()
                .map(TrainingSample::getSourceFile)
                .filter(src -> src.equals(second))
                .collect(Collectors.toList());
        List<Path> thirdPaths = samples.stream()
                .map(TrainingSample::getSourceFile)
                .filter(src -> src.equals(third))
                .collect(Collectors.toList());

        firstPaths.sort(Comparator.naturalOrder());
        secondPaths.sort(Comparator.naturalOrder());
        thirdPaths.sort(Comparator.naturalOrder());

        assertAll("Check for existence of all generated predicates",
                () -> assertEquals(samples.size(),
                        firstPaths.size() + secondPaths.size() + thirdPaths.size(),
                        "Did not get all samples"),
                () -> assertEquals(firstPaths.size(), secondPaths.size(),
                        "Should have equally much samples for first and second"),
                () -> assertEquals(firstPaths.size(), thirdPaths.size(),
                        "Should have equally much samples for first and third"),
                () -> assertEquals(firstPaths.size(),
                        TestMachines.loadExpectedPredicates(
                                TestMachines.FORMULAE_GEN_MCH_PREDICATE_FILE)
                                .size(),
                        "Number of samples does not match"));
    }

    @Test
    public void shouldeEliminateCommonSourceDirectoryPattern() throws IOException {
        Path sourceDir = Paths.get(TestMachines.TEST_MACHINE_DIR);
        Path targetDir = Paths.get("non/existent");

        List<Path> files = new ArrayList<>();
        doAnswer(invocation -> {
            TrainingData data = invocation.getArgument(0);
            files.add(data.getSourceFile());
            return null;
        }).when(formatMock).writeSamples(any(TrainingData.class), any());

        generator.generateTrainingData(sourceDir, targetDir, false);

        assertAll("No file should contain the common source directory",
                files.stream().map(file -> () ->
                        assertFalse(file.startsWith(sourceDir),
                                file.toString() + " starts with " + sourceDir)));
    }

    @Test
    public void shouldReturnOnlyFileNameWhenSourceIsButOneFile() throws IOException {
        Path sourceDir = Paths.get(TestMachines.FORMULAE_GEN_MCH);
        Path targetDir = Paths.get("non/existent");

        List<Path> files = new ArrayList<>();
        doAnswer(invocation -> {
            TrainingData data = invocation.getArgument(0);
            files.add(data.getSourceFile());
            return null;
        }).when(formatMock).writeSamples(any(TrainingData.class), any());

        generator.generateTrainingData(sourceDir, targetDir, false);

        Path expected = sourceDir.getFileName();
        assertEquals(expected, files.get(0));
    }

    @Test
    void shouldCreateCsvFileWhenGeneratingTheoryFeaturesAndDecisionTimings()
            throws FormulaException, IOException {
        Backend backend = mock(Backend.class);
        when(backend.measureEvalTime(anyString(), any(StateSpace.class)))
                .thenReturn(1L);

        CsvFormat format = new CsvFormat();
        TrainingSetGenerator<TheoryFeatures, DecisionTimings> generator =
                new PredicateTrainingGenerator<>(
                        new TheoryFeatures.Generator(),
                        new DecisionTimings.Generator(1, backend),
                        format
                );

        Path source = Paths.get(TestMachines.FORMULAE_GEN_MCH);
        Path targetDir = Files.createTempDirectory("neurob-it");
        Path targetFile = format.getTargetLocation(source.getFileName(), targetDir);

        generator.generateTrainingData(source, targetDir);

        assertAll("File created and contents match",
                () -> assertTrue(Files.exists(targetFile),
                        "CSV not created: " + targetFile));

    }

}