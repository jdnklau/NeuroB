package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.testharness.TestFeatures;
import de.hhu.stups.neurob.testharness.TestLabelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatIT {

    @Test
    public void shouldWriteJsonToNullFileWhenNoSourceFileIsSpecified()
            throws IOException {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels);

        JsonFormat format = new JsonFormat();

        Path targetDir = Files.createTempDirectory("neurob");
        Path targetFile = targetDir.resolve("null.json");

        String expected =
                "{\"samples\":["
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]},"
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]}"
                + "]}";

        format.writeSamples(
                new TrainingData<>(null, Stream.of(sample, sample)),
                targetDir);
        String actual = Files.lines(targetFile).collect(Collectors.joining());

        assertEquals(expected, actual);
    }

    @Test
    public void shouldWriteJsonToFileWhenSourceFileIsSpecifiedAndNoSubdirExists()
            throws IOException {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels);

        Path source = Paths.get("non-existent.mch");
        JsonFormat format = new JsonFormat();

        Path targetDir = Files.createTempDirectory("neurob");
        Path targetFile = targetDir.resolve("non-existent.json");

        String expected =
                "{\"samples\":["
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]},"
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]}"
                + "]}";

        format.writeSamples(
                new TrainingData<>(source, Stream.of(sample, sample)),
                targetDir);
        String actual = Files.lines(targetFile).collect(Collectors.joining());

        assertEquals(expected, actual);
    }

    @Test
    public void shouldWriteJsonToFileWhenSourceFileIsSpecified()
            throws IOException {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels);

        Path source = Paths.get("non/existent.mch");
        JsonFormat format = new JsonFormat();

        Path targetDir = Files.createTempDirectory("neurob");
        Path targetFile = targetDir.resolve("non/existent.json");

        String expected =
                "{\"samples\":["
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]},"
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]}"
                + "]}";

        format.writeSamples(
                new TrainingData<>(source, Stream.of(sample, sample)),
                targetDir);
        String actual = Files.lines(targetFile).collect(Collectors.joining());

        assertEquals(expected, actual);
    }

    @Test
    public void shouldCalculateStatistics()
            throws IOException {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels);

        Path source = Paths.get("non/existent.mch");
        JsonFormat format = new JsonFormat();

        Path targetDir = Files.createTempDirectory("neurob");
        Path targetFile = targetDir.resolve("non/existent.json");

        DataGenerationStats stats = format.writeSamples(
                new TrainingData<>(source, Stream.of(sample, sample, sample, sample)),
                targetDir);

        assertAll("Json statistics",
                () -> assertEquals(1, stats.getFilesSeen(),
                        "Should only have seen one file"),
                () -> assertEquals(1, stats.getFilesCreated(),
                        "Should only have created one file"),
                () -> assertEquals(4, stats.getSamplesWritten(),
                        "Should have written two samples"));
    }

    @Test
    public void shouldLoadSamplesFromJson() throws IOException {
        Path jsonDir = Paths.get(
                JsonFormatIT.class.getClassLoader()
                .getResource("formats/json/").getFile());

        JsonFormat format = new JsonFormat();

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(new Features(1., 2.), new Labelling(3., 4., 5.));

        List<TrainingSample<Features, Labelling>> expected = new ArrayList<>();
        expected.add(sample);
        expected.add(sample);

        List<TrainingSample<Features, Labelling>> actual =
                format.loadSamples(jsonDir.resolve("test.json"))
                .collect(Collectors.toList());

        assertEquals(expected, actual,
                "Read samples do not match");
    }

    private Features createFeatures(Double... features) {
        return new TestFeatures(features);
    }

    private Labelling createLabels(Double... labels) {
        return new TestLabelling(labels);
    }

}
