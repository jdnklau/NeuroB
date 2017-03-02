package neurob.training.generators.labelling;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import org.nd4j.linalg.util.ArrayUtil;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.util.ProblemType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.interfaces.PredicateDumpTranslator;
import neurob.training.generators.util.FormulaGenerator;
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
public class SolverSelectionGenerator implements LabelGenerator, PredicateDumpTranslator {
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
		IBEvalElement formula = FormulaGenerator.generateBCommandByMachineType(stateSpace, predicate);
		
		// Check for solvers if they can decide the predicate + get the time they need
		long ProBTime = PredicateEvaluator.getCommandExecutionTimeInNanoSeconds(stateSpace, formula);
		long KodKodTime = PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, "KODKOD", formula);
		long ProBZ3Time = PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, "SMT_SUPPORTED_INTERPRETER", formula);
		
		return getLabellingByTimes(ProBTime, KodKodTime, ProBZ3Time);
		
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
	
	private String getLabellingByTimes(long ProBTime, long KodKodTime, long ProBZ3Time){
		// check if any solver could decide the formula
		if(0 == ArrayUtil.argMax(new long[]{0, ProBTime, KodKodTime, ProBZ3Time})){
			// this means all timers returned with -1, indicating that none could decide the formula
			return "0";
		}
		
		// get fastest solver
//		double eps = 1e-3;
//		int fastestIndex = ArrayUtil.argMax(new double[]{1./(ProBTime+eps), 1./(KodKodTime+eps), 1./(ProBZ3Time+eps)});
//		// actual label will be the fastest index+1, as 0 already represents that no solver can decide it.
//		return Integer.toString(fastestIndex+1);
		// NOTE: Above code did not work, as one single solver still could be -1; we only ensured that not all of them are.
		
		double eps = 1e-3;
		double proB = 1./(ProBTime+eps);
		double kodKod = 1./(KodKodTime+eps);
		double proBZ3 = 1./(ProBZ3Time+eps);
		
		// select biggest
		if(proB >= kodKod && proB >= proBZ3){
			return "1";
		}
		else if(kodKod >= proBZ3){
			return "2";
		}
		else {
			return "3";
		}
	}

	@Override
	public String translateToCSVLabelString(ArrayList<Long> labellings) {
		return getLabellingByTimes(labellings.get(0), labellings.get(1), labellings.get(2));
	}

}
