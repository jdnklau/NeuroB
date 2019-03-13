package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatures;
import de.hhu.stups.neurob.core.features.predicates.TheoryFeatures;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.testharness.TestMachines;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.formats.CsvFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import de.prob.cli.CliVersionNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredicateTrainingGeneratorIT {

    private
    PredicateTrainingGenerator generator;

    /**
     * Backends used by this test suite
     */
    private final Backend[] BACKENDS_USED = {
            new ProBBackend(),
            new KodkodBackend(),
            new Z3Backend(),
            new SmtBackend(),
    };

    private TrainingDataFormat<PredicateFeatures, PredicateLabelling> formatMock;
    private PredicateFeatureGenerating<PredicateFeatures> featureGen;
    private PredicateLabelGenerating<PredicateLabelling> labelGen;

    private final String DB_RESOURCE_DIR =
            PredicateTrainingGeneratorIT.class.getClassLoader()
                    .getResource("db/").getFile();

    @BeforeEach
    public void setUpMocks() {
        featureGen = (pred, ss) -> generateMockedFeatures(pred);
        labelGen = (pred, ss) -> generateMockedLabels(pred);
        formatMock = mock(TrainingDataFormat.class);

        // NOTE: Generator should be set to null before each test to ensure no
        // test might accidentally run on one initialised by another test.
        // Actually, the generator might better be declared in the tests
        // themselves, but due to generics the signature is waaay to long
        // and would only hurt readability.
        generator = null;
    }

    private PredicateFeatures generateMockedFeatures(BPredicate pred) {
        return new PredicateFeatures(pred, 1., 2., 3.);
    }

    private PredicateLabelling generateMockedLabels(BPredicate pred) {
        return new PredicateLabelling(pred, 1., 2., 3.);
    }

    @Test
    public void shouldStreamSamplesWithSourcePredicateWhenStreamingFromFile()
            throws IOException {
        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

        List<String> expected = TestMachines.loadExpectedPredicates(
                TestMachines.FORMULAE_GEN_MCH_PREDICATE_FILE);

        List<String> actual = generator.streamSamplesFromFile(
                Paths.get(TestMachines.FORMULAE_GEN_MCH))
                .map(sample -> ((PredicateFeatures) sample.getData())
                        .getPredicate())
                .map(Object::toString)
                .collect(Collectors.toList());

        expected.sort(Comparator.naturalOrder());
        actual.sort(Comparator.naturalOrder());

        assertEquals(expected, actual,
                "Streamed predicates do not match");
    }

    @Test
    public void shouldStreamSamplesWithSourceFileWhenStreamingFromFile() {
        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

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
        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

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
            samples.addAll(in.getSamples().collect(Collectors.toList()));
            return new DataGenerationStats();
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
    public void shouldEliminateCommonSourceDirectoryPattern() throws IOException {
        Path sourceDir = Paths.get(TestMachines.TEST_MACHINE_DIR);
        Path targetDir = Paths.get("non/existent");

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

        List<Path> files = new ArrayList<>();
        doAnswer(invocation -> {
            TrainingData data = invocation.getArgument(0);
            files.add(data.getSourceFile());
            return new DataGenerationStats();
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

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

        List<Path> files = new ArrayList<>();
        doAnswer(invocation -> {
            TrainingData data = invocation.getArgument(0);
            files.add(data.getSourceFile());
            return new DataGenerationStats();
        }).when(formatMock).writeSamples(any(TrainingData.class), any());

        generator.generateTrainingData(sourceDir, targetDir, false);

        Path expected = sourceDir.getFileName();
        assertEquals(expected, files.get(0));
    }

    @Test
    void shouldCreateCsvFileWhenGeneratingTheoryFeaturesAndDecisionTimings()
            throws FormulaException, IOException {
        Backend backend = mock(Backend.class);
        when(backend.measureEvalTime(anyString(), any(MachineAccess.class)))
                .thenReturn(1L);

        CsvFormat format = new CsvFormat(TheoryFeatures.FEATURE_DIMENSION, 1);
        PredicateTrainingGenerator generator =
                new PredicateTrainingGenerator(
                        new TheoryFeatures.Generator(),
                        new DecisionTimings.Generator(backend),
                        format
                );

        Path source = Paths.get(TestMachines.FORMULAE_GEN_MCH);
        Path targetDir = Files.createTempDirectory("neurob-it");
        Path targetFile = format.getTargetLocation(source.getFileName(), targetDir);

        generator.generateTrainingData(source, targetDir);

        assertAll("File created and contents match",
                () -> assertTrue(Files.exists(targetFile),
                        "CSV not created: " + targetFile));
        // TODO: Check whether contents match

    }

    @Test
    void shouldCreateJsonDbFiles() throws IOException, FormulaException {
        // Set up mocked backends
        Backend b1 = mock(Backend.class);
        Backend b2 = mock(Backend.class);
        Backend[] backends = {b1, b2};
        for (int i = 0; i < backends.length; i++) {
            Backend b = backends[i];

            doReturn(new TimedAnswer(Answer.VALID, (long) i, "neurob-it mocked backend"))
                    .when(b).solvePredicate(any(), any(), any(), any());
            doReturn(new BPreferences()).when(b).getPreferences();
            doReturn(2500L).when(b).getTimeOutValue();
            doReturn(TimeUnit.MILLISECONDS).when(b).getTimeOutUnit();
        }

        JsonDbFormat format = new JsonDbFormat(new Backend[]{b1, b2});
        PredicateTrainingGenerator gen = new PredicateTrainingGenerator(
                (p, a) -> p,
                format.getLabelGenerator(
                        new CliVersionNumber("0", "1", "2", "neurob", "revision-hash"),
                        backends),
                format
        );

        // Set up working directory
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path mchDir = tmpDir.resolve("mch");
        Path targetDir = tmpDir.resolve("target");
        Files.createDirectories(mchDir);
        Files.createDirectories(mchDir.resolve("subdir"));
        Files.createDirectories(targetDir);

        // Copy machine to work with
        Files.copy(Paths.get(TestMachines.FEATURES_CHECK_MCH),
                mchDir.resolve("features_check.mch"));
        Files.copy(Paths.get(TestMachines.FEATURES_CHECK_MCH),
                mchDir.resolve("subdir/features_check.mch"));

        // Generate data
        DataGenerationStats stats = gen.generateTrainingData(mchDir, targetDir);

        // first file
        Path expected1File = Paths.get(TestMachines.FEATURES_CHECK_JSON);
        String expected1 = String.join("\n", Files.readAllLines(expected1File));
        Path actual1File = targetDir.resolve("features_check.json");
        String actual1 = String.join("\n", Files.readAllLines(actual1File));

        // second file
        Path expected2File = Paths.get(TestMachines.getDbPath("json/subdir/features_check.json"));
        String expected2 = String.join("\n", Files.readAllLines(expected2File));
        Path actual2File = targetDir.resolve("subdir/features_check.json");
        String actual2 = String.join("\n", Files.readAllLines(actual2File));

        assertAll("Equality of generated Db files",
                () -> assertEquals(expected1, actual1,
                        "Db files do not match for features_gen.mch"),
                () -> assertEquals(expected2, actual2,
                        "Db files do not match for subdir/features_gen.mch"),
                () -> assertEquals(new DataGenerationStats(2, 2, 0, 2, 0), stats,
                        "Generation stats do not match")
        );
    }

    @Test
    void shouldContainAllPredicatesAsEntriesInDb() throws IOException {
        KodkodBackend kodkod = new KodkodBackend();
        ProBBackend prob = new ProBBackend();
        SmtBackend smt = new SmtBackend();
        Z3Backend z3 = new Z3Backend();
        PredicateDbFormat format = new JsonDbFormat(BACKENDS_USED);

        PredicateTrainingGenerator gen = new PredicateTrainingGenerator();

        // Set up working directory
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path mchDir = tmpDir.resolve("mch");
        Path targetDir = tmpDir.resolve("target");
        Files.createDirectories(mchDir);
        Files.createDirectories(targetDir);

        // Copy machines to work with
        Files.copy(Paths.get(TestMachines.FORMULAE_GEN_MCH),
                mchDir.resolve("first.mch"));

        // Generate data
        DataGenerationStats stats = gen.generateTrainingData(mchDir, targetDir);

        // Count data points
        long expected = TestMachines.loadExpectedPredicates(
                TestMachines.FORMULAE_GEN_MCH_PREDICATE_FILE).size();
        long actual = format.loadSamples(targetDir.resolve("first.json")).count();

        assertAll("Number of samples",
                () -> assertEquals(expected, actual,
                        "Number of generated samples does not match"),
                () -> assertEquals(expected, stats.getSamplesWritten(),
                        "Number of samples in statistics does not match"));
    }

    @Test
    public void shouldRecogniseNonexistenceWhenDbFileNonexistent() throws IOException {
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path targetDir = tmpDir.resolve("target");
        TrainingDataFormat format = new JsonDbFormat(JsonDbFormat.DEFAULT_BACKENDS);

        // File to use
        Path mch = Paths.get(TestMachines.FEATURES_CHECK_MCH);
        Path target = format.getTargetLocation(mch, targetDir);

        // Make sure target really does not exist
        Files.deleteIfExists(target);

        PredicateTrainingGenerator gen = new PredicateTrainingGenerator();

        assertFalse(gen.dataAlreadyExists(mch, target),
                "Data should not be existent");
    }

    @Test
    public void shouldRecogniseExistenceWhenDbFileExistent() throws IOException {
        TrainingDataFormat format = new JsonDbFormat(BACKENDS_USED);

        // File to use
        Path mch = Paths.get(TestMachines.FEATURES_CHECK_MCH);

        // Copy target file to ensure it is newer than the source
        Path origTarget = Paths.get(TestMachines.FEATURES_CHECK_JSON);
        Path targetDir = Files.createTempDirectory("neurob-it");
        Path target = format.getTargetLocation(mch.getFileName(), targetDir);
        Files.copy(origTarget, target);

        PredicateTrainingGenerator gen = new PredicateTrainingGenerator();

        assertTrue(gen.dataAlreadyExists(mch, target),
                "Data should be existent");
    }

    @Test
    public void shouldStateNonexistentWhenSourceDataIsNewerThanTarget() throws IOException {
        TrainingDataFormat format = new JsonDbFormat(BACKENDS_USED);

        // Copy file to emulate fresh version
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path mch = Paths.get(TestMachines.FEATURES_CHECK_MCH);
        Path newMch = Files.copy(mch, tmpDir.resolve(mch.getFileName()));

        // Set up targets
        Path targetDir = Paths.get(TestMachines.getDbPath(""));
        Path target = format.getTargetLocation(mch.getFileName(), targetDir);

        PredicateTrainingGenerator gen = new PredicateTrainingGenerator();

        assertFalse(gen.dataAlreadyExists(newMch, target),
                "Data should be marked as nonexistent");
    }

    @Test
    public void shouldIgnoreFilesFromExcludeList() throws IOException {
        PredicateDbFormat format = mock(PredicateDbFormat.class);
        Set<Path> computedFiles = Collections.synchronizedSet(new HashSet<>());
        doAnswer(invocation -> {
            TrainingData data = invocation.getArgument(0);
            Path file = data.getSourceFile();
            computedFiles.add(file);
            return new DataGenerationStats();
        }).when(format).writeSamples(any(), any());


        PredicateTrainingGenerator gen = new PredicateTrainingGenerator(
                (p, a) -> p,
                (p, a) -> new PredicateLabelling("pred", 1., 2., 3.),
                format
        );

        // Set up working directory
        // tmp/
        // +- features_check.mch
        // +- subdir/
        // |  +- features_check.mch
        // |  +- featurinos.mch
        // +- subdir2/
        //    +- features_check.mch

        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path mchDir = tmpDir.resolve("mch");
        Path targetDir = tmpDir.resolve("target");
        Files.createDirectories(mchDir);
        Files.createDirectories(mchDir.resolve("subdir"));
        Files.createDirectories(mchDir.resolve("subdir2"));
        Files.createDirectories(targetDir);

        // Copy machine to work with
        Files.copy(Paths.get(TestMachines.FEATURES_CHECK_MCH),
                mchDir.resolve("features_check.mch"));
        Files.copy(Paths.get(TestMachines.FEATURES_CHECK_MCH),
                mchDir.resolve("subdir/features_check.mch"));
        Files.copy(Paths.get(TestMachines.FEATURES_CHECK_MCH),
                mchDir.resolve("subdir/featurinos.mch"));
        Files.copy(Paths.get(TestMachines.FEATURES_CHECK_MCH),
                mchDir.resolve("subdir2/features_check.mch"));

        Collection<Path> excludes = new HashSet<>();
        excludes.add(Paths.get("subdir/featurinos.mch"));
        excludes.add(Paths.get("subdir2/"));

        // Generate data
        gen.generateTrainingData(mchDir, targetDir, false, excludes);

        Set<Path> expected = new HashSet<>();
        expected.add(Paths.get("features_check.mch"));
        expected.add(Paths.get("subdir/features_check.mch"));

        assertEquals(expected, computedFiles);
    }

}
