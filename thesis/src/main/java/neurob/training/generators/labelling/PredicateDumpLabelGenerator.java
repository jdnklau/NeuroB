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

	/**
	 * Number of solvers the predicate dump considers and generates labellings for
	 */
	public static final int solversAccountedFor = 4;
	/**
	 * Array indexing the order of solvers used. The {@code i}-th entry in the label vector
	 * corresponds to the solver stated in {@code solverOrder[i]}.
	 */
	public static final SolverType[] solverOrder =
		{SolverType.PROB, SolverType.KODKOD, SolverType.Z3, SolverType.SMT_SUPPORTED_INTERPRETER};

	private int samplingSize;

	public PredicateDumpLabelGenerator(int samplingSize) {
		this.samplingSize = samplingSize;
	}

	/**
	 * For a given solver, returns the index of said solver at which it appears in the generated
	 * labelling. Indexing starts at 0.
	 * @param solver Solver to get index position of
	 * @return Index of given solver in labelling
	 */
	public static int getSolverIndex(SolverType solver){
		int index = 0; // set initially to 0
		for(SolverType s : PredicateDumpLabelGenerator.solverOrder){
			if (s!=solver){
				// if solver with currently set index is not the right one, use next index
				index++;
			} else {
				// if solver was found, leave index unchanged and exit loop
				break;
			}
		}
		return index;
	}

	@Override
	public int getClassCount() {
		return -1;
	}

	@Override
	public int getLabelDimension() {
		return 3;
	}

	@Override
	public int getTrainingLabelDimension() {
		return 3;
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
		long SMTSupportedTime = 0;

		for(int sample=0; sample<samplingSize; ++sample){
			ProBTime += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, SolverType.PROB, formula);
			KodKodTime += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, SolverType.KODKOD, formula);
			Z3Time += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, SolverType.Z3, formula);
			SMTSupportedTime += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace,
					SolverType.SMT_SUPPORTED_INTERPRETER, formula);
		}

		return new double[]{ProBTime/(double)samplingSize
							, KodKodTime/(double)samplingSize
							, Z3Time/(double)samplingSize
							, SMTSupportedTime/(double)samplingSize
							};
	}

//	@Override
//	public TrainingDataGenerator getTrainingDataGenerator(FeatureGenerator fg) {
//		return new PredicateDumpGenerator();
//	}

	@Override
	public double[] translateLabelling(DumpData dumpData) {
		return dumpData.getLabellings();
	}

}
