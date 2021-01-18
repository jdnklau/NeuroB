package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;

import java.util.Arrays;
import java.util.Map;

/**
 * Multi-class classification between ProB backends.
 * <p>
 * The classification happens over the backends used at initialisation
 * and {@code null}.
 * The associated class corresponds to the backend able to find an answer for a given predicate
 * the fastest. If no backend can return an answer, the predicate is classified as unknown to all,
 * represented as the {@code null} backend in {@link #getBackendFromClassIndex(int)}.
 * <p>
 * To generate such labelling, use the corresponding {@link Generator}.
 */
public class BackendClassification extends PredicateLabelling {
    /** Selection of backends used in this classification task. */
    protected final Backend[] backends;
    /** Backend this label belongs to. */
    protected final Backend backend;
    protected final int classCount;

    /**
     * Initialises a backend classification label for the given {@code predicate}.
     * If none of the specified backends is able to find an answer for the predicate,
     * set {@code b} to {@code null}.
     *
     * @param predicate The classified predicate
     * @param backends Selection of backends over which the classification takes place.
     * @param b The backend the predicate is classified as.
     */
    public BackendClassification(BPredicate predicate, Backend[] backends, Backend b) {
        super(predicate, (double) getIndexByBackend(b, backends));
        this.backends = backends;
        this.classCount = backends.length + 1; // +1 for null backend.
        this.backend = b;
    }

    public BackendClassification(BPredicate predicate, Backend[] backends,
            Map<Backend, TimedAnswer> answerMap) {
        this(predicate, backends, classifyFastestBackend(backends, answerMap));
    }

    /**
     * Returns the classification index of the given backend assuming the
     * specified classification backends.
     *
     * @param backend Classified backend.
     * @param classificationBackends Ordered collection of backends the classification was
     *         conducted over.
     *
     * @return Index from 0 to classificationBackends.length
     */
    static int getIndexByBackend(Backend backend, Backend[] classificationBackends) {
        if (backend == null) {
            return 0;
        }

        for (int i = 0; i < classificationBackends.length; i++) {
            if (classificationBackends[i].equals(backend)) {
                return i + 1; // Beware of offset for null backend.
            }
        }

        throw new IllegalArgumentException(
                "Backend " + backend + " not part of the classification.");
    }

    static Backend classifyFastestBackend(Backend[] classificationBackends,
            Map<Backend, TimedAnswer> answerMap) {
        Backend fastest = null;
        long fastestTime = Long.MAX_VALUE;
        Answer fastestAnswer = Answer.ERROR;

        for (int i = 0; i < classificationBackends.length; i++) {
            Backend backend = classificationBackends[i];
            TimedAnswer answer = answerMap.get(backend);

            if (answer == null) {
                throw new IllegalArgumentException("Backend " + backend + " not in answer map.");
            }

            Answer answerValue = answer.getAnswer();
            Long time = answer.getNanoSeconds();
            if (Answer.isSolvable(answerValue)
                && (time < fastestTime || !Answer.isSolvable(fastestAnswer))) {
                fastest = classificationBackends[i];
                fastestTime = time;
                fastestAnswer = answerValue;
            } else if (!Answer.isSolvable(fastestAnswer)
                       && time < fastestTime) {
                fastest = classificationBackends[i];
                fastestTime = time;
                fastestAnswer = answerValue;
            }
        }

        return fastest;
    }

    static int getClassificationIndexFromMap(Backend[] classificationBackends,
            Map<Backend, TimedAnswer> answerMap) {
        Backend fastest = classifyFastestBackend(classificationBackends, answerMap);
        return getIndexByBackend(fastest, classificationBackends);
    }

    /**
     * Returns a {@link Backend} corresponding to the given class index.
     * <p>
     * The index 0 always corresponds to {@code null} and matches the case
     * in which no backend was able to find an answer for the predicate.
     * <p>
     * Throws an {@link IllegalArgumentException} when the class index is not available.
     *
     * @param index
     *
     * @return
     */
    public Backend getBackendFromClassIndex(int index) {
        return BackendClassification.getBackendFromClassIndex(backends, index);
    }

    /**
     * Returns a {@link Backend} corresponding to the given class index.
     * <p>
     * The index 0 always corresponds to {@code null} and matches the case
     * in which no backend was able to find an answer for the predicate.
     * <p>
     * Throws an {@link IllegalArgumentException} when the class index is not available.
     *
     * @param backends Ordered array of backends the index associates to
     * @param index
     *
     * @return
     */
    static public Backend getBackendFromClassIndex(Backend[] backends, int index) {
        int classCount = backends.length + 1; // +1 for null backend
        if (index == 0) {
            // 0 corresponds to the case that no backend could find an answer
            return null;
        } else if (index > classCount) {
            throw new IllegalArgumentException("Cannot retrieve Backend for class index "
                                               + index + ": No such class, only " + classCount + " classes available.");
        }

        return backends[index - 1];
    }

    public Backend getBackend() {
        return backend;
    }

    /**
     * Returns the ordered backends used for the classification belonging to this labelling.
     *
     * @return Array of backends.
     */
    public Backend[] getBackendsFromClassification() {
        return backends;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BackendClassification) {
            BackendClassification other = (BackendClassification) o;
            return super.equals(o)
                   && Arrays.equals(this.backends, other.backends)
                   && this.backend.equals(other.backend);
        }

        return false;
    }

    public static class Generator implements PredicateLabelGenerating<BackendClassification> {
        private final Backend[] backends;

        public Generator(Backend[] backends) {
            this.backends = backends;
        }

        @Override
        public BackendClassification generate(BPredicate predicate, MachineAccess bMachine) throws LabelCreationException {
            // FIXME: the core package should not have any dependencies to the training package
            // Generate predicate data.
            PredDbEntry dbEntry = new PredDbEntry.Generator(1, backends).generate(predicate, bMachine);

            Backend classification = classifyFastestBackend(backends, dbEntry.getResults());

            // If fastest is an error, we want to classify as index 0 (null backend).
            classification = (Answer.isSolvable(dbEntry.getResult(classification).getAnswer()))
                    ? classification
                    : null;
            return new BackendClassification(predicate, backends, classification);
        }

    }

    public static class Translator implements LabelTranslation<PredDbEntry, BackendClassification> {
        private final Backend[] backends;

        public Translator(Backend[] backends) {
            this.backends = backends;
        }

        @Override
        public BackendClassification translate(PredDbEntry origLabels) {

            int groundTruth = 0;
            Long fastest = Long.MAX_VALUE;

            TimedAnswer[] answerArray = origLabels.getAnswerArray(backends);

            for(int i = 0; i<answerArray.length; i++) {
                TimedAnswer timed = answerArray[i];
                Answer a = timed.getAnswer();
                if (a.equals(Answer.ERROR) || a.equals(Answer.TIMEOUT) || a.equals(Answer.UNKNOWN)) {
                    continue;
                }

                if (fastest > timed.getNanoSeconds()) {
                    fastest = timed.getNanoSeconds();
                    groundTruth = i + 1;
                }
            }

            Backend fastestBackend = (groundTruth == 0)
                    ? null
                    : backends[groundTruth-1];

            return new BackendClassification(origLabels.getPredicate(),
                    backends,
                    fastestBackend);
        }
    }
}
