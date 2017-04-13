package neurob.core.features;

import neurob.training.generators.PredicateTrainingImageGenerator;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.interfaces.LabelGenerator;

public class PredicateImages extends CodeImages {
	
	public PredicateImages(int dimension) {
		super(dimension);
	}

	@Override
	public TrainingDataGenerator getTrainingDataGenerator(LabelGenerator lg) {
		return new PredicateTrainingImageGenerator(this, lg);
	}
	
}
