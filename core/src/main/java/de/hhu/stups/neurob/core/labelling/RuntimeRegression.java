package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.training.db.PredDbEntry;

import java.util.Map;

/**
 * Runtime regression for a single ProB backend.
 * <p>
 * To generate such labelling, use the corresponding {@link RuntimeRegression.Generator}.
 */
public class RuntimeRegression extends PredicateLabelling {

    /** Backend this label belongs to. */
    protected final Backend backend;

    /**
     * Initialises a runtime regression label for the given {@code predicate}.
     *
     * @param predicate The predicate
     * @param b The backend for which the runtime is to be output
     */
    private RuntimeRegression(BPredicate predicate, Backend b, double runtime) {
        super(predicate, runtime);
        this.backend = b;
    }

    public RuntimeRegression(String predicate, Backend b, Map<Backend, TimedAnswer> answerMap) {
        this(BPredicate.of(predicate), b, getRuntime(b, answerMap));
    }

    public RuntimeRegression(BPredicate predicate, Backend b, Map<Backend, TimedAnswer> answerMap) {
        this(predicate, b, getRuntime(b, answerMap));
    }

    /**
     * Returns the runtime of the given backend for the given predicate.
     *
     * @param b The backend for which the runtime is to be output
     * @param answerMap The map containing the runtime of each ProB backend for the specified predicate
     * @return The runtime of the given backend
     */
    private static double getRuntime(Backend b, Map<Backend, TimedAnswer> answerMap) {
        TimedAnswer answer = answerMap.get(b);
        if (answer == null) {
            throw new IllegalArgumentException("Backend " + b + " not in answer map.");
        }
        return answer.getNanoSeconds();
    }


    public static class Generator implements PredicateLabelGenerating<RuntimeRegression> {
        protected final Backend backend;

        public Generator(Backend backend) {
            this.backend = backend;
        }

        @Override
        public RuntimeRegression generate(BPredicate predicate, MachineAccess bMachine) throws LabelCreationException {
            // FIXME: the core package should not have any dependencies to the training package
            // Generate predicate data.
            PredDbEntry dbEntry = new PredDbEntry.Generator(1, backend).generate(predicate, bMachine);

            double runtime = getRuntime(backend, dbEntry.getResults());
            return new RuntimeRegression(predicate, backend, runtime);
        }

    }

}
