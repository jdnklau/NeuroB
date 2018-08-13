package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.testharness.TestFeatures;
import de.hhu.stups.neurob.testharness.TestLabelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatTest {

    @Test
    public void shouldReturnDummyNameWhenSourceFileIsNull() {
        JsonFormat format = new JsonFormat();

        Path targetDirectory = Paths.get("target/dir/");

        Path expected = Paths.get("target/dir/null.json");
        Path actual = format.getTargetLocation(null, targetDirectory);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnWithJsonFileExtensionWhenMchFile() {
        TrainingDataFormat<Features> format = new JsonFormat();
        Path sourceFile = Paths.get("non/existent.mch");
        Path targetDirectory = Paths.get("target/dir/");

        Path expected = Paths.get("target/dir/non/existent.json");
        Path actual = format.getTargetLocation(sourceFile, targetDirectory);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnWithJsonFileExtensionWhenBcmFile() {
        TrainingDataFormat<Features> format = new JsonFormat();
        Path sourceFile = Paths.get("non/existent.bcm");
        Path targetDirectory = Paths.get("target/dir/");

        Path expected = Paths.get("target/dir/non/existent.json");
        Path actual = format.getTargetLocation(sourceFile, targetDirectory);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldJsonifyFeaturesLabellingAndSourceFile() {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);
        Path source = Paths.get("non/existend.mch");

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels, source);

        String expected =
                "{\"sourceFile\":\"non/existend.mch\","
                + "\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]}";
        String actual = new JsonFormat().createJsonEntry(sample);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldJsonifyWithoutSourceFileWhenSourceFileIsNull() {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels);

        String expected =
                "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]}";
        String actual = new JsonFormat().createJsonEntry(sample);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldWriteEmptyJsonObjectWhenStreamIsEmpty() throws IOException {
        JsonFormat format =
                new JsonFormat();

        StringWriter out = new StringWriter();
        format.writeSamples(Stream.empty(), out);

        String expected =
                "{\"samples\":[]}";
        String actual = out.toString();

        assertEquals(expected, actual);

    }

    @Test
    public void shouldWriteSingleTrainingSample() throws IOException {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels);

        JsonFormat format = new JsonFormat();

        StringWriter out = new StringWriter();
        format.writeSamples(Stream.of(sample), out);

        String expected =
                "{\"samples\":["
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]}"
                + "]}";
        String actual = out.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldWriteTwoTrainingSamples() throws IOException {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels);

        JsonFormat format = new JsonFormat();

        StringWriter out = new StringWriter();
        format.writeSamples(Stream.of(sample, sample), out);

        String expected =
                "{\"samples\":["
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]},"
                + "{\"features\":[1.0,2.0],"
                + "\"labelling\":[3.0,4.0,5.0]}"
                + "]}";
        String actual = out.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldCountWrittenSamplesForStatistics() throws IOException {
        Features features = createFeatures(1., 2.);
        Labelling labels = createLabels(3., 4., 5.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(features, labels);

        JsonFormat format = new JsonFormat();

        StringWriter out = new StringWriter();
        DataGenerationStats stats = format.writeSamples(Stream.of(sample, sample), out);

        int expected = 2;
        int actual = stats.getSamplesWritten();

        assertEquals(expected, actual);
    }

    private Features createFeatures(Double... features) {
        return new TestFeatures(features);
    }

    private Labelling createLabels(Double... labels) {
        return new TestLabelling(labels);
    }
}
