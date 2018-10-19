package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
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

        Map<Backend, Double> timings = new HashMap<>();
        timings.put(new ProBBackend(), 1.);
        timings.put(new KodkodBackend(), 2.);
        timings.put(new Z3Backend(), 3.);
        timings.put(new SmtBackend(), -1.);

        DecisionTimings labels = new DecisionTimings("pred", timings,
                new ProBBackend(), new KodkodBackend(), new Z3Backend(), new SmtBackend());
        Path source = Paths.get("non/existent.mch");
        Stream<TrainingSample<BPredicate, DecisionTimings>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, DecisionTimings> trainingData =
                new TrainingData<>(source, sampleStream);

        JsonDbFormat format = new JsonDbFormat();

        Path targetDir = Files.createTempDirectory("neurob-it");
        Path targetFile = format.getTargetLocation(source, targetDir);

        format.writeSamples(trainingData, targetDir);

        String singleJsonEntry =
                "{"
                + "\"predicate\":\"pred\","
                + "\"source\":\"non/existent.mch\","
                + "\"timings\":{"
                + "\"ProBBackend\":1.0,"
                + "\"KodkodBackend\":2.0,"
                + "\"Z3Backend\":3.0,"
                + "\"SmtBackend\":-1.0"
                + "}}";
        String expected = "{\"samples\":["
                          + singleJsonEntry + ","
                          + singleJsonEntry + "]}";
        String actual = Files.lines(targetFile).collect(Collectors.joining());

        assertEquals(expected, actual,
                "File contents do not match");
    }

    @Test
    public void shouldCalculateStatistics() throws IOException {
        // Prepare training sample data to encapsulate
        BPredicate predicate = new BPredicate("pred");
        DecisionTimings labels =
                new DecisionTimings(predicate,JsonDbFormat.BACKENDS_USED, 3., 1., -1., 2.);
        Path source = Paths.get("non/existent.mch");
        Stream<TrainingSample<BPredicate, DecisionTimings>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, DecisionTimings> trainingData =
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
