package de.hhu.stups.neurob.training.migration.legacy;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PredicateDumpFormatTest {

    private final Path MIGRATION_SOURCE =
            Paths.get(getClass()
                    .getClassLoader()
                    .getResource("db/migration/migrate.pdump")
                    .getFile());
    private TrainingSample<BPredicate, DecisionTimings> sample0;
    private TrainingSample<BPredicate, DecisionTimings> sample1;
    private TrainingSample<BPredicate, DecisionTimings> sample2;
    private TrainingSample<BPredicate, DecisionTimings> sample3;

    @BeforeEach
    public void setupDbSamplesFromMigration() {
        // Prepare timings
        Map<Backend, Double> timings = new HashMap<>();
        timings.put(PredicateDump.PROB, 1.0);
        timings.put(PredicateDump.KODKOD, 2.0);
        timings.put(PredicateDump.Z3, 3.0);
        timings.put(PredicateDump.SMT, 4.0);

        // Prepare samples
        sample0 = new TrainingSample<>(
                new BPredicate("null:PREDICATES"),
                new DecisionTimings("", timings, PredicateDump.BACKENDS_USED));
        sample1 = new TrainingSample<>(
                new BPredicate("first:PREDICATES"),
                new DecisionTimings("", timings, PredicateDump.BACKENDS_USED),
                Paths.get("first/source/machine.mch"));
        sample2 = new TrainingSample<>(
                new BPredicate("second:PREDICATES"),
                new DecisionTimings("", timings, PredicateDump.BACKENDS_USED),
                Paths.get("second/source/machine.mch"));
        sample3 = new TrainingSample<>(
                new BPredicate("third:PREDICATES"),
                new DecisionTimings("", timings, PredicateDump.BACKENDS_USED),
                Paths.get("second/source/machine.mch"));

    }

    @Test
    void shouldReturnPdumpAsExtension() {
        assertEquals("pdump", new PredicateDumpFormat().getFileExtension(),
                "Extension should be pdump");
    }

    @Test
    void shouldLoadSamplesFromFile() throws IOException {
        PredicateDumpFormat format = new PredicateDumpFormat();

        List<TrainingSample> expected = new ArrayList<>();
        expected.add(sample0);
        expected.add(sample1);
        expected.add(sample2);
        expected.add(sample3);
        List<TrainingSample> actual = format.loadSamples(MIGRATION_SOURCE).collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    @Test
    void shouldLoadFirstOriginalSource() throws IOException {
        PredicateDumpFormat format = new PredicateDumpFormat();

        Path expected = Paths.get("first/source/machine.mch");
        Path actual = format.getDataSource(MIGRATION_SOURCE);

        assertEquals(expected, actual);
    }

    @Test
    void shouldWritePdumpToTarget() throws IOException {
        PredicateDumpFormat format = new PredicateDumpFormat();

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
