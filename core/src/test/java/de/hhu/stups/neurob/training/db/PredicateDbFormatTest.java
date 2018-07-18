package de.hhu.stups.neurob.training.db;

import com.google.gson.stream.JsonReader;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PredicateDbFormatTest {

    @Test
    public void shouldGiveJsonPathWhenMch() {
        PredicateDbFormat dbFormat = new PredicateDbFormat();

        Path source = Paths.get("non/existent/source.mch");
        Path targetDir = Paths.get("target/dir");
        Path expected = Paths.get("target/dir/non/existent/source.json");

        assertEquals(expected, dbFormat.getTargetLocation(source, targetDir));
    }

    @Test
    public void shouldLoadSamples() throws IOException {
        String fileUrl = PredicateDbFormatTest.class.getClassLoader()
                .getResource("db/predicates/example.json").getFile();
        Path dbFile = Paths.get(fileUrl);

        // Expecting three samples
        Labelling labelling = new Labelling(2., 1., -1., 4.);
        DbSample sample1 = new DbSample<>(
                new BPredicate("pred1"), labelling,
                Paths.get("non/existent.mch"));
        DbSample sample2 = new DbSample<>(
                new BPredicate("pred1"), labelling,
                Paths.get("non/existent.mch"));
        DbSample sample3 = new DbSample<>(
                new BPredicate("pred1"), labelling,
                Paths.get("other/non/existent.mch"));

        List<DbSample> expected = new ArrayList<>();
        expected.add(sample1);
        expected.add(sample1);
        expected.add(sample3);

        List<DbSample> actual = new PredicateDbFormat().loadSamples(dbFile)
                .collect(Collectors.toList());

        assertEquals(expected, actual,
                "Loaded entries do not match");
    }

    @Test
    public void shouldHaveNoNextEntriesWhenInitialisedWithEmptyData() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldHaveNextEntries() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        assertTrue(iterator.hasNext());
    }

    @Test
    public void shouldHaveOnlyOneEntry() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        iterator.next();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldHaveMoreThanOneEntry() throws IOException {
        String json = getSampleJson(2);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        iterator.next();
        assertTrue(iterator.hasNext());
    }

    @Test
    public void shouldReadSample() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        DbSample<BPredicate> expected = getSample();
        DbSample<BPredicate> actual = iterator.next();

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

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);


        assertThrows(IllegalStateException.class,
                iterator::nextSampleData);
    }

    @Test
    public void shouldReadJsonAsMap() throws IOException {
        String json = getSampleJson(1);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        Map<String, String> expected = new HashMap<>();
        expected.put("predicate", "pred");
        expected.put("source", "non/existent.mch");
        expected.put("timings",
                "KodkodBackend:3.0,"
                + "ProBBackend:1.0,"
                + "SmtBackend:-1.0,"
                + "Z3Backend:2.0");

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
                      + "\"Z3Backend\":2.0,"
                      + "\"KodkodBackend\":3.0,"
                      + "\"SmtBackend\":-1"
                      + "}}";
        json += "]}";

        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);


        Map<String, String> expected = new HashMap<>();
        expected.put("predicate", "pred");
        expected.put("timings",
                "KodkodBackend:3.0,"
                + "ProBBackend:1.0,"
                + "SmtBackend:-1.0,"
                + "Z3Backend:2.0");

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

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        assertThrows(IllegalStateException.class,
                iterator::nextSampleData);
    }

    @Test
    public void shouldTranslateMapToDbEntry() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        Map<String, String> data = new HashMap<>();
        data.put("predicate", "pred");
        data.put("timings",
                "KodkodBackend:3.0,"
                + "ProBBackend:1.0,"
                + "SmtBackend:-1.0,"
                + "Z3Backend:2.0");

        DbSample<BPredicate> expected = getSample(null);
        DbSample<BPredicate> actual = iterator.translateToDbSample(data);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldTranslateTwoTimings() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        String timings = "solver1:1.0,solver2:2.0";
        Labelling expected = new Labelling(1., 2.);
        Labelling actual = iterator.translateTimings(timings);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldTranslateThreeTimings() throws IOException {
        String json = getSampleJson(0);
        JsonReader reader = new JsonReader(new StringReader(json));

        PredicateDbFormat.PredicateDbIterator iterator =
                new PredicateDbFormat.PredicateDbIterator(reader);

        String timings = "solver1:1.0,solver2:2.0,solver3:-1.0";
        Labelling expected = new Labelling(1., 2., -1.);
        Labelling actual = iterator.translateTimings(timings);

        assertEquals(expected, actual);
    }

    @Test
    @Ignore("Functionality not yet implemented")
    public void shouldWriteSample() throws IOException {
        PredicateFeatures features = new PredicateFeatures("pred");
        Labelling labels = new Labelling(3., 1., -1., 2.);
        Path source = Paths.get("non/existent.mch");

        TrainingSample<PredicateFeatures, Labelling> sample =
                new TrainingSample<>(features, labels, source);

        fail();
    }

    private String getSampleJson(int numEntries) {
        String json = "{\"samples\":[";
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < numEntries; i++) {
            entries.add(
                    "{"
                    + "\"predicate\":\"pred\","
                    + "\"source\":\"non/existent.mch\","
                    + "\"timings\":{"
                    + "\"ProBBackend\":1.0,"
                    + "\"Z3Backend\":2.0,"
                    + "\"KodkodBackend\":3.0,"
                    + "\"SmtBackend\":-1"
                    + "}}");
        }
        json += String.join(",", entries);
        json += "]}";

        return json;
    }

    private DbSample<BPredicate> getSample() {
        Path source = Paths.get("non/existent.mch");
        return getSample(source);
    }

    private DbSample<BPredicate> getSample(Path source) {
        BPredicate pred = new BPredicate("pred");
        Labelling labelling = new Labelling(3., 1., -1., 2.);

        return new DbSample<>(pred, labelling, source);
    }
}