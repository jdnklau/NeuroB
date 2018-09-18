package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.DbSample;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

class PredicateDbMigrationTest {

    private PredicateDbFormat sourceFormatMock;

    private final DbSample<BPredicate> sample0 = new DbSample<>(
            new BPredicate("null:PREDICATES"),
            new Labelling(1.0, 2.0, 3.0, 4.0));
    private final DbSample<BPredicate> sample1 = new DbSample<>(
            new BPredicate("first:PREDICATES"),
            new Labelling(1.0, 2.0, 3.0, 4.0),
            Paths.get("first/source/machine.mch"));
    private final DbSample<BPredicate> sample2 = new DbSample<>(
            new BPredicate("second:PREDICATES"),
            new Labelling(1.0, 2.0, 3.0, 4.0),
            Paths.get("second/source/machine.mch"));
    private final DbSample<BPredicate> sample3 = new DbSample<>(
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
    public void shouldTranslateIntoTrainingSampleWithNoFeaturesButPredicate() {
        PredicateDbMigration migration = new PredicateDbMigration();

        DbSample<BPredicate> dbSample = new DbSample<>(
                new BPredicate("null:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0));

        TrainingSample<PredicateFeatures, Labelling> expected = new TrainingSample<>(
                new PredicateFeatures("null:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0));
        TrainingSample<PredicateFeatures, Labelling> actual = migration.translate(dbSample);

        assertEquals(expected, actual,
                "Translated sample does not match");
    }

    @Test
    public void shouldTranslateIntoTrainingSampleWithSourceWhenDbSampleHasSource() {
        PredicateDbMigration migration = new PredicateDbMigration();

        DbSample<BPredicate> dbSample = new DbSample<>(
                new BPredicate("null:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0),
                Paths.get("no/such/file"));

        TrainingSample<PredicateFeatures, Labelling> expected = new TrainingSample<>(
                new PredicateFeatures("null:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0),
                Paths.get("no/such/file"));
        TrainingSample<PredicateFeatures, Labelling> actual = migration.translate(dbSample);

        assertEquals(expected, actual,
                "Translated sample does not match");
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
                dbToTrainingSample(sample0).toString() + "\n"
                + dbToTrainingSample(sample1).toString() + "\n"
                + dbToTrainingSample(sample2).toString() + "\n"
                + dbToTrainingSample(sample3).toString() + "\n";
        String actual = writer.toString();


        assertEquals(expected, actual,
                "Samples were not correctly translated into target format");
    }

    private TrainingSample dbToTrainingSample(DbSample<BPredicate> dbSample) {
        return new TrainingSample(
                new PredicateFeatures(dbSample.getBElement().getPredicate()),
                dbSample.getLabelling(), dbSample.getSourceMachine());
    }

}
