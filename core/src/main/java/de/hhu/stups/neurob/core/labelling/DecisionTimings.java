package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.prob.statespace.StateSpace;
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

    private final Double[] timingArray;
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

    private final int labellingDimension;

    private static final Logger log =
            LoggerFactory.getLogger(DecisionTimings.class);

    /**
     * Sets the default timeout for predicate evaluation.
     *
     * @param predicate
     * @param sampleSize Number of times each timing is run; final time
     *         is taken from the average.
     * @param stateSpace StateSpace over which the predicate shall be
     *         decided.
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public DecisionTimings(String predicate, int sampleSize,
            StateSpace stateSpace,
            Backend... backends)
            throws LabelCreationException {
        this(predicate, sampleSize, defaultTimeout, defaultTimeoutUnit,
                stateSpace, backends);
    }

    /**
     * @param predicate
     * @param sampleSize Number of times each timing is run; final time
     *         is taken from the average.
     * @param stateSpace StateSpace over which the predicate shall be
     *         decided.
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public DecisionTimings(String predicate, int sampleSize,
            Long timeOut, TimeUnit timeOutUnit,
            StateSpace stateSpace, Backend... backends)
            throws LabelCreationException {
        super(predicate);

        this.sampleSize = sampleSize;
        this.usedBackends = backends;

        this.labellingDimension = backends.length;

        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;
        this.timings = createTimings(stateSpace, backends);

        this.timingArray = new Double[labellingDimension];
        for (int i = 0; i < labellingDimension; i++) {
            timingArray[i] = timings.get(backends[i]);
        }
    }

    private Map<Backend, Double> createTimings(StateSpace ss, Backend... backends)
            throws LabelCreationException {
        Map<Backend, Double> timings = new HashMap<>();

        // TODO: set timeout inside of state space as well

        for (Backend backend : backends) {
            timings.put(backend, sampleTimings(ss, backend));
        }

        return timings;
    }

    private Double sampleTimings(StateSpace ss, Backend backend)
            throws LabelCreationException {
        Double sampled = 0.;
        for (int i = 0; i < sampleSize; i++) {
            Long timing = null;
            try {
                timing = backend.measureEvalTime(predicate, ss,
                        timeOut, timeUnit);
            } catch (FormulaException e) {
                throw new LabelCreationException(
                        "Could not create timing sample #" + i
                        + " for " + backend.toString() + "over predicate "
                        + predicate,
                        e);
            }
            // stop if already not decidable
            if (timing < 0) {
                sampled = -1.;
                break;
            }
            sampled += timing;
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
        return timingArray;
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
        public DecisionTimings generate(String predicate, StateSpace ss)
                throws LabelCreationException {
            return new DecisionTimings(predicate, sampleSize,
                    timeout, timeoutUnit, ss, backends);
        }
    }

}
