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
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class JsonDbFormatIT {

    @Test
    public void shouldWriteSamplesToJson() throws IOException {
        // Prepare training sample data to encapsulate
        BPredicate predicate = new BPredicate("pred");
        Path source = Paths.get("non/existent.mch");
        PredDbEntry labels =
                new PredDbEntry(predicate, new BMachine(source), JsonDbFormat.BACKENDS_USED,
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

        JsonDbFormat format = new JsonDbFormat();

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
                          + "\"sha512\":\"no-hashing-implemented-yet\","
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
                new PredDbEntry(predicate, new BMachine(source), JsonDbFormat.BACKENDS_USED,
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

        JsonDbFormat format = new JsonDbFormat();

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
}
