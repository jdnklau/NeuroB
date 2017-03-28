package neurob.training.generators.labelling;

import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.statespace.StateSpace;
import neurob.core.util.ProblemType;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.PredicateLabelGenerator;
import neurob.training.generators.util.DumpData;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateEvaluator;

public class PredicateDumpLabelGenerator implements PredicateLabelGenerator {
	
	private int samplingSize;

	public PredicateDumpLabelGenerator(int samplingSize) {
		this.samplingSize = samplingSize;
	}
	
	@Override
	public int getClassCount() {
		return 4;
	}

	@Override
	public int getLabelDimension() {
		return 4;
	}

	@Override
	public ProblemType getProblemType() {
		return null;
	}

	@Override
	public double[] generateLabelling(String predicate, StateSpace stateSpace) throws NeuroBException {
		IBEvalElement formula = FormulaGenerator.generateBCommandByMachineType(stateSpace, predicate);
		
		// Check for solvers if they can decide the predicate + get the time they need
		long ProBTime = 0;
		long KodKodTime = 0;
		long Z3Time = 0;
		
		for(int sample=0; sample<samplingSize; ++sample){
			ProBTime += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, SolverType.PROB, formula);
			KodKodTime += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, SolverType.KODKOD, formula);
			Z3Time += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, SolverType.Z3, formula);
		}
		
		return new double[]{ProBTime/(double)samplingSize
							, KodKodTime/(double)samplingSize
							, Z3Time/(double)samplingSize};
	}

//	@Override
//	public TrainingDataGenerator getTrainingDataGenerator(FeatureGenerator fg) {
//		return new PredicateDumpGenerator();
//	}

	@Override
	public double[] translateLabelling(DumpData dumpData) {
		return dumpData.getLabellings().stream().mapToDouble(Long::doubleValue).toArray();
	}

}
