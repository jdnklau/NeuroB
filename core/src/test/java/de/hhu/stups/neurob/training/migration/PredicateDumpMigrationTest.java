package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.DbSample;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDump;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredicateDumpMigrationTest {

    private final Path MIGRATION_SOURCE =
            Paths.get(getClass()
                    .getClassLoader()
                    .getResource("db/migration/migrate.pdump")
                    .getFile());
    private DbSample<BPredicate> sample0;
    private DbSample<BPredicate> sample1;
    private DbSample<BPredicate> sample2;
    private DbSample<BPredicate> sample3;

    @BeforeEach
    public void setupDbSamplesFromMigration() {
        sample0 = new DbSample<>(
                new BPredicate("null:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0));
        sample1 = new DbSample<>(
                new BPredicate("first:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0),
                Paths.get("first/source/machine"));
        sample2 = new DbSample<>(
                new BPredicate("second:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0),
                Paths.get("second/source/machine"));
        sample3 = new DbSample<>(
                new BPredicate("third:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0),
                Paths.get("second/source/machine"));

    }

    @Test
    public void shouldTranslatePredicateDumpIntoDbSample() {
        PredicateDump pdump = new PredicateDump("1.0,2.0,3.0,-1.0:predicate:PREDICATES");

        PredicateDumpMigration migration = new PredicateDumpMigration();

        // Set up expected sample
        DbSample<BPredicate> expected = new DbSample<>(
                new BPredicate("predicate:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, -1.0));
        DbSample<BPredicate> actual = migration.translate(pdump);

        assertEquals(expected, actual,
                "Migration into DbSample failed");
    }

    @Test
    public void shouldTranslatePredicateDumpWithSourceIntoDbSampleWithSource() {
        PredicateDump pdump = new PredicateDump("1.0,2.0,3.0,-1.0:predicate:PREDICATES",
                Paths.get("nonexistent/mch"));

        PredicateDumpMigration migration = new PredicateDumpMigration();

        // Set up expected sample
        DbSample<BPredicate> expected = new DbSample<>(
                new BPredicate("predicate:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, -1.0),
                Paths.get("nonexistent/mch"));
        DbSample<BPredicate> actual = migration.translate(pdump);

        assertEquals(expected, actual,
                "Migration into DbSample failed");
    }

    @Test
    public void shouldStreamDbSamplesFromPDumpFile() throws IOException {
        PredicateDumpMigration migration = new PredicateDumpMigration();

        List<DbSample<BPredicate>> expected = new ArrayList<>();
        expected.add(sample0);
        expected.add(sample1);
        expected.add(sample2);
        expected.add(sample3);

        List<DbSample<BPredicate>> actual =
                migration.streamTranslatedSamples(MIGRATION_SOURCE).collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    @Test
    public void shouldMigrateFileToFormat() throws IOException {
        PredicateDumpMigration migration = new PredicateDumpMigration();

        // Mock format for writing
        StringWriter writer = new StringWriter();
        PredicateDbFormat format = mock(PredicateDbFormat.class);
        when(format.writeSamples(any(TrainingData.class), any(Path.class)))
                .thenAnswer(invocation -> {
                    TrainingData data = invocation.getArgument(0);
                    System.out.println("foo");
                    data.getSamples().forEach(
                            sample ->
                                    writer.append(sample.toString()).write('\n'));
                    writer.flush();
                    return new DataGenerationStats();
                });


        migration.migrateFile(MIGRATION_SOURCE, Paths.get("non/existent/file"), format);


        // Helper function for translation step of samples
        Function<DbSample<BPredicate>, TrainingSample> toTrainingSample =
                sample -> new TrainingSample(
                        new PredicateFeatures(sample.getBElement().getPredicate()),
                        sample.getLabelling(), sample.getSourceMachine());

        String expected =
                toTrainingSample.apply(sample0) + "\n"
                + toTrainingSample.apply(sample1) + "\n"
                + toTrainingSample.apply(sample2) + "\n"
                + toTrainingSample.apply(sample3) + "\n";
        String actual = writer.toString();

        assertEquals(expected, actual,
                "Not all entries were written to the format");
    }

    @Test
    public void shouldMigrateDirectory() throws IOException {
        PredicateDumpMigration migration = new PredicateDumpMigration();

        // Mock format for writing
        StringWriter writer = new StringWriter();
        PredicateDbFormat format = mock(PredicateDbFormat.class);
        when(format.writeSamples(any(TrainingData.class), any(Path.class)))
                .thenAnswer(invocation -> {
                    TrainingData data = invocation.getArgument(0);
                    System.out.println("foo");
                    data.getSamples().forEach(
                            sample ->
                                    writer.append(sample.toString()).write('\n'));
                    writer.flush();
                    return new DataGenerationStats();
                });


        migration.migrate(MIGRATION_SOURCE.getParent(), Paths.get("non/existent/file"), format);


        // Helper function for translation step of samples
        Function<DbSample<BPredicate>, TrainingSample> toTrainingSample =
                sample -> new TrainingSample(
                        new PredicateFeatures(sample.getBElement().getPredicate()),
                        sample.getLabelling(), sample.getSourceMachine());

        String expected =
                toTrainingSample.apply(sample0) + "\n"
                + toTrainingSample.apply(sample1) + "\n"
                + toTrainingSample.apply(sample2) + "\n"
                + toTrainingSample.apply(sample3) + "\n";
        String actual = writer.toString();

        assertEquals(expected, actual,
                "Not all entries were written to the format");

    }
}
