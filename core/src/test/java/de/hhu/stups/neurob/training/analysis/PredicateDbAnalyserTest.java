package de.hhu.stups.neurob.training.analysis;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredicateDbAnalyserTest {

    private PredicateDbFormat<PredDbEntry> dbFormat;

    private final ProBBackend prob = new ProBBackend();
    private final KodkodBackend kodkod = new KodkodBackend();
    private final Z3Backend z3 = new Z3Backend();

    private final Backend[] backends = {prob, kodkod, z3};

    @BeforeEach
    void setupFormat() {
        dbFormat = mock(PredicateDbFormat.class);
    }

    @Test
    void shouldAnalyseStreamOfPredicates() throws IOException {
        // Setup stream
        Set<TrainingSample<BPredicate, PredDbEntry>> samples1 = new HashSet<>();
        samples1.add(createSample("pred1", "source1.mch", Answer.VALID, 100L, Answer.VALID, 300L, Answer.UNKNOWN, 100L));
        samples1.add(createSample("pred2", "source1.mch", Answer.TIMEOUT, 200L, Answer.TIMEOUT, 200L, Answer.ERROR, 300L));
        samples1.add(createSample("pred3", "source1.mch", Answer.VALID, 300L, Answer.ERROR, 500L, Answer.INVALID, 400L));
        samples1.add(createSample("pred4", "source1.mch", Answer.VALID, 400L, Answer.UNKNOWN, 100L, Answer.UNKNOWN, 600L));

        Set<TrainingSample<BPredicate, PredDbEntry>> samples2 = new HashSet<>();
        samples2.add(createSample("pred5", "source2.mch", Answer.VALID, 100L, Answer.INVALID, 300L, Answer.VALID, 100L));
        samples2.add(createSample("pred6", "source2.mch", Answer.TIMEOUT, 200L, Answer.TIMEOUT, 200L, Answer.ERROR, 300L));
        samples2.add(createSample("pred7", "source2.mch", Answer.INVALID, 300L, Answer.ERROR, 500L, Answer.UNKNOWN, 400L));
        samples2.add(createSample("pred8", "source3.mch", Answer.ERROR, 400L, Answer.VALID, 100L, Answer.SOLVABLE, 600L));

        when(dbFormat.loadTrainingData(any())).thenReturn(Stream.of(
                new TrainingData<BPredicate, PredDbEntry>(null, samples1.stream()),
                new TrainingData<BPredicate, PredDbEntry>(null, samples2.stream())
        ));

        PredDbAnalysis analysis = new PredicateDbAnalyser(dbFormat).analyse(Paths.get("non/existent"));

        assertAll("Check analysis after stream is done",
                // Number of predicates
                () -> assertEquals(new Long(8), analysis.getPredCount(),
                        "Number of Predicates does not match"),
                // Number of B Machines
                () -> assertEquals(3, analysis.getBMachineCount(),
                        "Number of BMachines does not match"),
                // Number of predicates per answer
                () -> assertEquals(new Long(3), analysis.getPredCount(Answer.VALID),
                        "Number of VALID Predicates does not match"),
                () -> assertEquals(new Long(1), analysis.getPredCount(Answer.INVALID),
                        "Number of INVALID Predicates does not match"),
                () -> assertEquals(new Long(4), analysis.getPredCount(Answer.SOLVABLE),
                        "Number of SOLVABLE Predicates does not match"),
                () -> assertEquals(new Long(0), analysis.getPredCount(Answer.UNKNOWN),
                        "Number of UNKNOWN Predicates does not match"),
                () -> assertEquals(new Long(2), analysis.getPredCount(Answer.TIMEOUT),
                        "Number of TIMEOUT Predicates does not match"),
                () -> assertEquals(new Long(0), analysis.getPredCount(Answer.ERROR),
                        "Number of ERROR Predicates does not match"),
                () -> assertEquals(new Long(2), analysis.getContradictionCount(),
                        "Number of Contradictions does not match"),
                // Number of predicates per backend
                // TODO: Not yet implemented
//                () -> assertEquals(new Long(8), analysis.getPredCount(prob),
//                        "Number of Predicates for ProB does not match"),
//                () -> assertEquals(new Long(8), analysis.getPredCount(kodkod),
//                        "Number of Predicates for Kodkod does not match"),
//                () -> assertEquals(new Long(8), analysis.getPredCount(z3),
//                        "Number of Predicates for Z3 does not match"),
                // Classification: ProB
                () -> assertEquals(new Long(4), analysis.getAnswerCount(Answer.VALID, prob),
                        "VALID count for ProB does not match"),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.INVALID, prob),
                        "INVALID count for ProB does not match"),
                () -> assertEquals(new Long(5), analysis.getAnswerCount(Answer.SOLVABLE, prob),
                        "SOLVABLE count for ProB does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.UNKNOWN, prob),
                        "UNKNOWN count for ProB does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.TIMEOUT, prob),
                        "TIMEOUT count for ProB does not match"),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.ERROR, prob),
                        "ERROR count for ProB does not match"),
                // Classification: Kodkod
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.VALID, kodkod),
                        "VALID count for Kodkod does not match"),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.INVALID, kodkod),
                        "INVALID count for Kodkod does not match"),
                () -> assertEquals(new Long(3), analysis.getAnswerCount(Answer.SOLVABLE, kodkod),
                        "SOLVABLE count for Kodkod does not match"),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.UNKNOWN, kodkod),
                        "UNKNOWN count for Kodkod does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.TIMEOUT, kodkod),
                        "TIMEOUT count for Kodkod does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.ERROR, kodkod),
                        "ERROR count for Kodkod does not match"),
                // Classification: Z3
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.VALID, z3),
                        "VALID count for Z3 does not match"),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.INVALID, z3),
                        "INVALID count for Z3 does not match"),
                () -> assertEquals(new Long(3), analysis.getAnswerCount(Answer.SOLVABLE, z3),
                        "SOLVABLE count for Z3 does not match"),
                () -> assertEquals(new Long(3), analysis.getAnswerCount(Answer.UNKNOWN, z3),
                        "UNKNOWN count for Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.TIMEOUT, z3),
                        "TIMEOUT count for Z3 does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.ERROR, z3),
                        "ERROR count for Z3 does not match"),
                // Classification: ProB+Kodkod
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.VALID, prob, kodkod),
                        "VALID count for ProB+Kodkod does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.INVALID, prob, kodkod),
                        "INVALID count for ProB+Kodkod does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.SOLVABLE, prob, kodkod),
                        "SOLVABLE count for ProB+Kodkod does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.UNKNOWN, prob, kodkod),
                        "UNKNOWN count for ProB+Kodkod does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.TIMEOUT, prob, kodkod),
                        "TIMEOUT count for ProB+Kodkod does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.ERROR, prob, kodkod),
                        "ERROR count for ProB+Kodkod does not match"),
                // Classification: ProB+Z3
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.VALID, prob, z3),
                        "VALID count for ProB+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.INVALID, prob, z3),
                        "INVALID count for ProB+Z3 does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.SOLVABLE, prob, z3),
                        "SOLVABLE count for ProB+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.UNKNOWN, prob, z3),
                        "UNKNOWN count for ProB+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.TIMEOUT, prob, z3),
                        "TIMEOUT count for ProB+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.ERROR, prob, z3),
                        "ERROR count for ProB+Z3 does not match"),
                // Classification: Kodkod+Z3
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.VALID, kodkod, z3),
                        "VALID count for Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.INVALID, kodkod, z3),
                        "INVALID count for Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(2), analysis.getAnswerCount(Answer.SOLVABLE, kodkod, z3),
                        "SOLVABLE count for Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.UNKNOWN, kodkod, z3),
                        "UNKNOWN count for Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.TIMEOUT, kodkod, z3),
                        "TIMEOUT count for Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.ERROR, kodkod, z3),
                        "ERROR count for Kodkod+Z3 does not match"),
                // Classification: ProB+Kodkod+Z3
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.VALID, prob, kodkod, z3),
                        "VALID count for ProB+Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.INVALID, prob, kodkod, z3),
                        "INVALID count for ProB+Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(1), analysis.getAnswerCount(Answer.SOLVABLE, prob, kodkod, z3),
                        "SOLVABLE count for ProB+Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.UNKNOWN, prob, kodkod, z3),
                        "UNKNOWN count for ProB+Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.TIMEOUT, prob, kodkod, z3),
                        "TIMEOUT count for ProB+Kodkod+Z3 does not match"),
                () -> assertEquals(new Long(0), analysis.getAnswerCount(Answer.ERROR, prob, kodkod, z3),
                        "ERROR count for ProB+Kodkod+Z3 does not match"),
                // Fastest answers
                () -> assertEquals(new Long(4), analysis.getFastestAnswerCount(prob),
                        "Fastest answer count for ProB does not match"),
                () -> assertEquals(new Long(2), analysis.getFastestAnswerCount(kodkod),
                        "Fastest answer count for Kodkod does not match"),
                () -> assertEquals(new Long(2), analysis.getFastestAnswerCount(z3),
                        "Fastest answer count for Z3 does not match")
        );
    }

    private TrainingSample<BPredicate, PredDbEntry> createSample(
            String pred,
            String source,
            Answer probAnswer, long probTime,
            Answer kodkodAnswer, long kodkodTime,
            Answer z3Answer, long z3Time) {
        BPredicate bpred = new BPredicate(pred);

        TimedAnswer probTimedAnswer = new TimedAnswer(probAnswer, probTime);
        TimedAnswer kodkodTimedAnswer = new TimedAnswer(kodkodAnswer, kodkodTime);
        TimedAnswer z3TimedAnswer = new TimedAnswer(z3Answer, z3Time);

        PredDbEntry entry = new PredDbEntry(bpred, new BMachine(source), backends,
                probTimedAnswer, kodkodTimedAnswer, z3TimedAnswer);
        return new TrainingSample<>(bpred, entry);
    }

}
