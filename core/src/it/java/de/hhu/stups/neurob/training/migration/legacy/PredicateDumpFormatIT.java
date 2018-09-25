package de.hhu.stups.neurob.training.migration.legacy;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
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

class PredicateDumpFormatIT {

    @Test
    void shouldWritePdumpToFile() throws IOException {
        Path targetDir = Files.createTempDirectory("neurob-it");
        Path sourceFile = Paths.get("non/existent.mch");

        // Set up training data
        Stream<TrainingSample> samples = Stream.of(
                new TrainingSample<>(
                        new BPredicate("predicate"),
                        new Labelling(1.0, 2.0, 3.0, 4.0)),
                new TrainingSample<>(
                        new BPredicate("predicate"),
                        new Labelling(1.0, 2.0, 3.0, 4.0),
                        Paths.get("non/existent.mch")),
                new TrainingSample<>(
                        new BPredicate("predicate"),
                        new Labelling(1.0, 2.0, 3.0, 4.0),
                        Paths.get("non/existent.mch"))
        );
        TrainingData data = new TrainingData(Paths.get("non/existent.mch"), samples);

        new PredicateDumpFormat().writeSamples(data, targetDir);

        String expected =
                "1.0,2.0,3.0,4.0:predicate\n"
                + "#source:non/existent.mch\n"
                + "1.0,2.0,3.0,4.0:predicate\n"
                + "1.0,2.0,3.0,4.0:predicate";
        String actual = Files.lines(targetDir.resolve("non/existent.pdump"))
                .collect(Collectors.joining("\n"));

        assertEquals(expected, actual);
    }
}
