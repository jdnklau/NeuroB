package de.hhu.stups.neurob.training.db;

import com.google.inject.Guice;
import de.hhu.stups.neurob.core.api.ProB2;
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
import de.hhu.stups.neurob.core.api.bmethod.MultiMachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.prob.cli.CliVersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PredDbEntry extends PredicateLabelling {

    private final BPredicate pred;
    private final BMachine source;
    private final Map<Backend, TimedAnswer> results;
    private final Map<Backend, SamplingStatistic> statistics;
    public static final Long DEFAULT_TIMEOUT = 2500L;
    public static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;
    private final Backend[] backendsUsed;
    /**
     * Revision of ProB used to generate the answers
     */
    private final CliVersionNumber probRevision;

    private static final Logger log =
            LoggerFactory.getLogger(PredDbEntry.class);

    /**
     * Array over default backends in use, also providing an ordering by id.
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
     * @param pred    Predicate over which the results were gathered
     * @param source  Source machine from which the predicate originates
     * @param results Map of backends with corresponding results
     */
    public PredDbEntry(BPredicate pred, BMachine source, Map<Backend, TimedAnswer> results) {
        this(pred, source, results.keySet().toArray(new Backend[0]), results);
    }

    /**
     * Initialises this entry with the given results.
     * <p>
     * From the results, only the {@link #DEFAULT_BACKENDS} are used,
     * which also imply an ordering.
     *
     * @param pred         Predicate over which the results were gathered
     * @param source       Source machine from which the predicate originates
     * @param results      Map of backends with corresponding results
     * @param probRevision Version of the ProB CLI
     */
    public PredDbEntry(BPredicate pred, BMachine source, Map<Backend, TimedAnswer> results,
                       CliVersionNumber probRevision) {
        this(pred, source, DEFAULT_BACKENDS, results, probRevision);
    }

    /**
     * Initialises this entry with the given results.
     * <p>
     * The given {@code orderedBackends} both dictate which backends
     * are to be used from the {@code results}
     * and imply an ordering over the backends.
     *
     * @param pred            Predicate over which the results were gathered
     * @param source          Source machine from which the predicate originates
     * @param orderedBackends Array of backends to be used
     * @param results         Map of backends with corresponding results
     */
    public PredDbEntry(BPredicate pred, BMachine source,
                       Backend[] orderedBackends, Map<Backend, TimedAnswer> results) {
        this(pred, source, orderedBackends, results, null);
    }

    /**
     * Initialises this entry with the given results.
     * <p>
     * The given {@code orderedBackends} both dictate which backends
     * are to be used from the {@code results}
     * and imply an ordering over the backends.
     *
     * @param pred            Predicate over which the results were gathered
     * @param source          Source machine from which the predicate originates
     * @param orderedBackends Array of backends to be used
     * @param results         Map of backends with corresponding results
     * @param probRevision    Hash of last git commit of the used revision of ProB Cli
     */
    public PredDbEntry(BPredicate pred, BMachine source,
                       Backend[] orderedBackends, Map<Backend, TimedAnswer> results,
                       CliVersionNumber probRevision) {
        super(pred, toArray(results, orderedBackends));
        this.pred = pred;
        this.source = source;
        this.results = results;
        this.backendsUsed = orderedBackends;
        this.probRevision = probRevision;

        // Build statistics map.
        this.statistics = new HashMap<>();
        for (Backend b : results.keySet()) {
            TimedAnswer answer = results.get(b);
            SamplingStatistic stats;
            if (answer == null) {
                continue;
            } else if (answer instanceof SampledTimedAnswer) {
                stats = ((SampledTimedAnswer) answer).getStats();
            } else {
                stats = SampledTimedAnswer.from(answer).getStats();
            }

            statistics.put(b, stats);
        }
    }

    /**
     * Initialises this entry with the given results.
     * The given backends are matched to the answers by order. A backend and an answer with
     * the same index are matched together.
     *
     * @param pred            Predicate over which the results were gathered
     * @param source          Source machine from which the predicate originates
     * @param orderedBackends Order of backends.
     * @param answers         Answers corresponding to backends with same indices.
     */
    public PredDbEntry(BPredicate pred, BMachine source,
                       Backend[] orderedBackends, TimedAnswer... answers) {
        this(pred, source, orderedBackends, toMap(orderedBackends, answers));
    }

    /**
     * Translates a given map of
     * {@link Backend backends} to {@link TimedAnswer timed answers}
     * into a Double array, respecting the stated ordering.
     * <p>
     * Backends not listed in the ordering but existent in the map will be ignored.
     * Backends missing from the map yield a {@code null} entry in the resulting array.
     *
     * @param results         Map containing a timed answer for a backend.
     * @param orderedBackends Desired ordering over the backends.
     * @return
     */
    public static Double[] toArray(Map<Backend, TimedAnswer> results, Backend[] orderedBackends) {
        return Arrays.stream(orderedBackends)
                .map(results::get)
                .map(time -> time == null ? null : time.getNanoSeconds())
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

    public SamplingStatistic getStatistics(Backend backend) {
        return statistics.getOrDefault(backend, null);
    }

    /**
     * Collects and returns the backend's answers as array in corresponding order.
     *
     * @param backends
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
     * Returns the ProB Cli version used to generate this entry.
     *
     * @return
     */
    public CliVersionNumber getProbRevision() {
        return probRevision;
    }

    /**
     * Returns the result stored for the given backend.
     * <p>
     * Might return {@code null} if no such backend exists.
     *
     * @param backend
     * @return
     */
    public TimedAnswer getResult(Backend backend) {
        return results.get(backend);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PredDbEntry) {
            PredDbEntry other = (PredDbEntry) o;

            boolean sourceEqual = Objects.equals(this.source, other.source);
            boolean predEqual = Objects.equals(this.pred, other.pred);
            boolean resultsEqual = Objects.equals(this.results, other.results);
            boolean probRevisionEqual = Objects.equals(this.probRevision, other.probRevision);

            return predEqual && resultsEqual && probRevisionEqual && sourceEqual;
        }


        return false;
    }

    @Override
    public String toString() {
        return "[pred=" + pred + ", "
                + "results=" + results + ","
                + "cli-version=" + probRevision + "]";
    }

    public static class Generator implements PredicateLabelGenerating<PredDbEntry> {

        private int samplingSize;
        private final Long timeout;
        private final TimeUnit timeUnit;
        private final Backend[] backends;
        private final CliVersionNumber cliVersion;

        private Map<MachineAccess, MultiMachineAccess> accessMap;

        /**
         * @param samplingSize Number of measurements per backend,
         *                     from which the average run time is taken.
         * @param backends     Backends to run in given order for each predicate
         * @param timeout
         * @param timeUnit
         */
        public Generator(int samplingSize, Long timeout, TimeUnit timeUnit, Backend... backends) {
            this(samplingSize, ProB2.api.getVersion(),
                    timeout, timeUnit, backends);
        }

        /**
         * @param samplingSize Number of measurements per backend,
         *                     from which the average run time is taken.
         * @param backends     Backends to run in given order for each predicate
         * @param timeout
         * @param timeUnit
         */
        public Generator(int samplingSize, CliVersionNumber cliVersion,
                         Long timeout, TimeUnit timeUnit, Backend... backends) {
            this.samplingSize = samplingSize;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.backends = backends;
            this.cliVersion = cliVersion;

            this.accessMap = new HashMap<>();
        }

        /**
         * @param samplingSize Number of measurements per backend,
         *                     from which the average run time is taken.
         * @param backends
         */
        public Generator(int samplingSize, Backend... backends) {
            this(samplingSize, PredDbEntry.DEFAULT_TIMEOUT, PredDbEntry.DEFAULT_TIMEUNIT, backends);
        }

        /**
         * @param samplingSize Number of measurements per backend,
         *                     from which the average run time is taken.
         * @param backends
         */
        public Generator(int samplingSize, CliVersionNumber version, Backend... backends) {
            this(samplingSize, version, PredDbEntry.DEFAULT_TIMEOUT, PredDbEntry.DEFAULT_TIMEUNIT, backends);
        }


        @Override
        public PredDbEntry generate(BPredicate predicate, MachineAccess machineAccess)
                throws LabelCreationException {
            MultiMachineAccess multiAccess;
            try {
                multiAccess = getMultiAccess(machineAccess);
            } catch (MachineAccessException e) {
                throw new LabelCreationException("Could not load access to machine for each backend", e);
            }

            // Gather results
            Map<Backend, TimedAnswer> results = new HashMap<>();
            Arrays.stream(backends).forEach(b -> {
                SampledTimedAnswer answer;
                MachineAccess backendAccess = multiAccess != null
                        ? multiAccess.getAccess(b)
                        : null;
                try {
                    answer = samplePredicate(predicate, b, backendAccess);
                } catch (LabelCreationException e) {
                    log.error("Unable to sample {} with backend {}",
                            predicate, b, e);
                    answer = null;
                }

                results.put(b, answer);
            });

            BMachine bMachine = machineAccess != null
                    ? new BMachine(machineAccess.getSource())
                    : null;

            return new PredDbEntry(predicate, bMachine, backends, results, cliVersion);
        }

        MultiMachineAccess getMultiAccess(MachineAccess baseAccess) throws MachineAccessException {
            if (baseAccess == null) {
                return null;
            } else if (accessMap.containsKey(baseAccess)) {
                return accessMap.get(baseAccess);
            }

            // Access not yet mapped to a multi access; translate it, put in map, add closeHandler

            MultiMachineAccess ma = new MultiMachineAccess(
                    baseAccess.getSource(),
                    baseAccess.getMachineType(),
                    backends,
                    baseAccess.isLoaded());

            accessMap.put(baseAccess, ma);

            baseAccess.onClose(a -> ma.close());
            baseAccess.onClose(a -> accessMap.remove(baseAccess));

            return ma;
        }

        public SampledTimedAnswer samplePredicate(BPredicate pred, Backend backend, MachineAccess bMachine)
                throws LabelCreationException {
            log.trace("Sampling timings over backend {}; {} times",
                    backend, samplingSize);

            if (samplingSize == 0) {
                return null;
            }

            List<Long> measurements = new ArrayList<>();
            TimedAnswer lastAnswer = null;
            for (int i = 0; i <= samplingSize; i++) {
                try {
                    TimedAnswer timing = backend.solvePredicate(pred, bMachine,
                            backend.getTimeOutValue(), backend.getTimeOutUnit());
                    // stop if already error or timeout
                    Answer answer = timing.getAnswer();
                    if (answer.equals(Answer.ERROR) || answer.equals(Answer.TIMEOUT)) {
                        return SampledTimedAnswer.from(timing);
                    }

                    // NOTE: There seems to be a sampling inconsistency where the first sampling increases
                    // the stdev over 10 runs by up to 185 %; discarding the very first sampled time reduces
                    // the stdev hence by around 99 %.
                    // Hence, we skip the first one.
                    if (i == 0) {
                        continue;
                    }

                    measurements.add(timing.getNanoSeconds());
                    log.debug("Sampling no. {} over {} took {} ns", i + 1, backend, timing.getNanoSeconds());
                    lastAnswer = timing;
                } catch (FormulaException e) {
                    throw new LabelCreationException(
                            "Could not create timing sample #" + i
                                    + " for " + backend.toString() + "over predicate "
                                    + pred.toString(),
                            e);
                }
            }

            // generate statistics
            long mean = measurements.stream().reduce(0L, Long::sum) / samplingSize;
            SamplingStatistic stats = new SamplingStatistic(
                    measurements.stream()
                            .map(Long::doubleValue)
                            .collect(Collectors.toList()),
                    false);

            return new SampledTimedAnswer(
                    lastAnswer.getAnswer(), mean, stats, lastAnswer.getMessage());
        }
    }
}
