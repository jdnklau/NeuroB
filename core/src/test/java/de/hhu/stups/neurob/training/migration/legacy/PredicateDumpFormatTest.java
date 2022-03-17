package de.hhu.stups.neurob.training.migration.legacy;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredDbEntry;
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
    private TrainingSample<BPredicate, PredDbEntry> sample0;
    private TrainingSample<BPredicate, PredDbEntry> sample1;
    private TrainingSample<BPredicate, PredDbEntry> sample2;
    private TrainingSample<BPredicate, PredDbEntry> sample3;

    @BeforeEach
    public void setupDbSamplesFromMigration() {
        // Prepare timings
        Map<Backend, TimedAnswer> timings = new HashMap<>();
        timings.put(PredicateDump.PROB,
                new TimedAnswer(Answer.SOLVABLE, 1L));
        timings.put(PredicateDump.KODKOD,
                new TimedAnswer(Answer.SOLVABLE, 2L));
        timings.put(PredicateDump.Z3,
                new TimedAnswer(Answer.SOLVABLE, 3L));
        timings.put(PredicateDump.SMT,
                new TimedAnswer(Answer.SOLVABLE, 4L));

        // Prepare samples
        Backend[] backends = PredicateDump.BACKENDS_USED; // shortcut
        sample0 = new TrainingSample<>(
                new BPredicate("null:PREDICATES"),
                new PredDbEntry(
                        new BPredicate("null:PREDICATES"),
                        new BMachine((Path) null),
                        backends, timings));
        sample1 = new TrainingSample<>(
                new BPredicate("first:PREDICATES"),
                new PredDbEntry(
                        BPredicate.of("first:PREDICATES"),
                        new BMachine(Paths.get("first/source/machine.mch")),
                        backends, timings),
                Paths.get("first/source/machine.mch"));
        sample2 = new TrainingSample<>(
                new BPredicate("second:PREDICATES"),
                new PredDbEntry(
                        BPredicate.of("second:PREDICATES"),
                        new BMachine(Paths.get("second/source/machine.mch")),
                        backends, timings),
                Paths.get("second/source/machine.mch"));
        sample3 = new TrainingSample<>(
                new BPredicate("third:PREDICATES"),
                new PredDbEntry(
                        BPredicate.of("third:PREDICATES"),
                        new BMachine(Paths.get("second/source/machine.mch")),
                        backends, timings),
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

    @Test
    void shouldTranslateNegativeTimeToTimeoutAnswer() {
        Double time = -1.0;

        PredicateDumpFormat format = new PredicateDumpFormat();

        TimedAnswer expected = new TimedAnswer(Answer.TIMEOUT, null);
        TimedAnswer actual = format.translateTiming(time);

        assertEquals(expected, actual);
    }

    @Test
    void shouldTranslateNonNegativeTimeToSolvableAnswer() {
        Double time = 1.234E8;

        PredicateDumpFormat format = new PredicateDumpFormat();

        TimedAnswer expected = new TimedAnswer(Answer.SOLVABLE, 123400000L);
        TimedAnswer actual = format.translateTiming(time);

        assertEquals(expected, actual);
    }

}
