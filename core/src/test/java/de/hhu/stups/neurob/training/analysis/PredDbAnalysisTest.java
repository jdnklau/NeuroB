package de.hhu.stups.neurob.training.analysis;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PredDbAnalysisTest {
    private final TimedAnswer valid = new TimedAnswer(Answer.VALID, 200L);
    private final TimedAnswer invalid = new TimedAnswer(Answer.INVALID, 200L);
    private final TimedAnswer solvable = new TimedAnswer(Answer.SOLVABLE, 200L);
    private final TimedAnswer unknown = new TimedAnswer(Answer.UNKNOWN, 200L);
    private final TimedAnswer timeout = new TimedAnswer(Answer.TIMEOUT, 200L);
    private final TimedAnswer error = new TimedAnswer(Answer.ERROR, 200L);

    @Test
    void shouldCountPredicates() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry entry = new PredDbEntry(null, null, backends, valid, valid);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry).add(null, entry);

        assertEquals(new Long(2), analysis.getPredCount());
    }

    @Test
    void shouldCountTimeoutWhenTimeoutAndError() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry answers = new PredDbEntry(null, null, backends, error, timeout);

        analysis.countBestAnswer(null, answers);

        assertEquals(new Long(1), analysis.getPredCount(Answer.TIMEOUT));
    }

    @Test
    void shouldCountUnknownWhenTimeoutAndUnknown() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry answers = new PredDbEntry(null, null, backends, unknown, timeout);

        analysis.countBestAnswer(null, answers);

        assertEquals(new Long(1), analysis.getPredCount(Answer.UNKNOWN));
    }

    @Test
    void shouldCountSolvableWhenSolvableAndUnknown() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry answers = new PredDbEntry(null, null, backends, unknown, solvable);

        analysis.countBestAnswer(null, answers);

        assertEquals(new Long(1), analysis.getPredCount(Answer.SOLVABLE));
    }

    @Test
    void shouldCountValidWhenSolvableAndValid() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry answers = new PredDbEntry(null, null, backends, valid, solvable);

        analysis.countBestAnswer(null, answers);

        assertEquals(new Long(1), analysis.getPredCount(Answer.VALID));
    }

    @Test
    void shouldCountInvalidWhenSolvableAndInvalid() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry answers = new PredDbEntry(null, null, backends, invalid, solvable);

        analysis.countBestAnswer(null, answers);

        assertEquals(new Long(1), analysis.getPredCount(Answer.INVALID));
    }

    @Test
    void shouldCountValidAndInvalidAsSolvable() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry answersValid = new PredDbEntry(null, null, backends, valid, valid);
        PredDbEntry answersInvalid = new PredDbEntry(null, null, backends, invalid, invalid);

        analysis.countBestAnswer(null, answersValid);
        analysis.countBestAnswer(null, answersInvalid);

        assertAll(
                () -> assertEquals(new Long(1), analysis.getPredCount(Answer.VALID),
                        "Valid count does not match"),
                () -> assertEquals(new Long(1), analysis.getPredCount(Answer.INVALID),
                        "Invalid count does not match"),
                () -> assertEquals(new Long(2), analysis.getPredCount(Answer.SOLVABLE),
                        "Solvable count does not match")
        );
    }

    @Test
    void shouldCountContradictions() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry contradiction = new PredDbEntry(null, null, backends, valid, invalid);

        analysis.countBestAnswer(null, contradiction);

        assertEquals(new Long(1), analysis.getContradictionCount());
    }

    @Test
    void shouldNotCountContradictionsAsValidNorInvalid() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry contradiction = new PredDbEntry(null, null, backends, valid, invalid);

        analysis.countBestAnswer(null, contradiction);

        assertAll(
                () -> assertEquals(new Long(0), analysis.getPredCount(Answer.VALID),
                        "Valid count does not match"),
                () -> assertEquals(new Long(0), analysis.getPredCount(Answer.INVALID),
                        "Invalid count does not match")
        );
    }

    @Test
    void shouldNotCountContradictionWhenOnePredicateIsValidAndAnotherIsInvalid() {
        PredDbAnalysis analysis = new PredDbAnalysis();

        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry answersValid = new PredDbEntry(null, null, backends, valid, valid);
        PredDbEntry answersInvalid = new PredDbEntry(null, null, backends, invalid, invalid);

        analysis.countBestAnswer(null, answersValid);
        analysis.countBestAnswer(null, answersInvalid);

        assertEquals(new Long(0), analysis.getContradictionCount());
    }

    @Test
    void shouldCountValidAnswersForEachBackend() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry entry = new PredDbEntry(null, null, backends, valid, valid);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry);

        assertAll(
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.VALID, backends[0])),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.VALID, backends[1]))
        );
    }

    @Test
    void shouldCountValidAndInvalidPredicatesAsSolvable() {
        Backend[] backends = {new ProBBackend()};
        PredDbEntry entryValid = new PredDbEntry(null, null, backends, valid);
        PredDbEntry entryInvalid = new PredDbEntry(null, null, backends, invalid);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entryValid).add(null, entryInvalid);

        assertAll(
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.VALID, backends[0]),
                        "Valid count does not match"),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.INVALID, backends[0]),
                        "Invalid count does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.SOLVABLE, backends[0]),
                        "Solvable count does not match")
        );
    }

    @Test
    void shouldClusterProBWithKodkodAndZ3Separately() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend(), new Z3Backend()};
        PredDbEntry entry = new PredDbEntry(null, null, backends, error, error, unknown);

        Set<Backend> cluster1 = new HashSet<>();
        cluster1.add(backends[0]);
        cluster1.add(backends[1]);
        Set<Backend> cluster2 = new HashSet<>();
        cluster2.add(backends[2]);

        Map<Answer, Set<Backend>> expected = new HashMap<>();
        expected.put(Answer.ERROR, cluster1);
        expected.put(Answer.UNKNOWN, cluster2);
        Map<Answer, Set<Backend>> actual = PredDbAnalysis.clusterBackendsByAnswer(entry);

        assertEquals(expected, actual);
    }

    @Test
    void shouldClusterValidAndInvalidAsSolvable() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry entry = new PredDbEntry(null, null, backends, valid, invalid);

        Set<Backend> clusterValid = new HashSet<>();
        clusterValid.add(backends[0]);
        Set<Backend> clusterInvalid = new HashSet<>();
        clusterInvalid.add(backends[1]);
        Set<Backend> clusterSolvable = new HashSet<>();
        clusterSolvable.add(backends[0]);
        clusterSolvable.add(backends[1]);

        Map<Answer, Set<Backend>> expected = new HashMap<>();
        expected.put(Answer.VALID, clusterValid);
        expected.put(Answer.INVALID, clusterInvalid);
        expected.put(Answer.SOLVABLE, clusterSolvable);
        Map<Answer, Set<Backend>> actual = PredDbAnalysis.clusterBackendsByAnswer(entry);

        assertEquals(expected, actual);
    }

    @Test
    void shouldCountSameAnswerForTwoBackendsForTheirCombination() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        PredDbEntry entry = new PredDbEntry(null, null, backends, valid, valid);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry);

        assertEquals(new Long(1), analysis.getAnswerCount(Answer.VALID, backends));
    }

    @Test
    void shouldCountAnswersForMatchingBackends() {
        ProBBackend prob = new ProBBackend();
        KodkodBackend kodkod = new KodkodBackend();
        Z3Backend z3 = new Z3Backend();
        Backend[] backends = {prob, kodkod, z3};
        PredDbEntry entry1 = new PredDbEntry(null, null, backends, valid, valid, solvable);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, unknown, error, unknown);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, unknown, invalid, invalid);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertAll(
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.VALID, prob, kodkod, z3),
                        "Valids of ProB, Kodkod, Z3 is incorrect."),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.VALID, prob, kodkod),
                        "Valids of ProB, Kodkod is incorrect."),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.SOLVABLE, prob, kodkod, z3),
                        "Solvables of ProB, Kodkod, Z3 is incorrect."),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.SOLVABLE, kodkod, z3),
                        "Solvables of Kodkod, Z3 is incorrect."),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.UNKNOWN, prob),
                        "Unknowns of ProB is incorrect."),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.UNKNOWN, prob, z3),
                        "Unknowns of ProB, Z3 is incorrect.")
        );
    }

    @Test
    void shouldDetectBackendsSeen() {
        ProBBackend prob = new ProBBackend();
        KodkodBackend kodkod = new KodkodBackend();
        Z3Backend z3 = new Z3Backend();
        Backend[] backends = {prob, kodkod, z3};
        PredDbEntry entry1 = new PredDbEntry(null, null, backends, valid, valid, solvable);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, unknown, error, unknown);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, unknown, invalid, invalid);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        Set<Backend> expected = new HashSet<>();
        expected.add(prob);
        expected.add(kodkod);
        expected.add(z3);

        Set<Backend> actual = analysis.getBackendsSeen();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateAverageRuntimePerAnswer() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);

        assertEquals(new Double(150), analysis.getAverageRuntime(Answer.VALID, backends[0]));
    }

    @Test
    void shouldCountValidAndInvalidAsSolvableForRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.INVALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.SOLVABLE, 600L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Double(300), analysis.getAverageRuntime(Answer.SOLVABLE, backends[0]));
    }

    @Test
    void shouldCalculateAverageRuntimeForEachBackend() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);

        assertAll(
                () -> assertEquals(new Double(200), analysis.getAverageRuntime(Answer.VALID, backends[0]),
                        "Average for ProB does not match."),
                () -> assertEquals(new Double(300), analysis.getAverageRuntime(Answer.VALID, backends[1]),
                        "Average for Kodkod does not match.")
        );
    }

    @Test
    void shouldCalculateMinimumRuntimeForEachBackend() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);

        assertAll(
                () -> assertEquals(new Long(100), analysis.getMinimumRuntime(Answer.VALID, backends[0]),
                        "Minimum for ProB does not match."),
                () -> assertEquals(new Long(200), analysis.getMinimumRuntime(Answer.VALID, backends[1]),
                        "Minimum for Kodkod does not match.")
        );
    }

    @Test
    void shouldCalculateMaximumRuntimeForEachBackend() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);

        assertAll(
                () -> assertEquals(new Long(300), analysis.getMaximumRuntime(Answer.VALID, backends[0]),
                        "Maximum for ProB does not match."),
                () -> assertEquals(new Long(400), analysis.getMaximumRuntime(Answer.VALID, backends[1]),
                        "Maximum for Kodkod does not match.")
        );
    }

    @Test
    void shouldCalculateMedianRuntimeForEachBackend() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);
        TimedAnswer t5 = new TimedAnswer(Answer.VALID, 500L);
        TimedAnswer t6 = new TimedAnswer(Answer.VALID, 600L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t5, t6);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertAll(
                () -> assertEquals(new Double(300), analysis.getMedianRuntime(Answer.VALID, backends[0]),
                        "Median for ProB does not match."),
                () -> assertEquals(new Double(400), analysis.getMedianRuntime(Answer.VALID, backends[1]),
                        "Median for Kodkod does not match.")
        );
    }

    @Test
    void shouldCalculateFirstQuartileRuntimeForEachBackend() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);
        TimedAnswer t5 = new TimedAnswer(Answer.VALID, 500L);
        TimedAnswer t6 = new TimedAnswer(Answer.VALID, 600L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t5, t6);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertAll(
                () -> assertEquals(new Double(200), analysis.getFirstQuartileRuntime(Answer.VALID, backends[0]),
                        "FirstQuartile for ProB does not match."),
                () -> assertEquals(new Double(300), analysis.getFirstQuartileRuntime(Answer.VALID, backends[1]),
                        "FirstQuartile for Kodkod does not match.")
        );
    }

    @Test
    void shouldCalculateThirdQuartileRuntimeForEachBackend() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);
        TimedAnswer t5 = new TimedAnswer(Answer.VALID, 500L);
        TimedAnswer t6 = new TimedAnswer(Answer.VALID, 600L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t5, t6);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertAll(
                () -> assertEquals(new Double(400), analysis.getThirdQuartileRuntime(Answer.VALID, backends[0]),
                        "ThirdQuartile for ProB does not match."),
                () -> assertEquals(new Double(500), analysis.getThirdQuartileRuntime(Answer.VALID, backends[1]),
                        "ThirdQuartile for Kodkod does not match.")
        );
    }

    @Test
    void shouldCalculateAverageOfSolvableAndUnknownAsAnsweredInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.SOLVABLE, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);

        assertEquals(new Double(150), analysis.getAverageAnsweredRuntime(backends[0]));
    }

    @Test
    void shouldCalculateMedianOfSolvableAndUnknownAsAnsweredInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.SOLVABLE, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Double(200), analysis.getMedianAnsweredRuntime(backends[0]));
    }

    @Test
    void shouldCalculateFirstQuartileOfSolvableAndUnknownAsAnsweredInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.SOLVABLE, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Double(150), analysis.getFirstQuartileAnsweredRuntime(backends[0]));
    }

    @Test
    void shouldCalculateThirdQuartileOfSolvableAndUnknownAsAnsweredInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.SOLVABLE, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Double(250), analysis.getThirdQuartileAnsweredRuntime(backends[0]));
    }

    @Test
    void shouldCalculateMinimumOfSolvableAndUnknownAsAnsweredInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.SOLVABLE, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Long(100), analysis.getMinimumAnsweredRuntime(backends[0]));
    }

    @Test
    void shouldCalculateMaximumOfSolvableAndUnknownAsAnsweredInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.SOLVABLE, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Long(300), analysis.getMaximumAnsweredRuntime(backends[0]));
    }

    @Test
    void shouldCalculateAverageOfAnsweredAndErrorAsTimedInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.ERROR, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);

        assertEquals(new Double(150), analysis.getAverageTimedRuntime(backends[0]));
    }

    @Test
    void shouldCalculateMedianOfAnsweredAndErrorAsTimedInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.ERROR, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Double(200), analysis.getMedianTimedRuntime(backends[0]));
    }

    @Test
    void shouldCalculateFirstQuartileOfAnsweredAndErrorAsTimedInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.ERROR, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Double(150), analysis.getFirstQuartileTimedRuntime(backends[0]));
    }

    @Test
    void shouldCalculateThirdQuartileOfAnsweredAndErrorAsTimedInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.ERROR, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Double(250), analysis.getThirdQuartileTimedRuntime(backends[0]));
    }

    @Test
    void shouldCalculateMinimumOfAnsweredAndErrorAsTimedInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.ERROR, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Long(100), analysis.getMinimumTimedRuntime(backends[0]));
    }

    @Test
    void shouldCalculateMaximumOfAnsweredAndErrorAsTimedInRegression() {
        Backend[] backends = {new ProBBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.ERROR, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.UNKNOWN, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t3);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);

        assertEquals(new Long(300), analysis.getMaximumTimedRuntime(backends[0]));
    }

    @Test
    void shouldCountFastestAnswersPerBackend() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};

        TimedAnswer prob1 = new TimedAnswer(Answer.VALID, 100L); // fastest
        TimedAnswer prob2 = new TimedAnswer(Answer.INVALID, 100L); // fastest
        TimedAnswer prob3 = new TimedAnswer(Answer.SOLVABLE, 100L);
        TimedAnswer prob4 = new TimedAnswer(Answer.UNKNOWN, 100L); // fastest

        TimedAnswer kodkod1 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer kodkod2 = new TimedAnswer(Answer.INVALID, 50L); // fastest
        TimedAnswer kodkod3 = new TimedAnswer(Answer.SOLVABLE, 200L);
        TimedAnswer kodkod4 = new TimedAnswer(Answer.UNKNOWN, 200L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, prob1, kodkod1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, prob2, kodkod2);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, prob3, kodkod3);
        PredDbEntry entry4 = new PredDbEntry(null, null, backends, prob4, kodkod4);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);
        analysis.add(null, entry3);
        analysis.add(null, entry4);

        assertAll(
                () -> assertEquals(new Long(3), analysis.getFastestAnswerCount(backends[0]),
                        "Fastest count for ProB does not match."),
                () -> assertEquals(new Long(1), analysis.getFastestAnswerCount(backends[1]),
                        "Fastest count for Kodkod does not match.")
        );
    }

    @Test
    void shouldSkipErrorsAndTimeOutsWhenCountingFastest() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};

        TimedAnswer prob1 = new TimedAnswer(Answer.ERROR, 100L);
        TimedAnswer prob2 = new TimedAnswer(Answer.TIMEOUT, 100L);

        TimedAnswer kodkod1 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer kodkod2 = new TimedAnswer(Answer.INVALID, 200L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, prob1, kodkod1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, prob2, kodkod2);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);

        assertAll(
                () -> assertEquals(new Long(0), analysis.getFastestAnswerCount(backends[0]),
                        "Fastest count for ProB does not match."),
                () -> assertEquals(new Long(2), analysis.getFastestAnswerCount(backends[1]),
                        "Fastest count for Kodkod does not match.")
        );
    }

    @Test
    void shouldCountAllFastestBackendsWhenSomeShareFastestTime() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};

        TimedAnswer prob1 = new TimedAnswer(Answer.ERROR, 100L);
        TimedAnswer prob2 = new TimedAnswer(Answer.INVALID, 100L);

        TimedAnswer kodkod1 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer kodkod2 = new TimedAnswer(Answer.INVALID, 100L); // same as ProB

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, prob1, kodkod1);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, prob2, kodkod2);

        PredDbAnalysis analysis = new PredDbAnalysis();
        analysis.add(null, entry1);
        analysis.add(null, entry2);

        assertAll(
                () -> assertEquals(new Long(1), analysis.getFastestAnswerCount(backends[0]),
                        "Fastest count for ProB does not match."),
                () -> assertEquals(new Long(2), analysis.getFastestAnswerCount(backends[1]),
                        "Fastest count for Kodkod does not match.")
        );
    }

    @Test
    void shouldBeUnequal() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);
        TimedAnswer t5 = new TimedAnswer(Answer.VALID, 500L);
        TimedAnswer t6 = new TimedAnswer(Answer.VALID, 600L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t5, t6);

        PredDbAnalysis analysis1 = new PredDbAnalysis();
        analysis1.add(null, entry1);
        analysis1.add(null, entry2);

        PredDbAnalysis analysis2 = new PredDbAnalysis();
        analysis2.add(null, entry1);
        analysis2.add(null, entry3);

        assertNotEquals(analysis1, analysis2);
    }

    @Test
    void shouldBeEqual() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);
        TimedAnswer t5 = new TimedAnswer(Answer.VALID, 500L);
        TimedAnswer t6 = new TimedAnswer(Answer.VALID, 600L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t5, t6);

        PredDbAnalysis analysis1 = new PredDbAnalysis();
        analysis1.add(null, entry1);
        analysis1.add(null, entry2);
        analysis1.add(null, entry3);

        PredDbAnalysis analysis2 = new PredDbAnalysis();
        analysis2.add(null, entry1);
        analysis2.add(null, entry2);
        analysis2.add(null, entry3);

        assertEquals(analysis1, analysis2);
    }

    @Test
    void shouldMergeTwoAnalyses() {
        Backend[] backends = {new ProBBackend(), new KodkodBackend()};
        TimedAnswer t1 = new TimedAnswer(Answer.VALID, 100L);
        TimedAnswer t2 = new TimedAnswer(Answer.VALID, 200L);
        TimedAnswer t3 = new TimedAnswer(Answer.VALID, 300L);
        TimedAnswer t4 = new TimedAnswer(Answer.VALID, 400L);
        TimedAnswer t5 = new TimedAnswer(Answer.VALID, 500L);
        TimedAnswer t6 = new TimedAnswer(Answer.VALID, 600L);

        PredDbEntry entry1 = new PredDbEntry(null, null, backends, t1, t2);
        PredDbEntry entry2 = new PredDbEntry(null, null, backends, t3, t4);
        PredDbEntry entry3 = new PredDbEntry(null, null, backends, t5, t6);

        PredDbAnalysis oneStep = new PredDbAnalysis();
        oneStep.add(null, entry1);
        oneStep.add(null, entry1);
        oneStep.add(null, entry2);
        oneStep.add(null, entry3);

        PredDbAnalysis merged = new PredDbAnalysis();
        merged.add(null, entry1);
        merged.add(null, entry2);

        PredDbAnalysis other = new PredDbAnalysis();
        other.add(null, entry1);
        other.add(null, entry3);

        merged.mergeWith(other);

        assertEquals(oneStep, merged);
    }
}
