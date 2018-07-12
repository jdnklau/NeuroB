package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.util.TheoryFeatureCollector;
import de.prob.statespace.StateSpace;

/**
 * Small feature set over B predicates. Initial feature set for NeuroB.
 * <p>
 * There are 17 features, are the number of
 * <ul>
 * <li>arithmetic operators,</li>
 * <li>comparisons,</li>
 * <li>universal quantifiers,</li>
 * <li>existential quantifiers,</li>
 * <li>conjunctions,</li>
 * <li>disjunctions,</li>
 * <li>negations,</li>
 * <li>set operations,</li>
 * <li>set memberships,</li>
 * <li>functions,</li>
 * <li>relation operators,</li>
 * <li>unique identifiers,</li>
 * <li>identifiers with finite domains,</li>
 * <li>identifiers with infinite domains,</li>
 * <li>identifier with no domain information whatsoever,</li>
 * <li>implications, and</li>
 * <li>equivalences.</li>
 * </ul>
 * </p>
 */
public class TheoryFeatures implements PredicateFeatureGenerating<PredicateFeatures> {

    public static final int featureDimension = 17;

    @Override
    public PredicateFeatures generate(String predicate, StateSpace ss) throws FeatureCreationException {
        Double[] features = TheoryFeatureCollector.collect(predicate, ss).toArray();
        return new PredicateFeatures(predicate, features);
    }

    /**
     * Generator class for creating TheoryFeatures without explicitly calling
     * any of their constructors.
     * <p>
     * An instance of this can be used in a
     * {@link de.hhu.stups.neurob.training.generation.TrainingSetGenerator}
     */
    public static class Generator
            implements PredicateFeatureGenerating<PredicateFeatures> {
        @Override
        public PredicateFeatures generate(String predicate, StateSpace ss)
                throws FeatureCreationException {
            return new TheoryFeatures().generate(predicate, ss);
        }
    }
}
