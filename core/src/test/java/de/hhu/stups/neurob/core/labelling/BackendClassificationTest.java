package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackendClassificationTest {

    private Backend[] backends = new Backend[4];

    @BeforeEach
    void setupBackends() {
        backends[0] = mock(ProBBackend.class);
        backends[1] = mock(KodkodBackend.class);
        backends[2] = mock(Z3Backend.class);
        backends[3] = mock(SmtBackend.class);
    }

    @Test
    void shouldAssociate0WithNull() {
        assertNull(BackendClassification.getBackendFromClassIndex(backends, 0));
    }

    @Test
    void shouldAssociate1WithProB() {
        assertEquals(backends[0], BackendClassification.getBackendFromClassIndex(backends, 1));
    }

    @Test
    void shouldAssociate1WithKodkod() {
        assertEquals(backends[1], BackendClassification.getBackendFromClassIndex(backends, 2));
    }

    @Test
    void shouldAssociate1WithZ3() {
        assertEquals(backends[2], BackendClassification.getBackendFromClassIndex(backends, 3));
    }

    @Test
    void shouldAssociate1WithSmt() {
        assertEquals(backends[3], BackendClassification.getBackendFromClassIndex(backends, 4));
    }

    @Test
    void shouldThrowExceptionWhenIndexIsOutOfBounds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BackendClassification.getBackendFromClassIndex(backends, 200000));
    }

    @Test
    void shouldFindIndex0() {
        assertEquals(0, BackendClassification.getIndexByBackend(null, backends));
    }

    @Test
    void shouldFindIndex3() {
        assertEquals(3, BackendClassification.getIndexByBackend(backends[2], backends));
    }

    @Test
    void shouldClassifyAsNoAnswerByAnyBackend() throws FormulaException, LabelCreationException {
        // setup backends
        TimedAnswer timeout = new TimedAnswer(Answer.TIMEOUT, 200L);
        for (Backend b : backends) {
            setAnswer(b, timeout);
        }

        PredicateLabelGenerating backendClassifier = new BackendClassification.Generator(backends);

        PredicateLabelling expected = new PredicateLabelling("pred", 0.0);
        PredicateLabelling actual = backendClassifier.generate("pred");

        assertEquals(expected, actual);
    }

    @Test
    void shouldClassifyAsClassThree() throws FormulaException, LabelCreationException {
        // setup backends
        TimedAnswer unknown = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer valid = new TimedAnswer(Answer.VALID, 400L);
        TimedAnswer slowValid = new TimedAnswer(Answer.VALID, 800L);

        setAnswer(backends[0], unknown);
        setAnswer(backends[1], slowValid);
        setAnswer(backends[2], valid);
        setAnswer(backends[3], slowValid);

        PredicateLabelGenerating backendClassifier = new BackendClassification.Generator(backends);

        PredicateLabelling expected = new PredicateLabelling("pred", 3.0);
        PredicateLabelling actual = backendClassifier.generate("pred");

        assertEquals(expected, actual);
    }

    @Test
    void shouldFindFastest() throws FormulaException {
        TimedAnswer valid = new TimedAnswer(Answer.VALID, 400L);
        TimedAnswer slowValid = new TimedAnswer(Answer.VALID, 800L);

        Map<Backend, TimedAnswer> answerMap = new HashMap<>();
        answerMap.put(backends[0], slowValid);
        answerMap.put(backends[1], slowValid);
        answerMap.put(backends[2], valid);
        answerMap.put(backends[3], slowValid);

        Backend expected = backends[2];
        Backend actual = BackendClassification.classifyFastestBackend(backends, answerMap);

        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnNullIfNoBackendCanFindAnser() throws FormulaException {
        TimedAnswer timeout = new TimedAnswer(Answer.TIMEOUT, 400L);

        Map<Backend, TimedAnswer> answerMap = new HashMap<>();
        answerMap.put(backends[0], timeout);
        answerMap.put(backends[1], timeout);
        answerMap.put(backends[2], timeout);
        answerMap.put(backends[3], timeout);

        Backend actual = BackendClassification.classifyFastestBackend(backends, answerMap);

        assertNull(actual);
    }

    private void setAnswer(Backend b, TimedAnswer answer) throws FormulaException {
        when(b.solvePredicate(any(), any())).thenReturn(answer);
        when(b.solvePredicate(any(), any(), any(), any())).thenReturn(answer);
    }

}
