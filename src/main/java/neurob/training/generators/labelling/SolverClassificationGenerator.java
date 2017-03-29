package neurob.training.generators.labelling;

import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.statespace.StateSpace;
import neurob.core.util.ProblemType;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.PredicateLabelGenerator;
import neurob.training.generators.util.DumpData;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateEvaluator;

/**
 * For given predicates, calculates a label that indicates if a given solver can decide it.
 * <p>
 * Via constructor control what solver the predicates shall be tested for, 
 * then continue by inputing a predicate, to generate the labelling vector.
 * <p>
 * Usage example:
 * <pre><code>
 * // set up state space
 * StateSpace stateSpace = ...
 * 
 * // Create SolverClassificationGenerator instance
 * SolverClassificationGenerator labelgen = new SolverClassificationGenerator(SolverType.PROB);
 * 
 * // Use it to solve predicates
 * String pred = "x : NAT & x > 4";
 * labels = generateLabelling(pred, stateSpace); // => "1"
 *
 *  </code></pre>
 * @author jannik
 * @see SolverSelectionGenerator
 * @See {@link #SolverClassificationGenerator(boolean, boolean, boolean)}
 *
 */
public class SolverClassificationGenerator implements PredicateLabelGenerator {
	// what to classify
	private SolverType solver;

	/**
	 * Set up a classification generator for the given predicate solver.
	 * @param solver
	 */
	@Inject 
	public SolverClassificationGenerator(SolverType solver){
		this.solver = solver;
	}
	
	
	@Override
	public String getDataPathIdentifier() {
		return PredicateLabelGenerator.super.getDataPathIdentifier() + "_" + solver.name();
	}

	@Override
	public int getClassCount() {
		return 2;
	}

	@Override
	public int getLabelDimension() {
		return 2;
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
		boolean label;
		IBEvalElement formula = FormulaGenerator.generateBCommandByMachineType(stateSpace, predicate);
		
		// Use specific solver
		label = PredicateEvaluator.isDecidableWithSolver(stateSpace, solver, formula);
		
		double val = (label) ? 1. : 0.; 
		
		return new double[]{val};
		
		
	}
	
	@Override
	public double[] translateLabelling(DumpData dumpData) {
		List<Long> labellings = dumpData.getLabellings();
		// Label to be picked depends on solver type
		Long label;
		switch (solver) {
		case KODKOD:
			label = labellings.get(1);
			break;
		case Z3:
			label = labellings.get(2);
			break;
		case SMT_SUPPORTED_INTERPRETER:
			label = labellings.get(3);
			break;
		case PROB:
		default: // defaulting to ProB
			label = labellings.get(0);
			break;
		}
		double val = (label >= 0) ? 1 : 0; 
		return new double[]{val};
	}

}
