package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.labelling.Labelling;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TrainingDataTest {

    private Stream<TrainingSample<Integer, Labelling>> genSampleStream(Integer... values) {
        return Arrays.stream(values).map(v -> new TrainingSample<>(v, new Labelling(v.doubleValue())));
    }

    private TrainingData<Integer, Labelling> genData(String source, Integer... values) {
        return new TrainingData<>(Paths.get(source), genSampleStream(values));
    }

    @Test
    void shouldSplitFiftyFifty() {
        TrainingData<Integer, Labelling> data = genData("non/existent", 1, 2, 3, 4, 5, 6, 7, 8);

        List<TrainingSample<Integer, Labelling>> expected1 =
                genSampleStream(2, 3, 4, 6).collect(Collectors.toList());
        List<TrainingSample<Integer, Labelling>> expected2 =
                genSampleStream(1, 5, 7, 8).collect(Collectors.toList());

        TrainingData<Integer, Labelling>[] actual = data.split(0.5, 1);
        List<TrainingSample<Integer, Labelling>> actual1 =
                actual[0].getSamples().collect(Collectors.toList());
        List<TrainingSample<Integer, Labelling>> actual2 =
                actual[1].getSamples().collect(Collectors.toList());

        assertAll(
                () -> assertEquals(expected1, actual1, "First half does not match"),
                () -> assertEquals(expected2, actual2, "Second half does not match")
        );
    }

    @Test
    void shouldSplitThird() {
        TrainingData<Integer, Labelling> data = genData("non/existent", 1, 2, 3, 4, 5, 6, 7, 8, 9);

        List<TrainingSample<Integer, Labelling>> expected1 =
                genSampleStream(3, 4, 6).collect(Collectors.toList());
        List<TrainingSample<Integer, Labelling>> expected2 =
                genSampleStream(1, 2, 5, 7, 8, 9).collect(Collectors.toList());

        TrainingData<Integer, Labelling>[] actual = data.split(1. / 3., 1);
        List<TrainingSample<Integer, Labelling>> actual1 =
                actual[0].getSamples().collect(Collectors.toList());
        List<TrainingSample<Integer, Labelling>> actual2 =
                actual[1].getSamples().collect(Collectors.toList());

        assertAll(
                () -> assertEquals(expected1, actual1, "First half does not match"),
                () -> assertEquals(expected2, actual2, "Second half does not match")
        );
    }

    @Test
    void shouldHaveSameMetadataAfterSplitting() {
        Path source = Paths.get("source.mch");
        Path absolute = Paths.get("absulute/path/to/source.mch");
        TrainingData<Integer, Labelling> data =
                new TrainingData<>(source, absolute, genSampleStream(1, 2, 3));

        TrainingData<Integer, Labelling>[] actual = data.split(1. / 3., 1);
        TrainingData<Integer, Labelling> d1 = actual[0];
        TrainingData<Integer, Labelling> d2 = actual[1];

        assertAll(
                () -> assertEquals(source, d1.getSourceFile(),
                        "Source file of first split does not match"),
                () -> assertEquals(absolute, d1.getAbsoluteSourcePath(),
                        "Absolute path of first split does not match"),
                () -> assertEquals(source, d2.getSourceFile(),
                        "Source file of second split does not match"),
                () -> assertEquals(absolute, d2.getAbsoluteSourcePath(),
                        "Absolute path of second split does not match")
        );
    }

}
