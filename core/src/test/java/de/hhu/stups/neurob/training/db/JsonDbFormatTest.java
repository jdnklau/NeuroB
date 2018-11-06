package de.hhu.stups.neurob.training.db;

import com.google.gson.stream.JsonReader;
import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
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
    private final String SINGLE_PRED_ENTRY =
            getPredicateJson(
                    "pred",
                    "b022209d472e8e192bcb096baf19bdf0e60c0b794e62a70da8e842f43b25f59b"
                    + "cbcf1c42157aec97589ef858bef1b6ac287523e36efab00cc8f3adead45651af");

    /**
     * Returns a {@link PredDbEntry} instance that conforms the labelling
     * format used by {@link JsonDbFormat}.
     *
     * @param pred
     * @param probLabel
     * @param kodkodLabel
     * @param z3Label
     * @param smtLabel
     *
     * @return
     */
    private PredDbEntry getLabelling(String pred,
            Answer probLabel, Answer kodkodLabel, Answer z3Label, Answer smtLabel) {
        // Prepare timing map
        Map<Backend, TimedAnswer> timings = new HashMap<>();
        timings.put(JsonDbFormat.BACKENDS_USED[0], new TimedAnswer(probLabel, 100L));
        timings.put(JsonDbFormat.BACKENDS_USED[1], new TimedAnswer(kodkodLabel, 200L));
        timings.put(JsonDbFormat.BACKENDS_USED[2], new TimedAnswer(z3Label, 300L));
        timings.put(JsonDbFormat.BACKENDS_USED[3], new TimedAnswer(smtLabel, 400L));

        return new PredDbEntry(BPredicate.of(pred), null, JsonDbFormat.BACKENDS_USED, timings);
    }

    private PredDbEntry getLabelling(String pred) {
        return getLabelling(pred, Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN);
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

        TrainingSample<BPredicate, PredDbEntry> sample1 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN),
                Paths.get("non/existent.mch"));
        TrainingSample<BPredicate, PredDbEntry> sample2 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN),
                Paths.get("non/existent.mch"));
        TrainingSample<BPredicate, PredDbEntry> sample3 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN),
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
    public void shouldReturnEmptyStreamWhenDbFileContainsNoSamples() throws IOException {
        String fileUrl = JsonDbFormatTest.class.getClassLoader()
                .getResource("db/predicates/no_samples.json").getFile();
        Path dbFile = Paths.get(fileUrl);

        long expected = 0;
        long actual = new JsonDbFormat().loadSamples(dbFile).count();

        assertEquals(expected, actual,
                "Stream should contain no elements");
    }

    @Test
    public void shouldReturnNullWhenNoSourceMachineCanBeDetermined() throws IOException {
        String fileUrl = JsonDbFormatTest.class.getClassLoader()
                .getResource("db/predicates/no_samples.json").getFile();
        Path dbFile = Paths.get(fileUrl);

        assertNull(new JsonDbFormat().getDataSource(dbFile));
    }

    @Test
    public void shouldLoadFirstReferencedSource() throws IOException {
        String fileUrl = JsonDbFormatTest.class.getClassLoader()
                .getResource("db/predicates/example.json").getFile();
        Path dbFile = Paths.get(fileUrl);

        Path expected = Paths.get("non/existent.mch");
        Path actual = new JsonDbFormat().getDataSource(dbFile);

        assertEquals(expected, actual);
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

        iterator.hasNext(); // true, see #shouldHaveNextEntries() test case
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

        TrainingSample<BPredicate, PredDbEntry> expected =
                getSample(Paths.get("non/existent.mch"));
        TrainingSample<BPredicate, PredDbEntry> actual = iterator.next();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldIgnoreUnexpectedPropertiesWhenTranslatingToMap() throws IOException {
        String json = "{"
                      + "\"predicate\":\"pred\","
                      + "\"parade\":\"aww yes\"," // unexpected line
                      + "\"sha512\":\"hash\""
                      + "}";
        json += "]";

        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(null);

        Map<String, Object> expected = new HashMap<>();
        expected.put("predicate", BPredicate.of("pred"));
        expected.put("sha512", "hash");
        expected.put("results", new HashMap<>());

        Map<String, Object> actual = iterator.nextSampleData(reader);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldReadJsonAsMap() throws IOException {
        String hash = "b022209d472e8e192bcb096baf19bdf0e60c0b794e62a70da8e842f43b25f59b"
                      + "cbcf1c42157aec97589ef858bef1b6ac287523e36efab00cc8f3adead45651af";
        String json = getPredicateJson("pred", hash);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(null);

        Map<String, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put("ProB[TIME_OUT=2500]", new TimedAnswer(Answer.VALID, 100L));
        resultMap.put("Kodkod[TIME_OUT=2500]", new TimedAnswer(Answer.VALID, 200L));
        resultMap.put("Z3[TIME_OUT=2500]", new TimedAnswer(Answer.VALID, 300L));
        resultMap.put("SMT_SUPPORTED_INTERPRETER[TIME_OUT=2500]", new TimedAnswer(Answer.UNKNOWN, 400L));

        Map<String, Object> expected = new HashMap<>();
        expected.put("predicate", new BPredicate("pred"));
        expected.put("results", resultMap);
        expected.put("sha512", hash);

        Map<String, Object> actual = iterator.nextSampleData(reader);

        assertEquals(expected.get("results"), actual.get("results"));
    }

    @Test
    public void shouldReadEmptyMapWhenNoTimingsAreGiven() throws IOException {
        String hash = "b022209d472e8e192bcb096baf19bdf0e60c0b794e62a70da8e842f43b25f59b"
                      + "cbcf1c42157aec97589ef858bef1b6ac287523e36efab00cc8f3adead45651af";
        String json =
                "{"
                + "\"predicate\":\"pred\","
                + "\"sha512\":\"" + hash + "\""
                + "}";

        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(null);

        Map<String, TimedAnswer> resultMap = new HashMap<>();

        Map<String, Object> expected = new HashMap<>();
        expected.put("predicate", new BPredicate("pred"));
        expected.put("results", resultMap);
        expected.put("sha512", hash);

        Map<String, Object> actual = iterator.nextSampleData(reader);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldTranslateMapToDbEntry() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader);

        Map<String, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put("ProB[TIME_OUT=2500]", new TimedAnswer(Answer.VALID, 100L));
        resultMap.put("Kodkod[TIME_OUT=2500]", new TimedAnswer(Answer.VALID, 200L));
        resultMap.put("Z3[TIME_OUT=2500]", new TimedAnswer(Answer.VALID, 300L));
        resultMap.put("SMT_SUPPORTED_INTERPRETER[TIME_OUT=2500]", new TimedAnswer(Answer.UNKNOWN, 400L));

        String hash = "b022209d472e8e192bcb096baf19bdf0e60c0b794e62a70da8e842f43b25f59b"
                      + "cbcf1c42157aec97589ef858bef1b6ac287523e36efab00cc8f3adead45651af";

        Map<String, Object> data = new HashMap<>();
        data.put("predicate", new BPredicate("pred"));
        data.put("results", resultMap);
        data.put("sha512", hash);

        TrainingSample expected = getSample(null);
        TrainingSample actual = iterator.translateToDbSample(data);

        assertEquals(expected, actual);
    }


    @Test
    public void shouldWriteSample() {
        TrainingSample<BPredicate, PredDbEntry> sample =
                getSample(Paths.get("non/existent.mch"));

        JsonDbFormat format = new JsonDbFormat();

        String expected = SINGLE_PRED_ENTRY;
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual,
                "Written JSON does not match");
    }

    @Test
    public void shouldWriteSampleWhenNoSourceExists() {
        TrainingSample<BPredicate, PredDbEntry> sample = getSample();

        JsonDbFormat format = new JsonDbFormat();

        String expected = SINGLE_PRED_ENTRY;
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual,
                "Written JSON does not match");
    }

    @Test
    public void shouldEscapeQuotesInPredicate() {
        BPredicate predWithString = BPredicate.of("pred = \"string\"");
        Path source = Paths.get("non/existent.mch");
        PredDbEntry labels = getLabelling(predWithString.getPredicate());
        TrainingSample<BPredicate, PredDbEntry> sample =
                new TrainingSample<>(predWithString, labels, source);

        JsonDbFormat format = new JsonDbFormat();

        String expected = getPredicateJson("pred = \\\"string\\\"",
                "5fb270a2cdfe203e557085fb99ba4196314811362c894dad37a8dd20b6debdf4e0e1c8"
                + "d2092a168c284803ab887ce9cfa490cca55a08dc2c6b508685507c2bea");
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldEscapeBackslashInPredicate() {
        BPredicate predWithString = BPredicate.of("{1,2} /\\ {2,3} = {1,2,3}");
        Path source = Paths.get("non/existent.mch");
        PredDbEntry labels = getLabelling(predWithString.getPredicate());
        TrainingSample<BPredicate, PredDbEntry> sample =
                new TrainingSample<>(predWithString, labels, source);

        JsonDbFormat format = new JsonDbFormat();

        String expected = getPredicateJson("{1,2} /\\\\ {2,3} = {1,2,3}",
                "7546137347059d6f816fe3fdeaa56fcb45a695df5f8fbb10b2c6f7c56ce25873529406"
                + "47ceeb90d07ab2e61b1eb12f0dbcdef2c3d4f9b263effed50828390881");
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldEscapeNewlineEscapeInPredicate() {
        BPredicate predWithString = BPredicate.of("string = \"\\n\"");
        Path source = Paths.get("non/existent.mch");
        PredDbEntry labels = getLabelling(predWithString.getPredicate());
        TrainingSample<BPredicate, PredDbEntry> sample =
                new TrainingSample<>(predWithString, labels, source);

        JsonDbFormat format = new JsonDbFormat();

        String expected = getPredicateJson("string = \\\"\\\\n\\\"",
                "80429e69552a61d24c6ae66d2697722d8d6a15b0361b3feca8f5815e42c11cbc1833f3794389"
                + "433675ef4c9331150d135333841ff1c1e8dbcf01b9c94938f43b");
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldLoadSampleWhenPredicateHasBackslashes() throws IOException {
        String sampleJson = getPredicateJson("{1,2} /\\\\ {2,3} = {1,2,3}",
                "7546137347059d6f816fe3fdeaa56fcb45a695df5f8fbb10b2c6f7c56ce25873529406"
                + "47ceeb90d07ab2e61b1eb12f0dbcdef2c3d4f9b263effed50828390881");

        JsonReader reader = new JsonReader(new StringReader(sampleJson));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(null);

        BPredicate predWithString = BPredicate.of("{1,2} /\\ {2,3} = {1,2,3}");
        PredDbEntry labels = getLabelling(predWithString.getPredicate());

        TrainingSample<BPredicate, PredDbEntry> expected =
                new TrainingSample<>(predWithString, labels);

        TrainingSample<BPredicate, PredDbEntry> actual = iterator.readTrainingSample(reader);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldLoadSampleContainingNewlineEscapeAndQuotationMarks() throws IOException {
        String sampleJson = getPredicateJson("string = \\\"\\\\n\\\"",
                "f3e82f15fdb52c51b7d62b8b5ba986fc7ec5e708a9b096d5617c7a53d5e7f7f274a"
                + "1dea1e97ceecef50c2c2ae341131500c230a22262616c041972a60fef2365");
        JsonReader jsonReader = new JsonReader(new StringReader(sampleJson));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(null);

        BPredicate predWithString = BPredicate.of("string = \"\\n\"");
        PredDbEntry labels = getLabelling(predWithString.getPredicate());
        TrainingSample<BPredicate, PredDbEntry> expected =
                new TrainingSample<>(predWithString, labels);

        TrainingSample<BPredicate, PredDbEntry> actual = iterator.readTrainingSample(jsonReader);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldWriteStreamedSamplesToWriter() throws IOException {
        // Prepare training sample data to encapsulate
        BPredicate predicate = new BPredicate("pred");
        PredDbEntry labels = getLabelling(predicate.getPredicate());
        Path source = Paths.get("non/existent.mch");
        Stream<TrainingSample<BPredicate, PredDbEntry>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, PredDbEntry> trainingData =
                new TrainingData<>(source, sampleStream);

        JsonDbFormat format = new JsonDbFormat();

        StringWriter writer = new StringWriter();
        format.writeSamples(trainingData, writer);

        String expected = "{\"non/existent.mch\":{"
                          + "\"sha512\":\"no-hashing-implemented-yet\","
                          + "\"formalism\":\"CLASSICALB\","
                          + "\"gathered-predicates\":["
                          + SINGLE_PRED_ENTRY + ","
                          + SINGLE_PRED_ENTRY + ","
                          + SINGLE_PRED_ENTRY + "]}}";
        String actual = writer.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldCountWrittenSamplesInStatistics() throws IOException {
        // Prepare training sample data to encapsulate
        BPredicate predicate = new BPredicate("pred");
        PredDbEntry labels = getLabelling("pred");
        Path source = Paths.get("non/existent.mch");
        Stream<TrainingSample<BPredicate, PredDbEntry>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, PredDbEntry> trainingData =
                new TrainingData<>(source, sampleStream);

        JsonDbFormat format = new JsonDbFormat();

        StringWriter writer = new StringWriter();
        DataGenerationStats stats = format.writeSamples(trainingData, writer);

        int expected = 3;
        int actual = stats.getSamplesWritten();

        assertEquals(expected, actual);
    }

    private String getPredicateJson(String pred, String hash) {
        String json =
                "{"
                + "\"predicate\":\"" + pred + "\","
                + "\"sha512\":\"" + hash + "\","
                + "\"results\":{"
                + "\"ProB[TIME_OUT=2500]\":{\"answer\":\"VALID\",\"time-in-ns\":100,\"timeout-in-ns\":2500000000},"
                + "\"Kodkod[TIME_OUT=2500]\":{\"answer\":\"VALID\",\"time-in-ns\":200,\"timeout-in-ns\":2500000000},"
                + "\"Z3[TIME_OUT=2500]\":{\"answer\":\"VALID\",\"time-in-ns\":300,\"timeout-in-ns\":2500000000},"
                + "\"SMT_SUPPORTED_INTERPRETER[TIME_OUT=2500]\":{\"answer\":\"UNKNOWN\",\"time-in-ns\":400,\"timeout-in-ns\":2500000000}"
                + "}}";
        return json;
    }

    private String getSampleJson(int numEntries) {
        String json = "{\"non/existent.mch\":{"
                      + "\"formalism\":\"CLASSICALB\","
                      + "\"sha512\":\"I don't know which hash should be here\","
                      + "\"gathered-predicates\":[";
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < numEntries; i++) {
            entries.add(SINGLE_PRED_ENTRY);
        }
        json += String.join(",", entries);
        json += "]}}";

        return json;
    }

    private TrainingSample<BPredicate, PredDbEntry> getSample() {
        return getSample(null);
    }

    private TrainingSample<BPredicate, PredDbEntry> getSample(Path source) {
        BPredicate pred = new BPredicate("pred");
        PredDbEntry labels = getLabelling("pred1", Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN);

        return new TrainingSample<>(pred, labels, source);

    }

}
