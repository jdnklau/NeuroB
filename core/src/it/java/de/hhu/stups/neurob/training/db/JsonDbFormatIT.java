package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.testharness.TestMachines;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.PredicateTrainingGenerator;
import de.hhu.stups.neurob.training.generation.TrainingSetGenerator;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.codehaus.groovy.tools.shell.IO;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class JsonDbFormatIT {

    /**
     * Backends used by this test suite
     */
    private final Backend[] BACKENDS_USED = {
            new ProBBackend(),
            new KodkodBackend(),
            new Z3Backend(),
            new SmtBackend(),
    };

    @Test
    public void shouldWriteSamplesToJson() throws IOException {
        // Prepare training sample data to encapsulate
        BPredicate predicate = new BPredicate("pred");
        Path source = Paths.get("non/existent.mch");
        PredDbEntry labels =
                new PredDbEntry(predicate, new BMachine(source), BACKENDS_USED,
                        new TimedAnswer(Answer.VALID, 100L),
                        new TimedAnswer(Answer.VALID, 200L),
                        new TimedAnswer(Answer.VALID, 300L),
                        new TimedAnswer(Answer.UNKNOWN, 400L));
        Stream<TrainingSample<BPredicate, PredDbEntry>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, PredDbEntry> trainingData =
                new TrainingData<>(source, sampleStream);

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

        Path targetDir = Files.createTempDirectory("neurob-it");
        Path targetFile = format.getTargetLocation(source, targetDir);

        format.writeSamples(trainingData, targetDir);

        String hash = "b022209d472e8e192bcb096baf19bdf0e60c0b794e62a70da8e842f43b25f59bcbcf1c42157a"
                      + "ec97589ef858bef1b6ac287523e36efab00cc8f3adead45651af";
        String singleJsonEntry =
                "{"
                + "\"predicate\":\"pred\","
                + "\"sha512\":\"" + hash + "\","
                + "\"results\":{"
                + "\"ProB[TIME_OUT=2500]\":{\"answer\":\"VALID\",\"time-in-ns\":100,\"timeout-in-ns\":2500000000},"
                + "\"Kodkod[TIME_OUT=2500]\":{\"answer\":\"VALID\",\"time-in-ns\":200,\"timeout-in-ns\":2500000000},"
                + "\"Z3[TIME_OUT=2500]\":{\"answer\":\"VALID\",\"time-in-ns\":300,\"timeout-in-ns\":2500000000},"
                + "\"SMT_SUPPORTED_INTERPRETER[TIME_OUT=2500]\":{\"answer\":\"UNKNOWN\",\"time-in-ns\":400,\"timeout-in-ns\":2500000000}"
                + "}}";
        String expected = "{\"non/existent.mch\":{"
                          + "\"sha512\":\"Hash error: File not found\","
                          + "\"formalism\":\"CLASSICALB\","
                          + "\"gathered-predicates\":["
                          + singleJsonEntry + ","
                          + singleJsonEntry + "]}}";
        String actual = Files.lines(targetFile).collect(Collectors.joining());

        assertEquals(expected, actual,
                "File contents do not match");
    }

    @Test
    public void shouldCalculateStatistics() throws IOException {
        // Prepare training sample data to encapsulate
        BPredicate predicate = new BPredicate("pred");
        Path source = Paths.get("non/existent.mch");
        PredDbEntry labels =
                new PredDbEntry(predicate, new BMachine(source), BACKENDS_USED,
                        new TimedAnswer(Answer.VALID, 100L),
                        new TimedAnswer(Answer.VALID, 200L),
                        new TimedAnswer(Answer.VALID, 300L),
                        new TimedAnswer(Answer.UNKNOWN, 400L));
        Stream<TrainingSample<BPredicate, PredDbEntry>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, PredDbEntry> trainingData =
                new TrainingData<>(source, sampleStream);

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

        Path targetDir = Files.createTempDirectory("neurob-it");
        Path targetFile = format.getTargetLocation(source, targetDir);

        DataGenerationStats stats = format.writeSamples(trainingData, targetDir);

        assertAll("Predicate DB statistics",
                () -> assertEquals(0, stats.getFilesSeen(),
                        "Should only have seen one file"),
                () -> assertEquals(1, stats.getFilesCreated(),
                        "Should only have created one file"),
                () -> assertEquals(2, stats.getSamplesWritten(),
                        "Should have written two samples"));
    }

    @Test
    public void fileShouldBeValid() {
        Path file = Paths.get(TestMachines.FEATURES_CHECK_JSON);
        JsonDbFormat format = new JsonDbFormat();
        assertTrue(format.isValidFile(file));
    }

    @Test
    public void fileShouldBeInvalid() {
        Path file = Paths.get(TestMachines.FEATURES_CHECK_CORRUPT_JSON);
        JsonDbFormat format = new JsonDbFormat();
        assertFalse(format.isValidFile(file));
    }

    @Test
    public void fileShouldBeFlaggedAsNotAlreadyExistent() throws IOException {
        Path mch = Paths.get(TestMachines.FEATURES_CHECK_MCH);
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path json = tmpDir.resolve("corrupt.json");
        Files.copy(Paths.get(TestMachines.FEATURES_CHECK_CORRUPT_JSON), json);

        TrainingSetGenerator gen = new PredicateTrainingGenerator(
                (predicate, machineAccess) -> null,
                (predicate, machineAccess) -> null,
                new JsonDbFormat()
        );

        assertFalse(gen.dataAlreadyExists(mch,json));
    }

    @Test
    public void fileShouldBeFlaggedAsAlreadyExistent() throws IOException {
        Path mch = Paths.get(TestMachines.FEATURES_CHECK_MCH);
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path json = tmpDir.resolve("corrupt.json");
        Files.copy(Paths.get(TestMachines.FEATURES_CHECK_JSON), json);

        TrainingSetGenerator gen = new PredicateTrainingGenerator(
                (predicate, machineAccess) -> null,
                (predicate, machineAccess) -> null,
                new JsonDbFormat()
        );

        assertTrue(gen.dataAlreadyExists(mch,json));
    }

    @Test
    void shouldContainAllDataInBuckets() throws IOException {
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path json = tmpDir.resolve("formulae_gen.json");
        Files.copy(Paths.get(TestMachines.getDbPath("json/formulae_generation.json")), json);

        JsonDbFormat format = new JsonDbFormat();

        Path shuffle = tmpDir.resolve("shuffled.json");
        Random rng = new Random(1);

        format.shuffleWithBuckets(json, 2, tmpDir, rng);

        Path bucket0 = tmpDir.resolve("bucket-0.json");
        Path bucket1 = tmpDir.resolve("bucket-1.json");

        long expected0 = 84;
        long actual0 = format.loadSamples(bucket0).count();

        long expected1 = 65;
        long actual1 = format.loadSamples(bucket1).count();

        long expectedSum = 149;
        long actualSum = actual0 + actual1;

        assertAll(
                () -> assertEquals(expected0, actual0,
                        "Number of samples in first bucket does not match."),
                () -> assertEquals(expected1, actual1,
                        "Number of samples in second bucket does not match."),
                () -> assertEquals(expectedSum, actualSum,
                        "Sum of samples from both buckets does not match.")
        );
    }

    @Test
    void shouldContainAllDataAfterShuffling() throws IOException {
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path json = tmpDir.resolve("formulae_gen.json");
        Files.copy(Paths.get(TestMachines.getDbPath("json/formulae_generation.json")), json);

        JsonDbFormat format = new JsonDbFormat();

        Path shuffle = tmpDir.resolve("shuffled.json");
        Random rng = new Random(1);

        format.shuffleWithBuckets(json, 2, tmpDir, rng);

        long expected = format.loadSamples(json).count(); // 149
        long actual = format.loadSamples(shuffle).count();

        assertEquals(expected, actual, "Number of samples in shuffled data does not match");
    }

    @Test
    void shouldBeValidJsonAfterBucketShuffle() throws IOException {
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path json = tmpDir.resolve("formulae_gen.json");
        Files.copy(Paths.get(TestMachines.getDbPath("json/formulae_generation.json")), json);

        JsonDbFormat format = new JsonDbFormat();

        Path shuffle = tmpDir.resolve("shuffled.json");
        Random rng = new Random(1);

        format.shuffleWithBuckets(json, 2, tmpDir, rng);

        assertTrue(format.isValidFile(shuffle));

    }

    @Test
    void shouldShuffleFileWithBuckets() throws IOException {
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Path json = tmpDir.resolve("formulae_gen.json");
        Path expectedShuffle = Paths.get(TestMachines.getDbPath("json/formulae_generation_shuffled.json"));
        Files.copy(Paths.get(TestMachines.getDbPath("json/formulae_generation.json")), json);

        JsonDbFormat format = new JsonDbFormat();

        Path shuffle = tmpDir.resolve("shuffled.json");
        Random rng = new Random(1);

        format.shuffleWithBuckets(json, 2, tmpDir, rng);

        List<TrainingSample<BPredicate, PredDbEntry>> expected =
                format.loadSamples(expectedShuffle).collect(Collectors.toList());
        List<TrainingSample<BPredicate, PredDbEntry>> actual =
                format.loadSamples(shuffle).collect(Collectors.toList());

        assertEquals(expected, actual, "Shuffled order is unexpected");
    }

}
