package neurob.training.generators.labelling;

import java.io.IOException;
import java.nio.file.Path;

import org.nd4j.linalg.util.ArrayUtil;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.util.ProblemType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.PredicateEvaluator;

/**
 * Classifies predicates with respect to which solver can decide them.
 * 
 * <p>
 * The classes in use are
 * <ol start=0>
 *   <li> Not decidable by any of the solvers in use
 *   <li> Use ProB
 *   <li> Use KodKod
 *   <li> Use ProB+Z3 
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
public class SolverSelectionGenerator implements LabelGenerator {
	private Api api;
	
	
	@Inject
	public SolverSelectionGenerator() {
		api = Main.getInjector().getInstance(Api.class);
	}

	@Override
	public int getClassCount() {
		/*
		 * 0 - no solver
		 * 1 - ProB
		 * 2 - KodKod
		 * 3 - ProBZ3
		 */
		return 4;
	}

	@Override
	public int getLabelDimension() {
		return 4;
	}
	
	@Override
	public ProblemType getProblemType(){
		return ProblemType.CLASSIFICATION;
	}

	@Override
	public String generateLabelling(String predicate, StateSpace stateSpace) throws NeuroBException {
		ClassicalB formula;
		// Set up formula to solve
		try {
			formula = new ClassicalB(predicate);
		} catch(Exception e) {
			throw new NeuroBException("Could not create command from formula "+predicate, e);
		}
		
		// Check for solvers if they can decide the predicate + get the time they need
		long ProBTime = PredicateEvaluator.getCommandExecutionTimeInNanoSeconds(stateSpace, formula);
		long KodKodTime = PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, "KODKOD", formula);
		long ProBZ3Time = PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, "SMT_SUPPORTED_INTERPRETER", formula);
		
		// check if any solver could decide the formula
		if(0 == ArrayUtil.argMax(new long[]{0, ProBTime, KodKodTime, ProBZ3Time})){
			// this means all timers returned with -1, indicating that none could decide the formula
			return "0";
		}
		
		// get fastest solver
		int fastestIndex = ArrayUtil.argMin(new long[]{ProBTime, KodKodTime, ProBZ3Time});
		// actual label will be the fastest index+1, as 0 already represents that no solver can decide it.
		return Integer.toString(fastestIndex+1);
		
	}

	@Override
	public String generateLabelling(String predicate, Path b_machine) throws NeuroBException {
		// setup up state space
		StateSpace ss;
		try {
			ss = api.b_load(b_machine.toString());
		} catch (IOException e) {
			throw new NeuroBException("Could not access file: "+b_machine.toString(), e);
		} catch (ModelTranslationError e) {
			throw new NeuroBException("Could not translate model: "+b_machine.toString(), e);
		}
		
		// Use other method to calculate labelling
		String labelling = generateLabelling(predicate, ss);
		
		ss.kill();
		// return
		return labelling;
	}

}
