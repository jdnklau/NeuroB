package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

class PredicateDbMigrationTest {

    private PredicateDbFormat sourceFormatMock;

    private final TrainingSample<BPredicate, Labelling> sample0 = new TrainingSample<>(
            new BPredicate("null:PREDICATES"),
            new Labelling(1.0, 2.0, 3.0, 4.0));
    private final TrainingSample<BPredicate, Labelling> sample1 = new TrainingSample<>(
            new BPredicate("first:PREDICATES"),
            new Labelling(1.0, 2.0, 3.0, 4.0),
            Paths.get("first/source/machine.mch"));
    private final TrainingSample<BPredicate, Labelling> sample2 = new TrainingSample<>(
            new BPredicate("second:PREDICATES"),
            new Labelling(1.0, 2.0, 3.0, 4.0),
            Paths.get("second/source/machine.mch"));
    private final TrainingSample<BPredicate, Labelling> sample3 = new TrainingSample<>(
            new BPredicate("third:PREDICATES"),
            new Labelling(1.0, 2.0, 3.0, 4.0),
            Paths.get("second/source/machine.mch"));

    @BeforeEach
    public void setupSourceFormatMock() throws IOException {
        sourceFormatMock = mock(PredicateDbFormat.class);

        when(sourceFormatMock.loadSamples(Paths.get("non/existent/src")))
                .thenReturn(Stream.of(sample0, sample1, sample2, sample3));
    }

    @Test
    public void shouldMigrateFileFromSourceToTargetFormat() throws IOException {
        PredicateDbMigration migration = new PredicateDbMigration(sourceFormatMock);

        PredicateDbFormat targetFormatMock = mock(PredicateDbFormat.class);
        StringWriter writer = new StringWriter();
        when(targetFormatMock.writeSamples(any(TrainingData.class), any(Path.class)))
                .thenAnswer(invocation -> {
                    TrainingData data = invocation.getArgument(0);
                    data.getSamples().forEach(
                            sample ->
                                    writer.append(sample.toString()).write('\n'));
                    writer.flush();
                    return new DataGenerationStats();
                });

        migration.migrateFile(
                Paths.get("non/existent/src"),
                Paths.get("non/existent/target/"),
                targetFormatMock);

        String expected =
                sample0.toString() + "\n"
                + sample1.toString() + "\n"
                + sample2.toString() + "\n"
                + sample3.toString() + "\n";
        String actual = writer.toString();


        assertEquals(expected, actual,
                "Samples were not correctly translated into target format");
    }

}
