package de.hhu.stups.neurob.training.migration.legacy;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Predicate dumps are a collection of predicates and their associated solving times
 * over ProB, Kodkod, Z3, and SMT_SUPPORTED_INTERPRETER.
 */
public class PredicateDump {

    private final BPredicate predicate;
    private final Path source;
    private final Map<Backend, Double> timings;

    /**
     * Array over backends in use, also providing an ordering by id.
     * 0 - ProB
     * 1 - Kodkod
     * 2 - Z3
     * 3 - SMT_SUPPORTED_INTERPRETER
     */
    public final static Backend[] BACKENDS_USED = {
            new ProBBackend(),
            new KodkodBackend(),
            new Z3Backend(),
            new SmtBackend(),
    };
    public static final Backend PROB = BACKENDS_USED[0];
    public static final Backend KODKOD = BACKENDS_USED[1];
    public static final Backend Z3 = BACKENDS_USED[2];
    public static final Backend SMT = BACKENDS_USED[3];
    // Define a helper to make use of Backend.class access for times
    private static final Map<Class<? extends Backend>, Backend> classInstanceMapping;

    static {
        classInstanceMapping = new HashMap<>();
        classInstanceMapping.put(ProBBackend.class, PROB);
        classInstanceMapping.put(KodkodBackend.class, KODKOD);
        classInstanceMapping.put(Z3Backend.class, Z3);
        classInstanceMapping.put(SmtBackend.class, SMT);
    }

    public PredicateDump(String predicateDumpEntry) {
        this(predicateDumpEntry, null);
    }

    /**
     * @param predicateDumpEntry
     * @param sourceMachine
     */
    public PredicateDump(String predicateDumpEntry, Path sourceMachine) {
        // Split entry into labels and predicates
        int splitPos = predicateDumpEntry.indexOf(':');

        this.predicate = new BPredicate(predicateDumpEntry.substring(splitPos + 1));
        this.source = sourceMachine;

        // Get timings
        this.timings = new HashMap<>();
        String[] timingLabels = predicateDumpEntry.substring(0, splitPos).split(",");
        this.timings.put(PROB, Double.valueOf(timingLabels[0]));
        this.timings.put(KODKOD, Double.valueOf(timingLabels[1]));
        this.timings.put(Z3, Double.valueOf(timingLabels[2]));
        this.timings.put(SMT, Double.valueOf(timingLabels[3]));
    }

    public BPredicate getPredicate() {
        return predicate;
    }

    /**
     * Accesses the time of the given Backend from this predicate dump entry.
     * <p>
     * Possible values are:
     * {@code ProBBackend.class},
     * {@code KodkodBackend.class},
     * {@code Z3Backend.class}, and
     * {@code SmtBackend.class}.
     *
     * @param backend
     *
     * @return
     */
    public Double getTime(Class<? extends Backend> backend) {
        return timings.get(backend);
    }

    /**
     * Returns a map of backend and timing pairs.
     * <p>
     * The backends (keys in the map) are
     * {@link #PROB}, {@link #KODKOD}, {@link #Z3}, and {@link #SMT}.
     * <p>
     * The timing is in nanoseconds.
     *
     * @return
     */
    public Map<Backend, Double> getTimings() {
        return timings;
    }

    public Path getSource() {
        return source;
    }
}
