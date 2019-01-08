package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredDbEntryTest {

    private final Backend[] BACKENDS_USED = {
            new ProBBackend(),
            new KodkodBackend(),
            new Z3Backend(),
            new SmtBackend(),
    };

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

        PredDbEntry dbEntry = new PredDbEntry(pred, null, BACKENDS_USED, resultMap);

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

        Map<Backend, TimedAnswer> expected = new HashMap<>();
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

    @Test
    void shouldAnswerWithAverageTimeOfAllRuns() throws FormulaException, LabelCreationException {
        Backend backend = mock(Backend.class);
        when(backend.solvePredicate(any(), any(), any(), any()))
                .thenReturn(new TimedAnswer(Answer.VALID, 10L))
                .thenReturn(new TimedAnswer(Answer.VALID, 20L))
                .thenReturn(new TimedAnswer(Answer.VALID, 30L))
                .thenReturn(new TimedAnswer(Answer.VALID, 80L));

        PredDbEntry.Generator generator =
                new PredDbEntry.Generator(4, null, null, null);

        TimedAnswer expected = new TimedAnswer(Answer.VALID, 35L);
        TimedAnswer actual = generator.samplePredicate(null, backend, null);

        assertEquals(expected, actual);
    }

    @Test
    void shouldGenerateSamplesForEachBackend() throws FormulaException, LabelCreationException {
        // Set up two backends to call
        Backend back1 = mock(Backend.class);
        when(back1.solvePredicate(any(), any(), any(), any()))
                .thenReturn(new TimedAnswer(Answer.VALID, 10L))
                .thenReturn(new TimedAnswer(Answer.VALID, 20L));
        Backend back2 = mock(Backend.class);
        when(back2.solvePredicate(any(), any(), any(), any()))
                .thenReturn(new TimedAnswer(Answer.INVALID, 30L))
                .thenReturn(new TimedAnswer(Answer.INVALID, 40L));
        Backend[] backends = {back1, back2};

        PredDbEntry.Generator generator =
                new PredDbEntry.Generator(2, null, null, null, backends);

        PredDbEntry expected = new PredDbEntry(BPredicate.of("pred"),
                null, backends,
                new TimedAnswer(Answer.VALID, 15L),
                new TimedAnswer(Answer.INVALID, 35L));
        PredDbEntry actual = generator.generate(BPredicate.of("pred"), (BMachine) null);

        assertEquals(expected, actual);
    }

    @Test
    void shouldGenerateNullSamplesWhenBackendThrowsException() throws FormulaException, LabelCreationException {
        // Set up two backends to call
        Backend back1 = mock(Backend.class);
        when(back1.solvePredicate(any(), any(), any(), any()))
                .thenReturn(new TimedAnswer(Answer.VALID, 10L))
                .thenReturn(new TimedAnswer(Answer.VALID, 20L));
        Backend back2 = mock(Backend.class);
        when(back2.solvePredicate(any(), any(), any(), any()))
                .thenThrow(new FormulaException("This exception is hard coded for a unit test"));
        Backend[] backends = {back1, back2};

        PredDbEntry.Generator generator =
                new PredDbEntry.Generator(2, null, null, null, backends);

        PredDbEntry expected = new PredDbEntry(BPredicate.of("pred"),
                null, backends,
                new TimedAnswer(Answer.VALID, 15L),
                null);
        PredDbEntry actual = generator.generate(BPredicate.of("pred"), (BMachine) null);

        assertEquals(expected, actual);
    }

    @Test
    void shouldCollectResultMapToArray() {
        Backend[] backendOrder = {new ProBBackend(), new KodkodBackend(), new Z3Backend()};

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[1], new TimedAnswer(Answer.INVALID, 200L));
        resultMap.put(backendOrder[2], new TimedAnswer(Answer.UNKNOWN, 300L));

        TimedAnswer[] expected = {
                new TimedAnswer(Answer.VALID, 100L),
                new TimedAnswer(Answer.INVALID, 200L),
                new TimedAnswer(Answer.UNKNOWN, 300L)
        };
        TimedAnswer[] actual = new PredDbEntry(null, null, BACKENDS_USED, resultMap)
                .getAnswerArray(backendOrder);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldCollectResultMapToArrayWithNullEntriesIfSomeDataIsMissing() {
        Backend[] backendOrder = {new ProBBackend(), new KodkodBackend(), new Z3Backend()};

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[2], new TimedAnswer(Answer.UNKNOWN, 300L));

        TimedAnswer[] expected = {
                new TimedAnswer(Answer.VALID, 100L),
                null,
                new TimedAnswer(Answer.UNKNOWN, 300L)
        };
        TimedAnswer[] actual = new PredDbEntry(null, null, BACKENDS_USED, resultMap)
                .getAnswerArray(backendOrder);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldReturnArrayWithNullEntriesWhenDataIsPresentButHasNullTimings() {
        Backend[] backendOrder = {new ProBBackend(), new KodkodBackend()};

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[1], new TimedAnswer(Answer.UNKNOWN, null));

        Double[] expected = {100., null};
        Double[] actual = PredDbEntry.toArray(resultMap, backendOrder);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldCollectSubsetOfDataWhenSubsetOfBackendsIsSpecified() {
        Backend[] backendOrder = {new ProBBackend(), new KodkodBackend(), new Z3Backend()};

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[1], new TimedAnswer(Answer.INVALID, 200L));
        resultMap.put(backendOrder[2], new TimedAnswer(Answer.UNKNOWN, 300L));

        TimedAnswer[] expected = {
                new TimedAnswer(Answer.VALID, 100L),
                new TimedAnswer(Answer.UNKNOWN, 300L)
        };
        TimedAnswer[] actual = new PredDbEntry(null, null, BACKENDS_USED, resultMap)
                .getAnswerArray(new ProBBackend(), new Z3Backend());

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldReturnProBTimedAnswer() {
        Backend[] backendOrder = {new ProBBackend(), new KodkodBackend()};

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[1], new TimedAnswer(Answer.UNKNOWN, 300L));

        PredDbEntry entry = new PredDbEntry(null, null, BACKENDS_USED, resultMap);

        TimedAnswer expected = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer actual = entry.getResult(new ProBBackend());

        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnNullWhenNoAnswerIsStoredForRequestedBackend() {
        Backend[] backendOrder = {new ProBBackend(), new KodkodBackend()};

        Map<Backend, TimedAnswer> resultMap = new HashMap<>();
        resultMap.put(backendOrder[0], new TimedAnswer(Answer.VALID, 100L));
        resultMap.put(backendOrder[1], new TimedAnswer(Answer.UNKNOWN, 300L));

        PredDbEntry entry = new PredDbEntry(null, null, BACKENDS_USED, resultMap);

        assertNull(entry.getResult(new Z3Backend()));
    }

}
