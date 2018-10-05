package de.hhu.stups.neurob.training.db;

import com.google.gson.stream.JsonReader;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
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

class JsonDbFormatTest {
    /**
     * Entry of a single sample wrt this unit test context.
     * Used for assertions.
     */
    private final String SINGLE_JSON_ENTRY =
            "{"
            + "\"predicate\":\"pred\","
            + "\"source\":\"non/existent.mch\","
            + "\"timings\":{"
            + "\"ProBBackend\":1.0,"
            + "\"KodkodBackend\":2.0,"
            + "\"Z3Backend\":3.0,"
            + "\"SmtBackend\":-1.0"
            + "}}";

    /**
     * Entry of a single sample wrt this unit test context, but without a
     * source defined.
     * Used for assertions.
     */
    private final String SINGLE_JSON_ENTRY_WITHOUT_SOURCE =
            "{"
            + "\"predicate\":\"pred\","
            + "\"timings\":{"
            + "\"ProBBackend\":1.0,"
            + "\"KodkodBackend\":2.0,"
            + "\"Z3Backend\":3.0,"
            + "\"SmtBackend\":-1.0"
            + "}}";

    private DecisionTimings labelling;

    @BeforeEach
    void setupLabelling() {
    }

    /**
     * Returns a {@link DecisionTimings} instance that conforms the labelling
     * format used by {@link JsonDbFormat}.
     *
     * @param pred Wrapped predicate
     *
     * @return
     */
    private DecisionTimings getLabelling(String pred,
            Double probLabel, Double kodkodLabel, Double z3Label, Double smtLabel) {
        // Prepare timing map
        Map<Backend, Double> timings = new HashMap<>();

        timings.put(JsonDbFormat.BACKENDS_USED[0], probLabel);
        timings.put(JsonDbFormat.BACKENDS_USED[1], kodkodLabel);
        timings.put(JsonDbFormat.BACKENDS_USED[2], z3Label);
        timings.put(JsonDbFormat.BACKENDS_USED[3], smtLabel);

        return new DecisionTimings(pred, timings, JsonDbFormat.BACKENDS_USED);
    }

    @Test
    public void shouldGiveJsonPathWhenMch() {
        JsonDbFormat dbFormat = new JsonDbFormat();

        Path source = Paths.get("non/existent/source.mch");
        Path targetDir = Paths.get("target/dir");
        Path expected = Paths.get("target/dir/non/existent/source.json");

        assertEquals(expected, dbFormat.getTargetLocation(source, targetDir));
    }

    @Test
    public void shouldLoadSamples() throws IOException {
        String fileUrl = JsonDbFormatTest.class.getClassLoader()
                .getResource("db/predicates/example.json").getFile();
        Path dbFile = Paths.get(fileUrl);

        TrainingSample sample1 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", 1., 2., 4., -1.),
                Paths.get("non/existent.mch"));
        TrainingSample sample2 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", 1., 2., 4., -1.),
                Paths.get("non/existent.mch"));
        TrainingSample sample3 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", 1., 2., 4., -1.),
                Paths.get("other/non/existent.mch"));

        List<TrainingSample> expected = new ArrayList<>();
        expected.add(sample1);
        expected.add(sample2);
        expected.add(sample3);

        List<TrainingSample> actual = new JsonDbFormat().loadSamples(dbFile)
                .collect(Collectors.toList());

        assertEquals(expected, actual,
                "Loaded entries do not match");
    }

    @Test
    public void shouldHaveNoNextEntriesWhenInitialisedWithEmptyData() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldHaveNextEntries() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        assertTrue(iterator.hasNext());
    }

    @Test
    public void shouldHaveOnlyOneEntry() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        iterator.next();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldHaveMoreThanOneEntry() throws IOException {
        String json = getSampleJson(2);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        iterator.next();
        assertTrue(iterator.hasNext());
    }

    @Test
    public void shouldReadSample() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        TrainingSample<BPredicate, DecisionTimings> expected =
                getSample(Paths.get("non/existent.mch"));
        TrainingSample<BPredicate, DecisionTimings> actual = iterator.next();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldThrowExceptionWhenTranslatingToMapWithUnexpectedProperties() throws IOException {
        String json = "{\"samples\":["
                      + "{"
                      + "\"predicate\":\"pred\","
                      + "\"parade\":\"aww yes\"," // unexpected line
                      + "\"source\":\"non/existent.mch\","
                      + "\"timings\":{"
                      + "\"ProBBackend\":1.0,"
                      + "\"Z3Backend\":2.0,"
                      + "\"KodkodBackend\":3.0,"
                      + "\"SmtBackend\":-1"
                      + "}}";
        json += "]}";

        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);


        assertThrows(IllegalStateException.class,
                iterator::nextSampleData);
    }

    @Test
    public void shouldReadJsonAsMap() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        Map<String, String> expected = new HashMap<>();
        expected.put("predicate", "pred");
        expected.put("source", "non/existent.mch");
        expected.put("timings",
                "ProBBackend:1.0,"
                + "KodkodBackend:2.0,"
                + "Z3Backend:3.0,"
                + "SmtBackend:-1.0");

        Map<String, String> actual = iterator.nextSampleData();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldReadJsonAsMapWhenNoSourceIsGiven() throws IOException {
        String json = "{\"samples\":["
                      + "{"
                      + "\"predicate\":\"pred\","
                      + "\"timings\":{"
                      + "\"ProBBackend\":1.0,"
                      + "\"Z3Backend\":3.0,"
                      + "\"KodkodBackend\":2.0,"
                      + "\"SmtBackend\":-1"
                      + "}}";
        json += "]}";

        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);


        Map<String, String> expected = new HashMap<>();
        expected.put("predicate", "pred");
        expected.put("timings",
                "ProBBackend:1.0,"
                + "KodkodBackend:2.0,"
                + "Z3Backend:3.0,"
                + "SmtBackend:-1.0");

        Map<String, String> actual = iterator.nextSampleData();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldThrowExceptionWhenNoTimingsAreGiven() throws IOException {
        String json = "{\"samples\":["
                      + "{"
                      + "\"predicate\":\"pred\","
                      + "\"source\":\"non/existent.mch\""
                      + "}}";
        json += "]}";

        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        assertThrows(IllegalStateException.class,
                iterator::nextSampleData);
    }

    @Test
    public void shouldTranslateMapToDbEntry() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        Map<String, String> data = new HashMap<>();
        data.put("predicate", "pred");
        data.put("timings",
                "KodkodBackend:2.0,"
                + "ProBBackend:1.0,"
                + "SmtBackend:-1.0,"
                + "Z3Backend:3.0");

        TrainingSample expected = getSample(null);
        TrainingSample actual = iterator.translateToDbSample(data);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldTranslateTimingsOrdered() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        String timings =
                "KodkodBackend:2.0,"
                + "ProBBackend:1.0,"
                + "SmtBackend:4.0,"
                + "Z3Backend:3.0";
        Labelling expected = new Labelling(1., 2., 3., 4.);
        Labelling actual = iterator.translateTimings(timings);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldTranslateTimingsIgnoringAdditionalBackends() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        String timings =
                "KodkodBackend:2.0,"
                + "ProBBackend:1.0,"
                + "SmtBackend:4.0,"
                + "NonExistingBackend:5.0," // does not exist
                + "Z3Backend:3.0";
        Labelling expected = new Labelling(1., 2., 3., 4.);
        Labelling actual = iterator.translateTimings(timings);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldTranslateTimingsSubstitutingMissingEntriesWithNegativeOne()
            throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        String timings =
                "KodkodBackend:2.0,"
                + "Z3Backend:3.0";
        Labelling expected = new Labelling(-1., 2., 3., -1.);
        Labelling actual = iterator.translateTimings(timings);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldWriteSample() {
        TrainingSample<BPredicate, DecisionTimings> sample =
                getSample(Paths.get("non/existent.mch"));

        JsonDbFormat format = new JsonDbFormat();

        String expected = SINGLE_JSON_ENTRY;
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual,
                "Written JSON does not match");
    }

    @Test
    public void shouldWriteSampleWhenNoSourceExists() {
        TrainingSample<BPredicate, DecisionTimings> sample = getSample();

        JsonDbFormat format = new JsonDbFormat();

        String expected = SINGLE_JSON_ENTRY_WITHOUT_SOURCE;
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual,
                "Written JSON does not match");
    }

    @Test
    public void shouldThrowExceptionWhenLessLabelsThanBackends() {
        TrainingSample<BPredicate, Labelling> sample =
                new TrainingSample<>(
                        BPredicate.of("pred"),
                        new Labelling(1., 2.)
                );

        JsonDbFormat format = new JsonDbFormat();

        assertThrows(IllegalArgumentException.class,
                () -> format.translateSampleToJsonObject(sample));
    }

    @Test
    public void shouldThrowExceptionWhenMoreLabelsThanBackends() {
        // Prepare training sample to write
        BPredicate predicate = new BPredicate("pred");
        Labelling labels = new Labelling(1., 2., 3., 4., 5.);
        TrainingSample<BPredicate, Labelling> sample =
                new TrainingSample<>(predicate, labels);

        JsonDbFormat format = new JsonDbFormat();

        assertThrows(IllegalArgumentException.class,
                () -> format.translateSampleToJsonObject(sample));
    }

    @Test
    public void shouldWriteStreamedSamplesToWriter() throws IOException {
        // Prepare training sample data to encapsulate
        BPredicate predicate = new BPredicate("pred");
        Labelling labels = new Labelling(1., 2., 3., -1.);
        Path source = Paths.get("non/existent.mch");
        Stream<TrainingSample<BPredicate, Labelling>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, Labelling> trainingData =
                new TrainingData<>(source, sampleStream);

        JsonDbFormat format = new JsonDbFormat();

        StringWriter writer = new StringWriter();
        format.writeSamples(trainingData, writer);

        String expected = "{\"samples\":["
                          + SINGLE_JSON_ENTRY + ","
                          + SINGLE_JSON_ENTRY_WITHOUT_SOURCE + ","
                          + SINGLE_JSON_ENTRY + "]}";
        String actual = writer.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldCountWrittenSamplesInStatistics() throws IOException {
        // Prepare training sample data to encapsulate
        BPredicate predicate = new BPredicate("pred");
        Labelling labels = new Labelling(1., 2., 3., -1.);
        Path source = Paths.get("non/existent.mch");
        Stream<TrainingSample<BPredicate, Labelling>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, Labelling> trainingData =
                new TrainingData<>(source, sampleStream);

        JsonDbFormat format = new JsonDbFormat();

        StringWriter writer = new StringWriter();
        DataGenerationStats stats = format.writeSamples(trainingData, writer);

        int expected = 3;
        int actual = stats.getSamplesWritten();

        assertEquals(expected, actual);
    }

    private String getSampleJson(int numEntries) {
        String json = "{\"samples\":[";
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < numEntries; i++) {
            entries.add(SINGLE_JSON_ENTRY);
        }
        json += String.join(",", entries);
        json += "]}";

        return json;
    }

    private TrainingSample<BPredicate, DecisionTimings> getSample() {
        return getSample(null);
    }

    private TrainingSample<BPredicate, DecisionTimings> getSample(Path source) {
        BPredicate pred = new BPredicate("pred");
        DecisionTimings labels = getLabelling("pred", 1., 2., 3., -1.);

        return new TrainingSample<>(pred, labels, source);

    }

}
