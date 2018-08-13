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
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CsvFormatTest {

    @Test
    public void shouldUseCsvExtensionWhenMchMachine() {
        Path source = Paths.get("non/existent.mch");
        Path targetDir = Paths.get("target/dir");

        CsvFormat format = new CsvFormat();

        Path expected = Paths.get("target/dir/non/existent.csv");
        Path actual = format.getTargetLocation(source, targetDir);

        assertEquals(expected, actual,
                "Generated target file name does not match");
    }

    @Test
    public void shouldUseCsvExtensionWhenBcmMachine() {
        Path source = Paths.get("non/existent.bcm");
        Path targetDir = Paths.get("target/dir");

        CsvFormat format = new CsvFormat();

        Path expected = Paths.get("target/dir/non/existent.csv");
        Path actual = format.getTargetLocation(source, targetDir);

        assertEquals(expected, actual,
                "Generated target file name does not match");
    }

    @Test
    public void shouldTranslateToCsv() {
        Features f = new TestFeatures(1., 2., 3.);
        Labelling l = new TestLabelling(4., 5.);
        TrainingSample sample = new TrainingSample<>(f, l);

        String expected = "1.0,2.0,3.0,4.0,5.0";
        String actual = new CsvFormat().generateCsvEntry(sample);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldWriteEntriesToCsv() throws IOException {
        Features f = new TestFeatures(1., 2., 3.);
        Labelling l = new TestLabelling(4., 5.);

        Stream<TrainingSample<Features, Labelling>> sampleStream =
                Stream.of(new TrainingSample<>(f, l),
                        new TrainingSample<>(f, l),
                        new TrainingSample<>(f, l));

        StringWriter writer = new StringWriter();
        CsvFormat format = new CsvFormat(writer);

        format.writeSamples(new TrainingData<>(null, sampleStream), null);

        String expected = "1.0,2.0,3.0,4.0,5.0\n"
                          + "1.0,2.0,3.0,4.0,5.0\n"
                          + "1.0,2.0,3.0,4.0,5.0\n";
        String actual = writer.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldCountSamplesWritten() throws IOException {
        Features f = new TestFeatures(1., 2., 3.);
        Labelling l = new TestLabelling(4., 5.);

        Stream<TrainingSample<Features, Labelling>> sampleStream =
                Stream.of(new TrainingSample<>(f, l),
                        new TrainingSample<>(f, l),
                        new TrainingSample<>(f, l));

        StringWriter writer = new StringWriter();
        CsvFormat format = new CsvFormat(writer);

        DataGenerationStats stats = format.writeSamples(new TrainingData<>(null, sampleStream), null);

        int expected = 3;
        int actual = stats.getSamplesWritten();

        assertEquals(expected, actual,
                "Count of written samples does not match");

    }

}
