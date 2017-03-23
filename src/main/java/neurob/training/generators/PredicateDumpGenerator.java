package neurob.training.generators;

import de.prob.statespace.StateSpace;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.labelling.PredicateDumpLabelGenerator;

public class PredicateDumpGenerator extends PredicateTrainingDataGenerator {

	public PredicateDumpGenerator() {
		this(3);
	}
	
	public PredicateDumpGenerator(int samplingSize) {
		super(null, new PredicateDumpLabelGenerator(samplingSize));
		preferedFileExtension = "pdump";
	}
	
	@Override
	protected String generateOutput(String predicate, StateSpace ss) throws NeuroBException {
		return lg.generateLabellingString(predicate, ss)+":"+predicate;
	}

}
