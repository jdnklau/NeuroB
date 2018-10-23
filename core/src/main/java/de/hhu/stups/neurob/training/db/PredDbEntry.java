package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PredDbEntry extends PredicateLabelling {

    private final BPredicate pred;
    private final BMachine source;
    private final Map<Backend, TimedAnswer> results;

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

    /**
     * Initialises this entry with the given results.
     *
     * @param pred Predicate over which the results were gathered
     * @param source Source machine from which the predicate originates
     * @param results Map of backends with corresponding results
     */
    public PredDbEntry(BPredicate pred, BMachine source, Map<Backend, TimedAnswer> results) {
        super(pred, toArray(results, BACKENDS_USED));
        this.pred = pred;
        this.source = source;
        this.results = results;
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
                .map(time -> time == null ? null : time.getTime().doubleValue())
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

    public BPredicate getPredicate() {
        return pred;
    }

    public BMachine getSource() {
        return source;
    }

    public Map<Backend, TimedAnswer> getResults() {
        return results;
    }
}
