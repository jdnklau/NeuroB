package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
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

    private final TrainingSample<BPredicate, DecisionTimings> sample0 = new TrainingSample<>(
            new BPredicate("null:PREDICATES"),
            new DecisionTimings("null:PREDICATES", JsonDbFormat.BACKENDS_USED, 1.0, 2.0, 3.0, 4.0));
    private final TrainingSample<BPredicate, DecisionTimings> sample1 = new TrainingSample<>(
            new BPredicate("first:PREDICATES"),
            new DecisionTimings("first:PREDICATES", JsonDbFormat.BACKENDS_USED, 1.0, 2.0, 3.0, 4.0),
            Paths.get("first/source/machine.mch"));
    private final TrainingSample<BPredicate, DecisionTimings> sample2 = new TrainingSample<>(
            new BPredicate("second:PREDICATES"),
            new DecisionTimings("second:PREDICATES", JsonDbFormat.BACKENDS_USED, 1.0, 2.0, 3.0, 4.0),
            Paths.get("second/source/machine.mch"));
    private final TrainingSample<BPredicate, DecisionTimings> sample3 = new TrainingSample<>(
            new BPredicate("third:PREDICATES"),
            new DecisionTimings("third:PREDICATES", JsonDbFormat.BACKENDS_USED, 1.0, 2.0, 3.0, 4.0),
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
                "[data=null:PREDICATES, labels=1.0,2.0,3.0,4.0]\n"
                + "[data=first:PREDICATES, labels=1.0,2.0,3.0,4.0, source=first/source/machine.mch]\n"
                + "[data=second:PREDICATES, labels=1.0,2.0,3.0,4.0, source=second/source/machine.mch]\n"
                + "[data=third:PREDICATES, labels=1.0,2.0,3.0,4.0, source=second/source/machine.mch]\n";
        String actual = writer.toString();

        assertEquals(expected, actual,
                "Samples were not correctly translated into target format");
    }

    @Test
    public void shouldTranslateSampleWithFeatureGenAndLabelTranslationWhenNoSource() throws FeatureCreationException {
        PredicateDbMigration migration = new PredicateDbMigration(sourceFormatMock);

        TrainingSample<Features, Labelling> expected =
                new TrainingSample<>(
                        new Features(0., 1., 2.),
                        new Labelling(1., 2., 3., 4.));

        TrainingSample<Features, Labelling> actual = migration.migrateSample(sample0,
                (predicate, bMachine) -> new Features(0., 1., 2.),
                timings -> new Labelling(timings.getLabellingArray()));

        assertEquals(expected, actual,
                "Translated training sample does not match");
    }

    @Test
    public void shouldTranslateSampleWithFeatureGenAndLabelTranslationWhenSourceIsGiven()
            throws FeatureCreationException {
        PredicateDbMigration migration = new PredicateDbMigration(sourceFormatMock);

        TrainingSample<Features, Labelling> expected =
                new TrainingSample<>(
                        new Features(0., 1., 2.),
                        new Labelling(1., 2., 3., 4.),
                        Paths.get("first/source/machine.mch"));

        TrainingSample<Features, Labelling> actual = migration.migrateSample(sample1,
                (predicate, bMachine) -> new Features(0., 1., 2.),
                timings -> new Labelling(timings.getLabellingArray()));

        assertEquals(expected, actual,
                "Translated training sample does not match");
    }

    @Test
    public void shouldMigrateFileWithFeatureAndLabelTranslation() throws IOException {
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
                Paths.get("non/existent"),
                Paths.get("non/existent/target/"),
                (predicate, bMachine) -> new Features(0., 1., 2.),
                timings -> new PredicateLabelling(timings.getPredicate(), timings.getLabellingArray()),
                targetFormatMock);

        String expected =
                "[data=0.0,1.0,2.0, labels=1.0,2.0,3.0,4.0]\n"
                + "[data=0.0,1.0,2.0, labels=1.0,2.0,3.0,4.0, source=first/source/machine.mch]\n"
                + "[data=0.0,1.0,2.0, labels=1.0,2.0,3.0,4.0, source=second/source/machine.mch]\n"
                + "[data=0.0,1.0,2.0, labels=1.0,2.0,3.0,4.0, source=second/source/machine.mch]\n";
        String actual = writer.toString();

        assertEquals(expected, actual,
                "Samples were not correctly translated into target format");
    }

}
