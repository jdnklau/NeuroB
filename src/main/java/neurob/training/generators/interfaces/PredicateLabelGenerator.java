package neurob.training.generators.interfaces;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.training.generators.PredicateTrainingDataGenerator;
import neurob.training.generators.TrainingDataGenerator;

public interface PredicateLabelGenerator extends LabelGenerator {
	
	/*
	 * (non-Javadoc)
	 * 
	 * Convenience implementation for predicate related label generators, so different ones do not need to reimplement
	 * this method each.
	 * 
	 * @see neurob.training.generators.interfaces.LabelGenerator#getTrainingDataGenerator(neurob.core.features.interfaces.FeatureGenerator)
	 */
	@Override
	default TrainingDataGenerator getTrainingDataGenerator(FeatureGenerator fg) {
		return new PredicateTrainingDataGenerator(fg, this);
	}

}
