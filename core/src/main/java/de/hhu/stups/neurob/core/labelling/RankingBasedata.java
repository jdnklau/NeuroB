package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Labelling to serve as a base for ranking evaluations via different means.
 * For each backend, in order, we report the values
 * - runtime in nano seconds,
 * - Response ID,
 * where the response IDs are
 *      1. valid
 *      2. invalid
 *      3. unknown
 *      4. timeout
 *      5. error
 */
public class RankingBasedata extends PredicateLabelling {

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
            LoggerFactory.getLogger(RankingBasedata.class);

    /**
     * @param predicate
     * @param timeOut Maximal runtime given for each backend to solve the predicate
     * @param timeOutUnit Unit in which the time out is measured
     * @param bMachine Access to the B machine the predicate belongs to
     * @param backends List of backend for which the labelling is
     *         created.
     */
    public RankingBasedata(BPredicate predicate, Long timeOut, TimeUnit timeOutUnit,
                           MachineAccess bMachine, Backend... backends)
            throws LabelCreationException {
        super(predicate, new Double[backends.length * 2]);

        this.usedBackends = backends;

        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;
        this.timings = createTimings(bMachine, backends);

        for (int i = 0; i < labellingDimension; i+=2) {
            labellingArray[i] = Double.valueOf(timings.get(backends[i/2]).getTime(TimeUnit.NANOSECONDS));
            int answerIndexValue = 0;
            switch (timings.get(backends[i/2]).getAnswer()) {
                case VALID:
                    answerIndexValue = 1;
                    break;
                case INVALID:
                    answerIndexValue = 2;
                    break;
                case UNKNOWN:
                    answerIndexValue = 3;
                    break;
                case TIMEOUT:
                    answerIndexValue = 4;
                    break;
                case ERROR:
                    answerIndexValue = 5;
                    break;
            }
            labellingArray[i+1] = (double) answerIndexValue;
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
    public RankingBasedata(BPredicate predicate, Long timeOut, TimeUnit timeOutUnit,
                           Map<Backend, TimedAnswer> timeMapping, Backend[] backends) {
        super(predicate, new Double[backends.length * 2]);

        this.usedBackends = backends;

        this.timeOut = timeOut;
        this.timeUnit = timeOutUnit;

        this.timings = timeMapping;

        // Distribute timings to labelling array, in order
        for (int i = 0; i < labellingDimension; i+=2) {
            if (timings.containsKey(backends[i/2])) {
                labellingArray[i] = Double.valueOf(timings.get(backends[i/2]).getTime(TimeUnit.NANOSECONDS));
                int answerIndexValue = 0;
                switch (timings.get(backends[i/2]).getAnswer()) {
                    case VALID:
                        answerIndexValue = 1;
                        break;
                    case INVALID:
                        answerIndexValue = 2;
                        break;
                    case UNKNOWN:
                        answerIndexValue = 3;
                        break;
                    case TIMEOUT:
                        answerIndexValue = 4;
                        break;
                    case ERROR:
                        answerIndexValue = 5;
                        break;
                }
                labellingArray[i+1] = (double) answerIndexValue;
            } else {
                labellingArray[i] = -1.;
                labellingArray[i+1] = 0.;
            }
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

    /**
     * Returns an array of used backends for which the times were generated.
     *
     * @return
     */
    public Backend[] getUsedBackends() {
        return usedBackends;
    }

    public static class Generator
            implements PredicateLabelGenerating<RankingBasedata> {

        private final Backend[] backends;
        private final Long timeout;
        private final TimeUnit timeoutUnit;

        /**
         * Sets the timeout for predicate evaluation to default of
         * {@link RankingBasedata}.
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
        public RankingBasedata generate(BPredicate predicate, MachineAccess machineAccess)
                throws LabelCreationException {
            return new RankingBasedata(predicate, timeout, timeoutUnit, machineAccess, backends);
        }
    }

    public static class Translator implements LabelTranslation<PredDbEntry, RankingBasedata> {

        private final Backend[] backends;

        public Translator(Backend[] backends) {
            this.backends = backends;
        }

        @Override
        public RankingBasedata translate(PredDbEntry origLabels) {
            return new RankingBasedata(origLabels.getPredicate(),
                    backends[0].getTimeOutValue(), backends[0].getTimeOutUnit(),
                    origLabels.getResults(),
                    backends
            );
        }
    }

}
