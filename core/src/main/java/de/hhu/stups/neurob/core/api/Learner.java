package de.hhu.stups.neurob.core.api;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.training.data.TrainingSample;

import java.util.List;

/**
 * A Learner is a machine learning algorithm.
 * <p>
 *     This can be understood as a learned function from a
 *     data space {@link Features} into a label space {@link Labelling}.
 * </p>
 * <p>
 *     Usage of this is to firstly call {@link #fit} for training.
 *     With {@link #predict(Features)}, a prediction is calculated based
 *     on the current knowledge.
 * </p>
 */
public interface Learner<F extends Features, L extends Labelling> {

    void fit(List<TrainingSample<F,L>> data);

    Double[] predict(F sample);

}
