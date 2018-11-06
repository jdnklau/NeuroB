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
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PredDbEntry extends PredicateLabelling {

    private final BPredicate pred;
    private final BMachine source;
    private final Map<Backend, TimedAnswer> results;
    public static final Long DEFAULT_TIMEOUT = 2500L;
    public static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;
    private final Backend[] backendsUsed;

    private static final Logger log =
            LoggerFactory.getLogger(PredDbEntry.class);

    /**
     * Array over backends in use, also providing an ordering by id.
     * <ol start="0">
     * <li>ProB</li>
     * <li>Kodkod</li>
     * <li>Z3</li>
     * <li>SMT_SUPPORTED_INTERPRETER</li>
     * </ol>
     */
    public final static Backend[] DEFAULT_BACKENDS = {
            new ProBBackend(),
            new KodkodBackend(),
            new Z3Backend(),
            new SmtBackend(),
    };

    /**
     * Initialises this entry with the given results.
     *
     * From the results, only the {@link #DEFAULT_BACKENDS} are used,
     * which also imply an ordering.
     *
     * @param pred Predicate over which the results were gathered
     * @param source Source machine from which the predicate originates
     * @param results Map of backends with corresponding results
     *
     * @
     */
    public PredDbEntry(BPredicate pred, BMachine source, Map<Backend, TimedAnswer> results) {
        this(pred, source, DEFAULT_BACKENDS, results);
    }

    /**
     * Initialises this entry with the given results.
     *
     * The given {@code orderedBackends} both dictate which backends
     * are to be used from the {@code results}
     * and imply an ordering over the backends.
     *
     * @param pred Predicate over which the results were gathered
     * @param source Source machine from which the predicate originates
     * @param orderedBackends Array of backends to be used
     * @param results Map of backends with corresponding results
     */
    public PredDbEntry(BPredicate pred, BMachine source,
            Backend[] orderedBackends,Map<Backend, TimedAnswer> results) {
        super(pred, toArray(results, orderedBackends));
        this.pred = pred;
        this.source = source;
        this.results = results;
        this.backendsUsed = orderedBackends;
    }

    /**
     * Initialises this entry with the given results.
     * The given backends are matched to the answers by order. A backend and an answer with
     * the same index are matched together.
     *
     * @param pred Predicate over which the results were gathered
     * @param source Source machine from which the predicate originates
     * @param orderedBackends Order of backends.
     * @param answers Answers corresponding to backends with same indices.
     */
    public PredDbEntry(BPredicate pred, BMachine source,
            Backend[] orderedBackends, TimedAnswer... answers) {
        this(pred, source, toMap(orderedBackends, answers));
    }

    /**
     * Translates a given map of
     * {@link Backend backends} to {@link TimedAnswer timed answers}
     * into a Double array, respecting the stated ordering.
     * <p>
     * Backends not listed in the ordering but existent in the map will be ignored.
     * Backends missing from the map yield a {@code null} entry in the resulting array.
     *
     * @param results Map containing a timed answer for a backend.
     * @param orderedBackends Desired ordering over the backends.
     *
     * @return
     */
    public static Double[] toArray(Map<Backend, TimedAnswer> results, Backend[] orderedBackends) {
        return Arrays.stream(orderedBackends)
                .map(results::get)
                .map(time -> time == null ? null : time.getTime())
                .map(longTime -> longTime == null ? null : longTime.doubleValue())
                .toArray(Double[]::new);
    }

    /**
     * Zips together an array of {@link Backend backends}
     * with an array of {@link TimedAnswer timed answers}.
     * <p>
     * The lengths of both arrays must be identical.
     *
     * @param backends
     * @param answers
     *
     * @return
     */
    public static Map<Backend, TimedAnswer> toMap(Backend[] backends, TimedAnswer... answers) {
        Map<Backend, TimedAnswer> map = new HashMap<>();

        // Check for argument mismatch
        if (backends.length != answers.length) {
            throw new IllegalArgumentException(
                    "Number of backends does not match number of answers: "
                    + backends.length + " vs " + answers.length);
        }

        for (int i = 0; i < backends.length; i++) {
            map.put(backends[i], answers[i]);
        }

        return map;
    }

    /**
     * Collects and returns the backends' answers as array,
     * ordered by {@link #DEFAULT_BACKENDS}.
     *
     * @return
     */
    public TimedAnswer[] getAnswerArray() {
        return getAnswerArray(DEFAULT_BACKENDS);
    }

    /**
     * Collects and returns the backend's answers as array in corresponding order.
     *
     * @param backends
     *
     * @return
     */
    public TimedAnswer[] getAnswerArray(Backend... backends) {
        return Arrays.stream(backends)
                .map(results::get)
                .toArray(TimedAnswer[]::new);

    }

    public BPredicate getPredicate() {
        return pred;
    }

    public BMachine getSource() {
        return source;
    }

    public Backend[] getBackendsUsed() {
        return backendsUsed;
    }

    public Map<Backend, TimedAnswer> getResults() {
        return results;
    }

    /**
     * Returns the result stored for the given backend.
     *
     * Might return {@code null} if no such backend exists.
     * @param backend
     * @return
     */
    public TimedAnswer getResult(Backend backend) {
        return results.get(backend);
    }

    public static class Generator implements PredicateLabelGenerating<PredDbEntry> {

        private int samplingSize;
        private final Long timeout;
        private final TimeUnit timeUnit;
        private final Backend[] backends;

        /**
         * @param samplingSize Number of measurements per backend,
         *         from which the average run time is taken.
         * @param backends Backends to run in given order for each predicate
         * @param timeout
         * @param timeUnit
         */
        public Generator(int samplingSize, Long timeout, TimeUnit timeUnit, Backend... backends) {
            this.samplingSize = samplingSize;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.backends = backends;
        }

        /**
         * @param samplingSize Number of measurements per backend,
         *         from which the average run time is taken.
         * @param backends
         */
        public Generator(int samplingSize, Backend... backends) {
            this(samplingSize, PredDbEntry.DEFAULT_TIMEOUT, PredDbEntry.DEFAULT_TIMEUNIT, backends);
        }


        @Override
        public PredDbEntry generate(BPredicate predicate, MachineAccess machineAccess) throws LabelCreationException {
            // Gather results
            Map<Backend, TimedAnswer> results = new HashMap<>();
            for (Backend b : backends) {
                TimedAnswer answer;
                try {
                    answer = samplePredicate(predicate, b, machineAccess);
                } catch (LabelCreationException e) {
                    log.error("Unable to sample {} with backend {}",
                            predicate, b, e);
                    answer = null;
                }

                results.put(b, answer);
            }

            BMachine bMachine = machineAccess != null
                    ? new BMachine(machineAccess.getSource())
                    : null;

            return new PredDbEntry(predicate, bMachine, results);
        }

        public TimedAnswer samplePredicate(BPredicate pred, Backend backend, MachineAccess bMachine)
                throws LabelCreationException {
            log.trace("Sampling timings over backend {}; {} times",
                    backend, samplingSize);

            if (samplingSize == 0) {
                return null;
            }

            Long sampled = 0L; // sum of all run times
            TimedAnswer lastAnswer = null;
            for (int i = 0; i < samplingSize; i++) {
                try {
                    TimedAnswer timing = backend.solvePredicate(pred, bMachine,
                            timeout, timeUnit);
                    // stop if already error or timeout
                    Answer answer = timing.getAnswer();
                    if (answer.equals(Answer.ERROR) || answer.equals(Answer.TIMEOUT)) {
                        return timing;
                    }
                    sampled += timing.getTime();
                    lastAnswer = timing;
                } catch (FormulaException e) {
                    throw new LabelCreationException(
                            "Could not create timing sample #" + i
                            + " for " + backend.toString() + "over predicate "
                            + pred.toString(),
                            e);
                }
            }

            return new TimedAnswer(
                    lastAnswer.getAnswer(), sampled / samplingSize, lastAnswer.getMessage());
        }
    }
}
