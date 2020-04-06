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
import org.nd4j.linalg.util.ArrayUtil;

import java.util.Arrays;

/**
 * Classifies predicates with respect to which solver can decide them.
 *
 * <p>
 * The classes in use are
 * <ol start=0>
 *   <li> Not decidable by any of the solvers in use
 *   <li> Use ProB
 *   <li> Use Z3
 * </ol>
 *
 * <p>
 * Usage example:
 * <pre><code>
 * // set up state space
 * StateSpace stateSpace = ...
 *
 * // Create SolverClassificationGenerator instance
 * SolverSelectionGenerator labelgen = new SolverSelectionGenerator();
 *
 * // Use it to solve predicates
 * String pred = "x : NAT & x > 4";
 * labels = labelgen(pred, stateSpace); // => "1", as ProB can decide pred
 *
 *  </code></pre>
 * @author jannik
 * @see SolverClassificationGenerator
 *
 */
public class SMTSelectionGenerator implements PredicateLabelGenerator {

	@Override
	public int getClassCount() {
		/*
		 * 0 - no solver
		 * 1 - ProB
		 * 3 - Z3
		 */
		return 3;
	}

	@Override
	public int getLabelDimension() {
		return 3;
	}

	@Override
	public int getTrainingLabelDimension() {
		return 1;
	}

	@Override
	public ProblemType getProblemType(){
		return ProblemType.CLASSIFICATION;
	}

	@Override
	public double[] generateLabelling(String predicate, StateSpace stateSpace) throws NeuroBException {
		IBEvalElement formula = FormulaGenerator.generateBCommandByMachineType(stateSpace, predicate);

		// Check for solvers if they can decide the predicate + get the time they need
		long ProBTime = PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, SolverType.PROB, formula);
		long Z3Time = PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, SolverType.Z3, formula);

		return new double[]{getLabellingByTimes(ProBTime, Z3Time)};

	}

	private int getLabellingByTimes(long ProBTime,  long Z3Time){
		// check if any solver could decide the formula
		if(0 == ArrayUtil.argMax(new long[]{0, ProBTime, Z3Time})){
			// this means all timers returned with -1, indicating that none could decide the formula
			return 0;
		}

		// get fastest solver
//		double eps = 1e-3;
//		int fastestIndex = ArrayUtil.argMax(new double[]{1./(ProBTime+eps), 1./(KodKodTime+eps), 1./(Z3Time+eps)});
//		// actual label will be the fastest index+1, as 0 already represents that no solver can decide it.
//		return Integer.toString(fastestIndex+1);
		// NOTE: Above code did not work, as one single solver still could be -1; we only ensured that not all of them are.

		double eps = 1e-3;
		double proB = 1./(ProBTime+eps);
		double z3 = 1./(Z3Time+eps);

		// select biggest
		if(proB >= z3){
			return 1;
		}
		else {
			return 2;
		}
	}

	@Override
	public double[] translateLabelling(DumpData dumpData) {
		long[] labellings = Arrays.stream(dumpData.getLabellings())
				.mapToLong(d->new Double(d).longValue()).toArray();
		return new double[]{getLabellingByTimes(labellings[0], labellings[2])};
	}

}
