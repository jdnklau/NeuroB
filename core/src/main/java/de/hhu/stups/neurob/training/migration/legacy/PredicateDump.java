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
    private final Map<Class<? extends Backend>, Double> timings;

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
        this.timings.put(ProBBackend.class, Double.valueOf(timingLabels[0]));
        this.timings.put(KodkodBackend.class, Double.valueOf(timingLabels[1]));
        this.timings.put(Z3Backend.class, Double.valueOf(timingLabels[2]));
        this.timings.put(SmtBackend.class, Double.valueOf(timingLabels[3]));

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

    public Path getSource() {
        return source;
    }
}
