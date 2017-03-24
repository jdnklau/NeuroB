package neurob.training.generators;

import java.nio.file.Path;

import de.prob.statespace.StateSpace;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.labelling.PredicateDumpLabelGenerator;
import neurob.training.generators.util.TrainingData;

public class PredicateDumpGenerator extends PredicateTrainingDataGenerator {

	public PredicateDumpGenerator() {
		this(3);
	}
	
	public PredicateDumpGenerator(int samplingSize) {
		super(null, new PredicateDumpLabelGenerator(samplingSize));
		preferedFileExtension = "pdump";
	}
	
	@Override
	protected String generateOutput(TrainingData td){
		return td.getLabelString()+":"+td.getComment();
	}
	
	@Override
	protected TrainingData setUpTrainingData(String predicate, Path source, StateSpace ss) throws NeuroBException {
		return new TrainingData(null, lg.generateLabelling(predicate, ss), source, predicate);
	}

}
