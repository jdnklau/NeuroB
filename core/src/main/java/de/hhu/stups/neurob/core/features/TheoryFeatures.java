package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.core.features.util.TheoryFeatureCollector;

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
public class TheoryFeatures extends PredicateFeatures {

    public static final int featureDimension = 17;

    /**
     * Generates the TheoryFeatures from the given predicate.
     *
     * @param predicate
     *
     * @throws FeatureCreationException
     */
    public TheoryFeatures(BPredicate predicate) throws FeatureCreationException {
        this(predicate, (BMachine) null);
    }

    /**
     * Generates the TheoryFeatures from the given predicate.
     *
     * @param predicate
     *
     * @throws FeatureCreationException
     */
    public TheoryFeatures(String predicate) throws FeatureCreationException {
        this(BPredicate.of(predicate), (BMachine) null);
    }

    /**
     * Generates the TheoryFeatures from the given predicate in the
     * given B machine.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate gets decided over
     *
     * @throws FeatureCreationException
     */
    public TheoryFeatures(BPredicate predicate, BMachine bMachine)
            throws FeatureCreationException {
        super(predicate, new Generator().generateArray(predicate, bMachine));
    }

    /**
     * Generates the TheoryFeatures from the given predicate in the
     * given B machine.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate gets decided over
     *
     * @throws FeatureCreationException
     */
    public TheoryFeatures(String predicate, BMachine bMachine)
            throws FeatureCreationException {
        this(BPredicate.of(predicate), bMachine);
    }

    /**
     * Wraps the given feature array in a TheoryFeature instance.
     * The features must be exactly {@link #featureDimension} entries long.
     *
     * @param features
     */
    public TheoryFeatures(Double... features) {
        this((BPredicate) null, features);
    }

    /**
     * Wraps the given feature array in a TheoryFeature instance.
     * The features must be exactly {@link #featureDimension} entries long.
     *
     * @param predicate
     * @param features
     */
    public TheoryFeatures(String predicate, Double... features) {
        this(BPredicate.of(predicate), features);
    }

    /**
     * Wraps the given feature array in a TheoryFeature instance.
     * The features must be exactly {@link #featureDimension} entries long.
     *
     * @param predicate
     * @param features
     */
    public TheoryFeatures(BPredicate predicate, Double... features) {
        super(predicate, features);
        // Check feature length
        if (features.length != featureDimension) {
            throw new IllegalArgumentException(
                    "TheoryFeatures need exactly "
                    + featureDimension + " entries, "
                    + "but received " + features.length);
        }
    }

    public static TheoryFeatures generate(BPredicate predicate)
            throws FeatureCreationException {
        return generate(predicate, null);
    }

    public static TheoryFeatures generate(BPredicate predicate, BMachine bMachine)
            throws FeatureCreationException {
        return new Generator().generate(predicate, bMachine);
    }

    public static TheoryFeatures generate(String predicate)
            throws FeatureCreationException {
        return generate(BPredicate.of(predicate), null);
    }

    public static TheoryFeatures generate(String predicate, BMachine bMachine)
            throws FeatureCreationException {
        return generate(BPredicate.of(predicate), bMachine);
    }

    /**
     * Generator class for creating TheoryFeatures without explicitly calling
     * any of their constructors.
     * <p>
     * An instance of this can be used in a
     * {@link de.hhu.stups.neurob.training.generation.TrainingSetGenerator}
     */
    public static class Generator
            implements PredicateFeatureGenerating<TheoryFeatures> {
        @Override
        public TheoryFeatures generate(BPredicate predicate, BMachine bMachine)
                throws FeatureCreationException {
            return new TheoryFeatures(predicate, generateArray(predicate, bMachine));
        }

        public Double[] generateArray(BPredicate predicate, BMachine bMachine)
                throws FeatureCreationException {
            try {
                MachineAccess machineAccess = (bMachine != null)
                        ? bMachine.getMachineAccess()
                        : null;
                return TheoryFeatureCollector.collect(predicate, machineAccess).toArray();
            } catch (MachineAccessException e) {
                throw new FeatureCreationException(
                        "Could not extract features over B machine " + bMachine, e);
            }
        }

    }
}
