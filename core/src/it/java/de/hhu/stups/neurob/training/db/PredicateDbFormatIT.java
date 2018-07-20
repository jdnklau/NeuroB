package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
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

class PredicateDbFormatIT {

    @Test
    public void shouldWriteSamplesToJson() throws IOException {
        // Prepare training sample data to encapsulate
        PredicateFeatures features = new PredicateFeatures("pred");
        Labelling labels = new Labelling(3., 1., -1., 2.);
        Path source = Paths.get("non/existent.mch");
        Stream<TrainingSample<PredicateFeatures, Labelling>> sampleStream =
                Stream.of(
                        new TrainingSample<>(features, labels, source),
                        new TrainingSample<>(features, labels, source));
        TrainingData<PredicateFeatures, Labelling> trainingData =
                new TrainingData<>(source, sampleStream);

        PredicateDbFormat format = new PredicateDbFormat();

        Path targetDir = Files.createTempDirectory("neurob-it");
        Path targetFile = format.getTargetLocation(source, targetDir);

        format.writeSamples(trainingData, targetDir);

        String singleJsonEntry =
                "{"
                + "\"predicate\":\"pred\","
                + "\"source\":\"non/existent.mch\","
                + "\"timings\":{"
                + "\"KodkodBackend\":3.0,"
                + "\"ProBBackend\":1.0,"
                + "\"SmtBackend\":-1.0,"
                + "\"Z3Backend\":2.0"
                + "}}";
        String expected = "{\"samples\"=["
                          + singleJsonEntry + ","
                          + singleJsonEntry + "]}";
        String actual = Files.lines(targetFile).collect(Collectors.joining());

        assertEquals(expected, actual,
                "File contents do not match");
    }

    protected DbSample<BPredicate> getSample() {
        Path source = Paths.get("non/existent.mch");
        return getSample(source);
    }

    private DbSample<BPredicate> getSample(Path source) {
        BPredicate pred = new BPredicate("pred");
        Labelling labelling = new Labelling(3., 1., -1., 2.);

        return new DbSample<>(pred, labelling, source);
    }
}
