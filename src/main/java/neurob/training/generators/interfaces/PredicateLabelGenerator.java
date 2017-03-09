package neurob.training.generators.interfaces;

import java.nio.file.Path;

import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.PredicateTrainingDataGenerator;
import neurob.training.generators.TrainingDataGenerator;

public interface PredicateLabelGenerator extends LabelGenerator {
	
	@Override
	default TrainingDataGenerator getTrainingDataGenerator(FeatureGenerator fg) {
		return new PredicateTrainingDataGenerator(fg, this);
	}
	
	/**
	 * Generates the labelling for the given predicate with respect to the given StateSpace.
	 * @param predicate The predicate to calculate the labelling for
	 * @param stateSpace The state space, that may be taken into account whilst label generation
	 * @return The generated label vector as String
	 * @throws NeuroBException
	 */
	public String generateLabelling(String predicate, StateSpace stateSpace) throws NeuroBException;
	
//	/**
//	 * Generates the labelling for the given predicate with respect to the given B Machine file.
//	 * <p>
//	 * <b>Important</b>: It is encouraged to use {@link #generateLabelling(String, StateSpace)} instead
//	 * and to handle the opening of the B Machine file by yourself; especially if you are evaluating different predicates 
//	 * in the context of the same machine file.
//	 * @param predicate The predicate to calculate the labelling for
//	 * @param b_machine The B machine file, that may be taken into account whilst label generation
//	 * @return The generated label vector as String
//	 * @throws NeuroBException
//	 */
//	public String generateLabelling(String predicate, Path b_machine) throws NeuroBException;

}
