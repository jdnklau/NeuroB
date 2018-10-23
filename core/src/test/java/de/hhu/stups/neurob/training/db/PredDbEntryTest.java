package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PredDbEntryTest {

    @Test
    void shouldTranslateResultMapToOrderedLabellingArrayContainingTimings() {
        Backend[] backendOrder = {new ProBBackend(), new KodkodBackend(), new Z3Backend()};

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[1], new TimedAnswer(Answer.INVALID, 200L));
        resultMap.put(backendOrder[2], new TimedAnswer(Answer.UNKNOWN, 300L));

        Double[] expected = {100., 200., 300.};
        Double[] actual = PredDbEntry.toArray(resultMap, backendOrder);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldTranslateMissingEntriesOfResultMapToNull() {
        Backend[] backendOrder = {new ProBBackend(), new KodkodBackend(), new Z3Backend()};

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[2], new TimedAnswer(Answer.UNKNOWN, 300L));

        Double[] expected = {100., null, 300.};
        Double[] actual = PredDbEntry.toArray(resultMap, backendOrder);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldConstructLabellingArrayWhenUsingConstructorWithMapAndArray() {
        BPredicate pred = BPredicate.of("pred");

        Backend[] backendOrder = {
                new ProBBackend(),
                new KodkodBackend(),
                new Z3Backend(),
                new SmtBackend()
        };

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[1], new TimedAnswer(Answer.INVALID, 200L));
        resultMap.put(backendOrder[2], new TimedAnswer(Answer.UNKNOWN, 300L));

        PredDbEntry dbEntry = new PredDbEntry(pred, null, resultMap);

        Double[] expected = {100., 200., 300., null};
        Double[] actual = dbEntry.getLabellingArray();

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldTranslateIntoResultMap() {
        Backend[] backendOrder = {
                new ProBBackend(),
                new KodkodBackend(),
                new Z3Backend(),
                new SmtBackend()
        };

        TimedAnswer[] answers = {
                new TimedAnswer(Answer.VALID, 100L),
                new TimedAnswer(Answer.INVALID, 200L),
                new TimedAnswer(Answer.UNKNOWN, 300L),
                new TimedAnswer(Answer.ERROR, 400L)
        };

        Map<Backend, TimedAnswer> expected =new HashMap<>();
        expected.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        expected.put(backendOrder[1], new TimedAnswer(Answer.INVALID, 200L));
        expected.put(backendOrder[2], new TimedAnswer(Answer.UNKNOWN, 300L));
        expected.put(backendOrder[3], new TimedAnswer(Answer.ERROR, 400L));

        Map<Backend, TimedAnswer> actual = PredDbEntry.toMap(backendOrder, answers);

        assertEquals(expected, actual);
    }

    @Test
    void shouldConstructResultArrayWhenUsingConstructorWithArrayAndAnswers() {
        BPredicate pred = BPredicate.of("pred");

        Backend[] backendOrder = {
                new ProBBackend(),
                new KodkodBackend(),
                new Z3Backend(),
                new SmtBackend()
        };

        TimedAnswer[] answers = {
                new TimedAnswer(Answer.VALID, 100L),
                new TimedAnswer(Answer.INVALID, 200L),
                new TimedAnswer(Answer.UNKNOWN, 300L),
                new TimedAnswer(Answer.ERROR, 400L)
        };

        PredDbEntry dbEntry = new PredDbEntry(pred, null, backendOrder, answers);

        Double[] expected = {100., 200., 300., 400.};
        Double[] actual = dbEntry.getLabellingArray();

        assertArrayEquals(expected, actual);

    }


}
