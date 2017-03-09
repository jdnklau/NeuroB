package neurob.training.generators;

import de.prob.statespace.StateSpace;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.labelling.SolverTimerGenerator;

public class PredicateDumpGenerator extends PredicateTrainingDataGenerator {

	public PredicateDumpGenerator() {
		super(null, new SolverTimerGenerator(3));
		preferedFileExtension = "pdump";
	}
	
	@Override
	protected String generateOutput(String predicate, StateSpace ss) throws NeuroBException {
		return lg.generateLabelling(predicate, ss)+":"+predicate;
	}

}
