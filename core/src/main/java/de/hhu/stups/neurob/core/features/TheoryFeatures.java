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
public class TheoryFeatures implements PredicateFeatures {

    public static final int featureDimension = 17;

    /** The StateSpaces serving as context for the predicate and may be null */
    private final StateSpace ss;
    private final String predicate;
    private final Double[] featureArray;


    public TheoryFeatures(String predicate) throws FeatureCreationException {
        this(predicate, null);
    }

    /**
     * Create new TheoryFeatures from the given predicate.
     * The state space <code>stateSpace</code> hereby serves as a context
     * for the predicate, as it needs to be parsed for feature extraction.
     *
     * @param predicate
     * @param stateSpace
     */
    public TheoryFeatures(String predicate, StateSpace stateSpace) throws FeatureCreationException {
        this.predicate = predicate;
        this.ss = stateSpace;

        // Create features
        featureArray =
                TheoryFeatureCollector.collect(predicate, stateSpace).toArray();

    }

    @Override
    public int getFeatureDimension() {
        return featureDimension;
    }

    @Override
    public Double[] getFeatureArray() {
        return featureArray;
    }

    @Override
    public String toString() {
        return getFeatureString();
    }

    @Override
    public String getPredicate() {
        return predicate;
    }

    /**
     * Generator class for creating TheoryFeatures without explicitly calling
     * any of their constructors.
     * <p>
     * An instance of this can be used in a
     * {@link de.hhu.stups.neurob.training.generation.TrainingDataGenerator}.
     */
    public static class Generator
            implements PredicateFeatureGenerating<TheoryFeatures> {
        @Override
        public TheoryFeatures generate(String predicate, StateSpace ss)
                throws FeatureCreationException {
            return new TheoryFeatures(predicate, ss);
        }
    }
}
