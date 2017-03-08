package neurob.training.generators.interfaces;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.training.generators.PredicateTrainingDataGenerator;
import neurob.training.generators.TrainingDataGenerator;

public interface PredicateLabelGenerator extends LabelGenerator {
	
	@Override
	default TrainingDataGenerator getTrainingDataGenerator(FeatureGenerator fg) {
		return new PredicateTrainingDataGenerator(fg, this);
	}

}
