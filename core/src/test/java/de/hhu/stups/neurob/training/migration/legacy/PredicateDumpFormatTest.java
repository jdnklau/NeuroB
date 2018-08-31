package de.hhu.stups.neurob.training.migration.legacy;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.DbSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PredicateDumpFormatTest {

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
    void shouldReturnPdumpAsExtension() {
        assertEquals("pdump", new PredicateDumpFormat().getFileExtension(),
                "Extension should be pdump");
    }

    @Test
    void shouldLoadSamplesFromFile() throws IOException {
        PredicateDumpFormat format = new PredicateDumpFormat();

        List<DbSample> expected = new ArrayList<>();
        expected.add(sample0);
        expected.add(sample1);
        expected.add(sample2);
        expected.add(sample3);
        List<DbSample> actual = format.loadSamples(MIGRATION_SOURCE).collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    @Test
    void shouldWritePdumpToTarget() throws IOException {
        PredicateDumpFormat format = new PredicateDumpFormat();

        // Set up training data
        Stream<TrainingSample> samples = Stream.of(
                new TrainingSample<>(
                        new PredicateFeatures("predicate"),
                        new Labelling(1.0, 2.0, 3.0, 4.0)),
                new TrainingSample<>(
                        new PredicateFeatures("predicate"),
                        new Labelling(1.0, 2.0, 3.0, 4.0),
                        Paths.get("non/existent.mch")),
                new TrainingSample<>(
                        new PredicateFeatures("predicate"),
                        new Labelling(1.0, 2.0, 3.0, 4.0),
                        Paths.get("non/existent.mch"))
        );
        TrainingData data = new TrainingData(Paths.get("non/existent.mch"), samples);

        StringWriter writer = new StringWriter();
        format.writeSamples(data, writer);

        String expected =
                "1.0,2.0,3.0,4.0:predicate\n"
                + "#source:non/existent.mch\n"
                + "1.0,2.0,3.0,4.0:predicate\n"
                + "1.0,2.0,3.0,4.0:predicate\n";
        String actual = writer.toString();

        assertEquals(expected, actual,
                "Written entry does not match");
    }

    @Test
    void shouldAddNewSourceAnnotationWhenPathsDiffer() throws IOException {
        Path oldPath = Paths.get("old/path");
        Path newPath = Paths.get("new/path");

        StringWriter writer = new StringWriter();
        new PredicateDumpFormat().addNewSourceIfNecessary(newPath, oldPath, writer);

        String expected = "#source:new/path\n";
        String actual = writer.toString();

        assertEquals(expected, actual);
    }

    @Test
    void shouldAddNewSourceAnnotationWhenOldIsNull() throws IOException {
        Path oldPath = null;
        Path newPath = Paths.get("new/path");

        StringWriter writer = new StringWriter();
        new PredicateDumpFormat().addNewSourceIfNecessary(newPath, oldPath, writer);

        String expected = "#source:new/path\n";
        String actual = writer.toString();

        assertEquals(expected, actual);
    }

    @Test
    void shouldNotAddSourceAnnotationWhenPathsEqual() throws IOException {
        Path oldPath = Paths.get("old/path");
        Path newPath = Paths.get("old/path");

        StringWriter writer = new StringWriter();
        new PredicateDumpFormat().addNewSourceIfNecessary(newPath, oldPath, writer);

        String expected = "";
        String actual = writer.toString();

        assertEquals(expected, actual);
    }

    @Test
    void shouldNotAddSourceAnnotationWhenBothPathsAreNull() throws IOException {
        Path oldPath = null;
        Path newPath = null;

        StringWriter writer = new StringWriter();
        new PredicateDumpFormat().addNewSourceIfNecessary(newPath, oldPath, writer);

        String expected = "";
        String actual = writer.toString();

        assertEquals(expected, actual);
    }

}
