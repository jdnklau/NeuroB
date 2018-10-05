package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CsvFormatIT {

    /** Path to the directory containing Csv files for the tests */
    private final Path CSV_DIR =
            Paths.get(CsvFormatIT.class.getClassLoader()
                    .getResource("formats/csv").getFile());

    /** Testing CSV File with header */
    private final Path CSV_WITH_HEADER = CSV_DIR.resolve("header_true.csv");
    /** Testing CSV file without header */
    private final Path CSV_WITHOUT_HEADER = CSV_DIR.resolve("header_false.csv");

    @Test
    public void shouldStreamSamplesWhenHeaderExists() throws IOException {
        CsvFormat format = new CsvFormat(3, 2, true);

        List<TrainingSample<Features, Labelling>> expected = new ArrayList<>();
        expected.add(new TrainingSample<>(
                new Features(1., 2., 3.),
                new Labelling(4., 5.)));
        expected.add(new TrainingSample<>(
                new Features(6., 7., 8.),
                new Labelling(9., 10.)));

        List<TrainingSample<Features, Labelling>> actual =
                format.loadSamples(CSV_WITH_HEADER)
                        .collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    @Test
    public void shouldStreamSamplesWhenNoHeaderExists() throws IOException {
        CsvFormat format = new CsvFormat(3, 2, false);

        List<TrainingSample<Features, Labelling>> expected = new ArrayList<>();
        expected.add(new TrainingSample<>(
                new Features(1., 2., 3.),
                new Labelling(4., 5.)));
        expected.add(new TrainingSample<>(
                new Features(6., 7., 8.),
                new Labelling(9., 10.)));

        List<TrainingSample<Features, Labelling>> actual =
                format.loadSamples(CSV_WITHOUT_HEADER)
                        .collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    @Test
    public void shouldWriteCsvWithHeader() throws IOException {
        Path targetDir = Files.createTempDirectory("neurob-it");

        CsvFormat format = new CsvFormat(3, 2, true);

        List<TrainingSample<Features, Labelling>> samples = new ArrayList<>();
        samples.add(new TrainingSample<>(
                new Features(1., 2., 3.),
                new Labelling(4., 5.)));
        samples.add(new TrainingSample<>(
                new Features(6., 7., 8.),
                new Labelling(9., 10.)));
        TrainingData<Features, Labelling> data =
                new TrainingData<>(Paths.get("test.mch"), samples.stream());

        format.writeSamples(data, targetDir);

        String expected =
                "Feature0,Feature1,Feature2,Label0,Label1\n"
                + "1.0,2.0,3.0,4.0,5.0\n"
                + "6.0,7.0,8.0,9.0,10.0";
        String actual = Files.lines(targetDir.resolve("test.csv")).collect(Collectors.joining("\n"));

        assertEquals(expected, actual,
                "File contents do not match");
    }

    @Test
    public void shouldWriteCsvWithoutHeader() throws IOException {
        Path targetDir = Files.createTempDirectory("neurob-it");

        CsvFormat format = new CsvFormat(3, 2, false);

        List<TrainingSample<Features, Labelling>> samples = new ArrayList<>();
        samples.add(new TrainingSample<>(
                new Features(1., 2., 3.),
                new Labelling(4., 5.)));
        samples.add(new TrainingSample<>(
                new Features(6., 7., 8.),
                new Labelling(9., 10.)));
        TrainingData<Features, Labelling> data =
                new TrainingData<>(Paths.get("test.mch"), samples.stream());

        format.writeSamples(data, targetDir);

        String expected =
                "1.0,2.0,3.0,4.0,5.0\n"
                + "6.0,7.0,8.0,9.0,10.0";
        String actual = Files.lines(targetDir.resolve("test.csv")).collect(Collectors.joining("\n"));

        assertEquals(expected, actual,
                "File contents do not match");
    }
}
