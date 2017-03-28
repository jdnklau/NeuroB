package neurob.training.generators;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.training.generators.interfaces.LabelGenerator;

public class PredicateTrainingImageGenerator extends PredicateTrainingDataGenerator {

	public PredicateTrainingImageGenerator(FeatureGenerator fg, LabelGenerator lg) {
		super(fg, lg);
	}

}
