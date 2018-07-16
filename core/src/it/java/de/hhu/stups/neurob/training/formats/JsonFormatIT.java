package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.testharness.TestFeatures;
import de.hhu.stups.neurob.testharness.TestLabelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private Features createFeatures(Double... features) {
        return new TestFeatures(features);
    }

    private Labelling createLabels(Double... labels) {
        return new TestLabelling(labels);
    }

}