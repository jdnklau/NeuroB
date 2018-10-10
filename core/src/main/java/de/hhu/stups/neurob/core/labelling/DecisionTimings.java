package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Labels a predicate over different backends with how long each backend took
 * to give a decision.
 * Timings are given in nano seconds. Negative values indicate non-decidability
 * with respect to the respective backend.
 * <p>
 * If in the constructor no timeout values are set, the defaults are taken,
 * dictated by {@link DecisionTimings#defaultTimeout}
 * and {@link DecisionTimings#defaultTimeoutUnit}.
 */
public class DecisionTimings extends PredicateLabelling {

    /** Number of times each timing is run; final timing is average of all calls */
    private final int sampleSize;

    // Time out how long a predicate will maximally run in evaluation.
    private final Long timeOut;
    private final TimeUnit timeUnit;

    /** Default time out set for predicate evaluation. */
    public final static Long defaultTimeout = 20L;
    /** Unit of defaultTimeout */
    public final static TimeUnit defaultTimeoutUnit = TimeUnit.SECONDS;

    private final Backend[] usedBackends;
    private Map<Backend, Double> timings;

    private static final Logger log =
            LoggerFactory.getLogger(DecisionTimings.class);

    /**
     * Sets the default timeout for predicate evaluation.
     *
     * @param predicate
     * @param sampleSize Number of times each timing is run; final time
     *         is taken from the average.
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public DecisionTimings(BPredicate predicate, int sampleSize,
            MachineAccess bMachine,
            Backend... backends)
            throws LabelCreationException {
        this(predicate, sampleSize, defaultTimeout, defaultTimeoutUnit,
                bMachine, backends);
    }

    /**
     * Sets the default timeout for predicate evaluation.
     *
     * @param predicate
     * @param sampleSize Number of times each timing is run; final time
     *         is taken from the average.
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public DecisionTimings(String predicate, int sampleSize,
            MachineAccess bMachine,
            Backend... backends)
            throws LabelCreationException {
        this(BPredicate.of(predicate), sampleSize, defaultTimeout, defaultTimeoutUnit,
                bMachine, backends);
    }

    /**
     * @param predicate
     * @param sampleSize Number of times each timing is run; final time
     *         is taken from the average.
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public DecisionTimings(BPredicate predicate, int sampleSize,
            Long timeOut, TimeUnit timeOutUnit,
            MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        super(predicate, new Double[backends.length]);

        this.sampleSize = sampleSize;
        this.usedBackends = backends;

        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;
        this.timings = createTimings(bMachine, backends);

        for (int i = 0; i < labellingDimension; i++) {
            labellingArray[i] = timings.get(backends[i]);
        }
    }

    /**
     * @param predicate
     * @param sampleSize Number of times each timing is run; final time
     *         is taken from the average.
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public DecisionTimings(String predicate, int sampleSize,
            Long timeOut, TimeUnit timeOutUnit,
            MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        this(BPredicate.of(predicate), sampleSize,
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
    public DecisionTimings(BPredicate predicate, Map<Backend, Double> timeMapping,
            Backend... backends) {
        this(predicate, 1, defaultTimeout, defaultTimeoutUnit, timeMapping, backends);
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
    public DecisionTimings(String predicate, Map<Backend, Double> timeMapping,
            Backend... backends) {
        this(BPredicate.of(predicate), 1, defaultTimeout, defaultTimeoutUnit, timeMapping, backends);
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
     * @param sampleSize Number of times each timing was run; final time
     *         is taken from the average.
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param timeMapping Mapping of backends to time measures. Will be translated into a
     *         vector.
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public DecisionTimings(BPredicate predicate, int sampleSize,
            Long timeOut, TimeUnit timeOutUnit,
            Map<Backend, Double> timeMapping, Backend[] backends) {
        super(predicate, new Double[backends.length]);

        this.sampleSize = sampleSize;
        this.usedBackends = backends;

        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;

        this.timings = timeMapping;

        // Distribute timings to labelling array, in order
        for (int i = 0; i < labellingDimension; i++) {
            labellingArray[i] = timings.getOrDefault(backends[i], -1.);
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
     * @param sampleSize Number of times each timing was run; final time
     *         is taken from the average.
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param timeMapping Mapping of backends to time measures. Will be translated into a
     *         vector.
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public DecisionTimings(String predicate, int sampleSize,
            Long timeOut, TimeUnit timeOutUnit,
            Map<Backend, Double> timeMapping, Backend[] backends) {
        this(BPredicate.of(predicate), sampleSize, timeOut, timeOutUnit,
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
    public DecisionTimings(BPredicate predicate,
            Backend[] backends, Double... timings) {
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
    public DecisionTimings(String predicate,
            Backend[] backends, Double... timings) {
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
    public DecisionTimings(BPredicate predicate,
            int sampleSize, Long timeOut, TimeUnit timeOutUnit,
            Backend[] backends, Double... timings) {
        // bind basic settings
        super(predicate, timings);

        if (timings.length != backends.length) {
            throw new IllegalArgumentException(
                    "Number of given timings must match number of backends");
        }

        this.sampleSize = sampleSize;
        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;

        // bind timings
        this.usedBackends = backends;
        this.timings = new HashMap<>();
        for (int i = 0; i < backends.length; i++) {
            this.timings.put(backends[i], timings[i]);
        }
    }

    private Map<Backend, Double> createTimings(MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        Map<Backend, Double> timings = new HashMap<>();

        // TODO: set timeout inside of state space as well

        log.debug("Sample timings for the backends {} over predicate {}",
                backends, predicate);
        for (Backend backend : backends) {
            timings.put(backend, sampleTimings(bMachine, backend));
        }

        return timings;
    }

    private Double sampleTimings(MachineAccess bMachine, Backend backend)
            throws LabelCreationException {
        Double sampled = 0.;
        log.trace("Sampling timing over backend {}; {} times",
                backend, sampleSize);
        for (int i = 0; i < sampleSize; i++) {
            try {
                Long timing = backend.measureEvalTime(predicate, bMachine,
                        timeOut, timeUnit);
                // stop if already not decidable
                if (timing < 0) {
                    sampled = -1.;
                    break;
                }
                sampled += timing;

            } catch (FormulaException e) {
                throw new LabelCreationException(
                        "Could not create timing sample #" + i
                        + " for " + backend.toString() + "over predicate "
                        + predicate,
                        e);
            }
        }

        return sampled / sampleSize;
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
    public Double getTiming(Backend backend) {
        return timings.getOrDefault(backend, null);
    }

    @Override
    public int getLabellingDimension() {
        return labellingDimension;
    }

    /**
     * Returns number of times the predicate was queried for each backend. The
     * average
     * is taken for the final labelling.
     *
     * @return
     */
    public int getSampleSize() {
        return sampleSize;
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
            implements PredicateLabelGenerating<DecisionTimings> {

        private final int sampleSize;
        private final Backend[] backends;
        private final Long timeout;
        private final TimeUnit timeoutUnit;

        /**
         * Sets the timeout for predicate evaluation to default of
         * {@link DecisionTimings}.
         *
         * @param sampleSize Number of times each backend runs
         * @param backends List of backends for which the timings are
         *         calculated.
         */
        public Generator(int sampleSize, Backend... backends) {
            this(sampleSize, defaultTimeout, defaultTimeoutUnit, backends);
        }

        /**
         * @param sampleSize Number of times each backend runs
         * @param backends List of backends for which the timings are
         *         calculated.
         * @param timeout
         * @param timeoutUnit
         */
        public Generator(int sampleSize, Long timeout, TimeUnit timeoutUnit,
                Backend... backends) {
            this.sampleSize = sampleSize;
            this.backends = backends;
            this.timeout = timeout;
            this.timeoutUnit = timeoutUnit;
        }

        @Override
        public DecisionTimings generate(BPredicate predicate, BMachine bMachine)
                throws LabelCreationException {
            try {
                MachineAccess machineAccess = (bMachine != null)
                        ? bMachine.getMachineAccess()
                        : null;
                DecisionTimings decisionTimings = new DecisionTimings(predicate, sampleSize,
                        timeout, timeoutUnit, machineAccess, backends);
                if (machineAccess != null) {
                    bMachine.closeMachineAccess();
                }
                return decisionTimings;
            } catch (MachineAccessException e) {
                throw new LabelCreationException(
                        "Could not generate labels due to missing machine access", e);
            }
        }
    }

}
