package de.hhu.stups.neurob.training.db;

import com.google.gson.stream.JsonReader;
import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import de.prob.cli.CliVersionNumber;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
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
     * Backends used by this test suite
     */
    private final Backend[] BACKENDS_USED = {
            new ProBBackend(),
            new KodkodBackend(),
            new Z3Backend(),
            new SmtBackend(),
    };

    private final Path BEVERAGE_VENDING_MACHINE = Paths.get(
            JsonDbFormat.class.getClassLoader().getResource("db/mch/bvm.mch").getFile());

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
        return getLabelling(pred, probLabel, kodkodLabel, z3Label, smtLabel,
                new CliVersionNumber("0", "1", "2", "neurob",
                        "revision-hash"));
    }

    /**
     * Returns a {@link PredDbEntry} instance that conforms the labelling
     * format used by {@link JsonDbFormat}.
     *
     * @param pred
     * @param source
     * @param probLabel
     * @param kodkodLabel
     * @param z3Label
     * @param smtLabel
     *
     * @return
     */
    private PredDbEntry getLabelling(String pred, Path source,
            Answer probLabel, Answer kodkodLabel, Answer z3Label, Answer smtLabel) {
        return getLabelling(pred, source, probLabel, kodkodLabel, z3Label, smtLabel,
                new CliVersionNumber("0", "1", "2", "neurob",
                        "revision-hash"));
    }

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
            Answer probLabel, Answer kodkodLabel, Answer z3Label, Answer smtLabel,
            CliVersionNumber cliVersion) {
        return getLabelling(pred, null,
                probLabel, kodkodLabel, z3Label, smtLabel,
                cliVersion);
    }

    private PredDbEntry getLabelling(String pred, Path source,
            Answer probLabel, Answer kodkodLabel, Answer z3Label, Answer smtLabel,
            CliVersionNumber cliVersion) {
        // Prepare timing map
        Map<Backend, TimedAnswer> timings = new HashMap<>();
        timings.put(BACKENDS_USED[0], new TimedAnswer(probLabel, 100L));
        timings.put(BACKENDS_USED[1], new TimedAnswer(kodkodLabel, 200L));
        timings.put(BACKENDS_USED[2], new TimedAnswer(z3Label, 300L));
        timings.put(BACKENDS_USED[3], new TimedAnswer(smtLabel, 400L));

        BMachine sourceMch = (source != null) ? new BMachine(source) : null;
        return new PredDbEntry(BPredicate.of(pred), sourceMch, BACKENDS_USED, timings, cliVersion);
    }

    private PredDbEntry getLabelling(String pred) {
        return getLabelling(pred, Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN);
    }

    @Test
    public void shouldGiveJsonPathWhenMch() {
        JsonDbFormat dbFormat = new JsonDbFormat(BACKENDS_USED);

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
        Path src = Paths.get("non/existent.mch");
        Path src2 = Paths.get("other/non/existent.mch");

        TrainingSample<BPredicate, PredDbEntry> sample1 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", src, Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN),
                src);
        TrainingSample<BPredicate, PredDbEntry> sample2 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", src, Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN),
                src);
        TrainingSample<BPredicate, PredDbEntry> sample3 = new TrainingSample<>(
                new BPredicate("pred1"),
                getLabelling("pred1", src2, Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN),
                src2);

        List<TrainingSample> expected = new ArrayList<>();
        expected.add(sample1);
        expected.add(sample2);
        expected.add(sample3);

        List<TrainingSample> actual = new JsonDbFormat(BACKENDS_USED).loadSamples(dbFile)
                .collect(Collectors.toList());

        assertEquals(expected, actual,
                "Loaded entries do not match");
    }

    @Test
    public void shouldLoadDataWhenInGeneratedOrder() throws IOException {
        String json = "{\"" + BEVERAGE_VENDING_MACHINE.toString() + "\":{"
                      + "\"sha512\":\"eea0db1101cc2928c3eb62d3b3409fd5456faca283ac61226789843619f75"
                      + "7e1e5214d22811f66cab42f2949e706417b5f844b2fbc952d08073b8137e38c98ee\","
                      + "\"formalism\":\"CLASSICALB\","
                      + "\"gathered-predicates\":[" + SINGLE_PRED_ENTRY + "]}}";

        JsonReader jreader = new JsonReader(new StringReader(json));
        JsonDbFormat.PredicateDbIterator iter =
                new JsonDbFormat.PredicateDbIterator(jreader, BACKENDS_USED);

        TrainingSample<BPredicate, PredDbEntry> expected = new TrainingSample<>(
                new BPredicate("pred"),
                getLabelling("pred",
                        BEVERAGE_VENDING_MACHINE,
                        Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN),
                BEVERAGE_VENDING_MACHINE);
        TrainingSample<BPredicate, PredDbEntry> actual = iter.next();

        assertAll(
                () -> assertEquals(expected, actual),
                () -> assertFalse(iter.hasNext())
        );
    }

    @Test
    @Disabled("Not possible to implement upon current code")
    public void shouldLoadDataIrregardlessOfOrder() throws IOException {
        String json = "{\"" + BEVERAGE_VENDING_MACHINE.toString() + "\":{"
                      + "\"formalism\":\"CLASSICALB\","
                      + "\"gathered-predicates\":[" + SINGLE_PRED_ENTRY + "],"
                      + "\"sha512\":\"eea0db1101cc2928c3eb62d3b3409fd5456faca283ac61226789843619f75"
                      + "7e1e5214d22811f66cab42f2949e706417b5f844b2fbc952d08073b8137e38c98ee\"}}";

        JsonReader jreader = new JsonReader(new StringReader(json));
        JsonDbFormat.PredicateDbIterator iter =
                new JsonDbFormat.PredicateDbIterator(jreader, BACKENDS_USED);

        TrainingSample<BPredicate, PredDbEntry> expected = new TrainingSample<>(
                new BPredicate("pred"),
                getLabelling("pred", Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN),
                BEVERAGE_VENDING_MACHINE);
        TrainingSample<BPredicate, PredDbEntry> actual = iter.next();

        assertAll(
                () -> assertEquals(expected, actual),
                () -> assertFalse(iter.hasNext())
        );
    }

    @Test
    public void shouldReturnEmptyStreamWhenDbFileContainsNoSamples() throws IOException {
        String fileUrl = JsonDbFormatTest.class.getClassLoader()
                .getResource("db/predicates/no_samples.json").getFile();
        Path dbFile = Paths.get(fileUrl);

        long expected = 0;
        long actual = new JsonDbFormat(BACKENDS_USED).loadSamples(dbFile).count();

        assertEquals(expected, actual,
                "Stream should contain no elements");
    }

    @Test
    public void shouldReturnNullWhenNoSourceMachineCanBeDetermined() throws IOException {
        String fileUrl = JsonDbFormatTest.class.getClassLoader()
                .getResource("db/predicates/no_samples.json").getFile();
        Path dbFile = Paths.get(fileUrl);

        assertNull(new JsonDbFormat(BACKENDS_USED).getDataSource(dbFile));
    }

    @Test
    public void shouldLoadFirstReferencedSource() throws IOException {
        String fileUrl = JsonDbFormatTest.class.getClassLoader()
                .getResource("db/predicates/example.json").getFile();
        Path dbFile = Paths.get(fileUrl);

        Path expected = Paths.get("non/existent.mch");
        Path actual = new JsonDbFormat(BACKENDS_USED).getDataSource(dbFile);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldHaveNoNextEntriesWhenInitialisedWithEmptyData() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader, BACKENDS_USED);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldHaveNextEntries() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader, BACKENDS_USED);

        assertTrue(iterator.hasNext());
    }

    @Test
    public void shouldHaveOnlyOneEntry() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader, BACKENDS_USED);

        iterator.hasNext(); // true, see #shouldHaveNextEntries() test case
        iterator.next();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldHaveMoreThanOneEntry() throws IOException {
        String json = getSampleJson(2);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader, BACKENDS_USED);

        iterator.next();
        assertTrue(iterator.hasNext());
    }

    @Test
    public void shouldReadSample() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        JsonDbFormat.PredicateDbIterator iterator =
                new JsonDbFormat.PredicateDbIterator(reader, BACKENDS_USED);

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
                new JsonDbFormat.PredicateDbIterator(null, BACKENDS_USED);

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
                new JsonDbFormat.PredicateDbIterator(null, BACKENDS_USED);

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
                new JsonDbFormat.PredicateDbIterator(null, BACKENDS_USED);

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
                new JsonDbFormat.PredicateDbIterator(reader, BACKENDS_USED);

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

        CliVersionNumber version = new CliVersionNumber("0", "1", "2", "neurob", "revision-hash");
        data.put("probcli", version);

        TrainingSample expected = getSample(null);
        TrainingSample actual = iterator.translateToDbSample(data);

        assertEquals(expected, actual);
    }


    @Test
    public void shouldWriteSample() {
        TrainingSample<BPredicate, PredDbEntry> sample =
                getSample(Paths.get("non/existent.mch"));

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

        String expected = SINGLE_PRED_ENTRY;
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual,
                "Written JSON does not match");
    }

    @Test
    public void shouldWriteSampleWhenNoSourceExists() {
        TrainingSample<BPredicate, PredDbEntry> sample = getSample();

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

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

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

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

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

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

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

        String expected = getPredicateJson("string = \\\"\\\\n\\\"",
                "80429e69552a61d24c6ae66d2697722d8d6a15b0361b3feca8f5815e42c11cbc1833f3794389"
                + "433675ef4c9331150d135333841ff1c1e8dbcf01b9c94938f43b");
        String actual = format.translateSampleToJsonObject(sample);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldEscapeLinebreaksInPredicate() {
        BPredicate predWithString = BPredicate.of("string = \"\n\r\"");
        Path source = Paths.get("non/existent.mch");
        PredDbEntry labels = getLabelling(predWithString.getPredicate());
        TrainingSample<BPredicate, PredDbEntry> sample =
                new TrainingSample<>(predWithString, labels, source);

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

        String expected = getPredicateJson("string = \\\"\\n\\r\\\"",
                "c64f548a1142d08ae0554dba5365469ef976b6deb529ef3875867c725988c4c44ae7d8723d62"
                + "d623b717f63fbe2792812dcffda3aea351ba5d3f6e15fc199eb2");
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
                new JsonDbFormat.PredicateDbIterator(null, BACKENDS_USED);

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
                new JsonDbFormat.PredicateDbIterator(null, BACKENDS_USED);

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
        Path source = BEVERAGE_VENDING_MACHINE;
        Stream<TrainingSample<BPredicate, PredDbEntry>> sampleStream =
                Stream.of(
                        new TrainingSample<>(predicate, labels, source),
                        new TrainingSample<>(predicate, labels),
                        new TrainingSample<>(predicate, labels, source));
        TrainingData<BPredicate, PredDbEntry> trainingData =
                new TrainingData<>(source, sampleStream);

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

        StringWriter writer = new StringWriter();
        format.writeSamples(trainingData, writer);

        String expected = "{\"" + BEVERAGE_VENDING_MACHINE.toString() + "\":{"
                          + "\"sha512\":\"eea0db1101cc2928c3eb62d3b3409fd5456faca283ac61226789843619f757e1e5214d22811f66cab42f2949e706417b5f844b2fbc952d08073b8137e38c98ee\","
                          + "\"formalism\":\"CLASSICALB\","
                          + "\"gathered-predicates\":["
                          + SINGLE_PRED_ENTRY + ","
                          + SINGLE_PRED_ENTRY + ","
                          + SINGLE_PRED_ENTRY + "]}}";
        String actual = writer.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldHashFile() throws IOException {
        Path mchFile = Paths.get(
                this.getClass().getClassLoader()
                        .getResource("db/mch/bvm.mch").getFile()
        );

        JsonDbFormat format = new JsonDbFormat();

        String expected = "eea0db1101cc2928c3eb62d3b3409fd5456faca283ac61226789843619f757e1e5214d22811f66cab42f2949e706417b5f844b2fbc952d08073b8137e38c98ee";
        String actual = format.getMachineHash(mchFile);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldHashFileErrorWhenNonExistent() throws IOException {
        Path mchFile = Paths.get("non/existent.mch");

        JsonDbFormat format = new JsonDbFormat();

        String expected = "Hash error: File not found";
        String actual = format.getMachineHash(mchFile);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldHashFileAsNullWhenNullReference() throws IOException {
        JsonDbFormat format = new JsonDbFormat();

        String expected = "Hash error: File was null";
        String actual = format.getMachineHash(null);

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

        JsonDbFormat format = new JsonDbFormat(BACKENDS_USED);

        StringWriter writer = new StringWriter();
        DataGenerationStats stats = format.writeSamples(trainingData, writer);

        int expected = 3;
        int actual = stats.getSamplesWritten();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldBeValidJson() {
        String json = getPredicateJson("pred", "hashihash");
        JsonDbFormat format = new JsonDbFormat();
        JsonReader reader = new JsonReader(new StringReader(json));
        assertTrue(format.isValidJsonDb(reader));
    }

    @Test
    public void shouldNotBeValidJson() {
        String json = getPredicateJson("pred", "hashihash").substring(0, 16);
        JsonDbFormat format = new JsonDbFormat();
        JsonReader reader = new JsonReader(new StringReader(json));
        assertFalse(format.isValidJsonDb(reader));
    }

    @Test
    public void shouldNotBeValidJsonDbWhenNoObject() {
        String json = "\"test\"";
        JsonDbFormat format = new JsonDbFormat();
        JsonReader reader = new JsonReader(new StringReader(json));
        assertFalse(format.isValidJsonDb(reader));
    }

    private String getPredicateJson(String pred, String hash) {
        String json =
                "{"
                + "\"predicate\":\"" + pred + "\","
                + "\"sha512\":\"" + hash + "\","
                + "\"probcli\":{\"version\":\"0.1.2-neurob\",\"revision\":\"revision-hash\"},"
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
        PredDbEntry labels = getLabelling("pred", source, Answer.VALID, Answer.VALID, Answer.VALID, Answer.UNKNOWN);

        return new TrainingSample<>(pred, labels, source);
    }

}
