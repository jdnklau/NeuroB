package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Labels a predicate over different backends with how long each backend took
 * to give a decision.
 * Timings are given in nano seconds.
 * <p>
 * This labelling makes use of the cost function presented by Healy et al. [1]:
 * the cost for a time t  is:
 * <ul>
 *     <li>t for a solved response,</li>
 *     <li>t + timeout for unknown,</li>
 *     <li>t + timeout*2 for error/timeout.</li>
 * </ul>
 *
 *
 * <p>
 * If in the constructor no timeout values are set, the defaults are taken,
 * dictated by {@link HealyTimings#defaultTimeout}
 * and {@link HealyTimings#defaultTimeoutUnit}.
 * <p>
 * [1] Healy, Andrew, Rosemary Monahan, and James F. Power.
 *     "Predicting SMT solver performance for software verification."
 *     arXiv preprint arXiv:1701.08466 (2017).
 */
public class HealyTimings extends PredicateLabelling {

    // Time out how long a predicate will maximally run in evaluation.
    private final Long timeOut;
    private final TimeUnit timeUnit;

    /** Default time out set for predicate evaluation. */
    public final static Long defaultTimeout = 2500L;
    /** Unit of defaultTimeout */
    public final static TimeUnit defaultTimeoutUnit = TimeUnit.MILLISECONDS;

    private final Backend[] usedBackends;
    private Map<Backend, TimedAnswer> timings;

    private static final Logger log =
            LoggerFactory.getLogger(HealyTimings.class);

    /**
     * Sets the default timeout for predicate evaluation.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public HealyTimings(BPredicate predicate, MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        this(predicate, defaultTimeout, defaultTimeoutUnit, bMachine, backends);
    }

    /**
     * Sets the default timeout for predicate evaluation.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public HealyTimings(String predicate, MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        this(BPredicate.of(predicate), defaultTimeout, defaultTimeoutUnit, bMachine, backends);
    }

    /**
     * @param predicate
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public HealyTimings(BPredicate predicate, Long timeOut, TimeUnit timeOutUnit,
            MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        super(predicate, new Double[backends.length]);

        this.usedBackends = backends;

        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;
        this.timings = createTimings(bMachine, backends);

        for (int i = 0; i < labellingDimension; i++) {
            labellingArray[i] = new Double(calcHealyCost(timings.get(backends[i])));
        }
    }

    /**
     * @param predicate
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public HealyTimings(String predicate,
            Long timeOut, TimeUnit timeOutUnit,
            MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        this(BPredicate.of(predicate),
                timeOut, timeOutUnit, bMachine, backends);
    }

    /**
     * Creates a labelling vector with respect to the given time mapping.
     * <p>
     * The listed backends dictate the order in which the entries of the vector will
     * be sorted. Backends not present in the mapping receive the value of -1.0.
     * Backends which are present in the mapping but not specifically listed for the
     * ordering are ignored.
     * <p>
     * For example the call
     * <pre>
     * {@code new DecisionTimings("pred", {A=2, B=1, C=3}, B, A, D)}
     * </pre>
     * will result in the vector {@code <1.0, 2.0, -1.0>}.
     * <p>
     * Internally the timeout data used for generating the mapping are assumed to
     * be {@link #defaultTimeout} and {@link #defaultTimeoutUnit},
     * with a sampling size of 1.
     *
     * @param predicate
     * @param timeMapping Mapping of backends to time measures. Will be translated into a
     *         vector.
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public HealyTimings(BPredicate predicate, Map<Backend, TimedAnswer> timeMapping,
            Backend... backends) {
        this(predicate, defaultTimeout, defaultTimeoutUnit, timeMapping, backends);
    }

    /**
     * Creates a labelling vector with respect to the given time mapping.
     * <p>
     * The listed backends dictate the order in which the entries of the vector will
     * be sorted. Backends not present in the mapping receive the value of -1.0.
     * Backends which are present in the mapping but not specifically listed for the
     * ordering are ignored.
     * <p>
     * For example the call
     * <pre>
     * {@code new DecisionTimings("pred", {A=2, B=1, C=3}, B, A, D)}
     * </pre>
     * will result in the vector {@code <1.0, 2.0, -1.0>}.
     * <p>
     * Internally the timeout data used for generating the mapping are assumed to
     * be {@link #defaultTimeout} and {@link #defaultTimeoutUnit},
     * with a sampling size of 1.
     *
     * @param predicate
     * @param timeMapping Mapping of backends to time measures. Will be translated into a
     *         vector.
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public HealyTimings(String predicate, Map<Backend, TimedAnswer> timeMapping, Backend... backends) {
        this(BPredicate.of(predicate), defaultTimeout, defaultTimeoutUnit, timeMapping, backends);
    }

    /**
     * Creates a labelling vector with respect to the given time mapping.
     * <p>
     * The listed backends dictate the order in which the entries of the vector will
     * be sorted. Backends not present in the mapping receive the value of -1.0.
     * Backends which are present in the mapping but not specifically listed for the
     * ordering are ignored.
     * <p>
     * For example the call
     * <pre>
     * {@code new DecisionTimings("pred", {A=2, B=1, C=3}, B, A, D)}
     * </pre>
     * will result in the vector {@code <1.0, 2.0, -1.0>}.
     *
     * @param predicate
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param timeMapping Mapping of backends to time measures. Will be translated into a
     *         vector.
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public HealyTimings(BPredicate predicate, Long timeOut, TimeUnit timeOutUnit,
            Map<Backend, TimedAnswer> timeMapping, Backend[] backends) {
        super(predicate, new Double[backends.length]);

        this.usedBackends = backends;

        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;

        this.timings = timeMapping;

        // Distribute timings to labelling array, in order
        for (int i = 0; i < labellingDimension; i++) {
            if (timings.containsKey(backends[i])) {
                labellingArray[i] = new Double(calcHealyCost(
                        timings.get(backends[i])));
            } else {
                labellingArray[i] = -1.;
            }
        }
    }

    /**
     * Creates a labelling vector with respect to the given time mapping.
     * <p>
     * The listed backends dictate the order in which the entries of the vector will
     * be sorted. Backends not present in the mapping receive the value of -1.0.
     * Backends which are present in the mapping but not specifically listed for the
     * ordering are ignored.
     * <p>
     * For example the call
     * <pre>
     * {@code new DecisionTimings("pred", {A=2, B=1, C=3}, B, A, D)}
     * </pre>
     * will result in the vector {@code <1.0, 2.0, -1.0>}.
     *
     * @param predicate
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param timeMapping Mapping of backends to time measures. Will be translated into a
     *         vector.
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public HealyTimings(String predicate,
            Long timeOut, TimeUnit timeOutUnit,
            Map<Backend, TimedAnswer> timeMapping, Backend[] backends) {
        this(BPredicate.of(predicate), timeOut, timeOutUnit,
                timeMapping, backends);
    }

    /**
     * Creates a labelling vector with respect to the given order of backends.
     * The {@code backends} array implies the ordering of the given timings,
     * i.e. the backend with index 0 is matched to the first timing,
     * index 1 with the second, and so on.
     * <p>
     * Internally the timeout data used for generating the mapping are assumed to
     * be {@link #defaultTimeout} and {@link #defaultTimeoutUnit},
     * with a sampling size of 1.
     *
     * @param predicate
     * @param backends Array of backends in use.
     * @param timings Already measured timings for the backends in use. Order is important.
     */
    public HealyTimings(BPredicate predicate,
            Backend[] backends, TimedAnswer... timings) {
        this(predicate, 1, defaultTimeout, defaultTimeoutUnit, backends, timings);
    }

    /**
     * Creates a labelling vector with respect to the given order of backends.
     * The {@code backends} array implies the ordering of the given timings,
     * i.e. the backend with index 0 is matched to the first timing,
     * index 1 with the second, and so on.
     * <p>
     * Internally the timeout data used for generating the mapping are assumed to
     * be {@link #defaultTimeout} and {@link #defaultTimeoutUnit},
     * with a sampling size of 1.
     *
     * @param predicate
     * @param backends Array of backends in use.
     * @param timings Already measured timings for the backends in use. Order is important.
     */
    public HealyTimings(String predicate,
            Backend[] backends, TimedAnswer... timings) {
        this(BPredicate.of(predicate), 1, defaultTimeout, defaultTimeoutUnit, backends, timings);
    }

    /**
     * Creates a labelling vector with respect to the given order of backends.
     * The {@code backends} array implies the ordering of the given timings,
     * i.e. the backend with index 0 is matched to the first timing,
     * index 1 with the second, and so on.
     *
     * @param predicate
     * @param sampleSize Number of times each timing was run; final time
     *         is taken from the average.
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param backends Array of backends in use.
     * @param timings Already measured timings for the backends in use. Order is important.
     */
    public HealyTimings(BPredicate predicate,
            int sampleSize, Long timeOut, TimeUnit timeOutUnit,
            Backend[] backends, TimedAnswer... timings) {
        // bind basic settings
        super(predicate, timingsToCostArray(timings, timeOut, timeOutUnit));

        if (timings.length != backends.length) {
            throw new IllegalArgumentException(
                    "Number of given timings must match number of backends");
        }

        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;

        // bind timings
        this.usedBackends = backends;
        this.timings = new HashMap<>();
        for (int i = 0; i < backends.length; i++) {
            this.timings.put(backends[i], timings[i]);
        }
    }

    private Map<Backend, TimedAnswer> createTimings(MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        Map<Backend, TimedAnswer> timings = new HashMap<>();

        // TODO: set timeout inside of state space as well
        // NOTE: I think the above has already been done.

        log.debug("Sample timings for the backends {} over predicate {}",
                backends, predicate);
        for (Backend backend : backends) {
            timings.put(backend, sampleTimings(bMachine, backend));
        }

        return timings;
    }

    private TimedAnswer sampleTimings(MachineAccess bMachine, Backend backend)
            throws LabelCreationException {
        log.trace("Measuring runtime of backend {}", backend);

        try {
            return backend.solvePredicate(predicate, bMachine, timeOut, timeUnit);
        } catch (FormulaException e) {
            throw new LabelCreationException(
                    "Could not create timing sample for " + backend.toString()
                    + " over predicate " + predicate, e);
        }
    }

    /**
     * Returns the estimated cost by Healy et al.'s cost function depending on response type
     * in nano seconds.
     * <p>
     * The cost for a time t  is:
     * <ul>
     *     <li>t for a solved response,</li>
     *     <li>t + timeout for unknown,</li>
     *     <li>t + timeout*2 for error/timeout.</li>
     * </ul>
     *
     * @param response
     *
     * @return
     */
    Long calcHealyCost(TimedAnswer response) {
        return calcHealyCost(response, timeOut, timeUnit);
    }

    static Long calcHealyCost(TimedAnswer answer, Long timeOut, TimeUnit timeUnit) {
        if (answer == null) {
            return -1L; // TODO: -1 would serve as indicator something is missing. Keep it that way?
            //       Maybe 3*to would be a more sensible choice.
        }

        Answer response = answer.getAnswer();
        Long nanoseconds = answer.getNanoSeconds();
        Long to = TimeUnit.NANOSECONDS.convert(timeOut, timeUnit);

        if (Answer.VALID.equals(response) || Answer.INVALID.equals(response)
            || Answer.SOLVABLE.equals(response)) {
            return nanoseconds;
        } else if (Answer.UNKNOWN.equals(response)) {
            return nanoseconds + to;
        } else if (Answer.ERROR.equals(response) || Answer.TIMEOUT.equals(response)) {
            return nanoseconds + 2 * to;
        } else {
            return -1L; // TODO: -1 would serve as indicator something is missing. Keep it that way?
            //       Maybe 3*to would be a more sensible choice.
        }
    }

    static Double[] timingsToCostArray(TimedAnswer[] responses, Long timeOut, TimeUnit timeUnit) {
        return Arrays.stream(responses).map(r -> calcHealyCost(r, timeOut, timeUnit))
                .map(Double::new)
                .toArray(Double[]::new);
    }

    /**
     * Returns an array consisting of the measured timings for each backend in
     * use.
     * <p>
     * The order of entries matches the order of backends returned by
     * {@link #getUsedBackends()}.
     * </p>
     *
     * @return
     *
     * @see #getTiming(Backend)
     */
    @Override
    public Double[] getLabellingArray() {
        return labellingArray;
    }

    /**
     * Returns the time it took for the given backend to decide the predicate.
     * If the queried backend is not part of the used backends, null is
     * returned.
     *
     * @param backend
     *
     * @return The decision time needed from the respective backend.
     */
    public TimedAnswer getTiming(Backend backend) {
        return timings.getOrDefault(backend, null);
    }

    @Override
    public int getLabellingDimension() {
        return labellingDimension;
    }

    /**
     * Returns an array of used backends for which the times were generated.
     *
     * @return
     */
    public Backend[] getUsedBackends() {
        return usedBackends;
    }

    public static class Generator
            implements PredicateLabelGenerating<HealyTimings> {

        private final Backend[] backends;
        private final Long timeout;
        private final TimeUnit timeoutUnit;

        /**
         * Sets the timeout for predicate evaluation to default of
         * {@link HealyTimings}.
         *
         * @param backends List of backends for which the timings are
         *         calculated.
         */
        public Generator(Backend... backends) {
            this(defaultTimeout, defaultTimeoutUnit, backends);
        }

        /**
         * @param backends List of backends for which the timings are
         *         calculated.
         * @param timeout
         * @param timeoutUnit
         */
        public Generator(Long timeout, TimeUnit timeoutUnit, Backend... backends) {
            this.backends = backends;
            this.timeout = timeout;
            this.timeoutUnit = timeoutUnit;
        }

        @Override
        public HealyTimings generate(BPredicate predicate, MachineAccess machineAccess)
                throws LabelCreationException {
            return new HealyTimings(predicate, timeout, timeoutUnit, machineAccess, backends);
        }
    }

}
