package neurob.training.generators.labelling;

import java.io.IOException;
import java.nio.file.Path;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.Main;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
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
		return 1;
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
		
		// get classification of predicate
		if(PredicateEvaluator.evaluateCommandExecution(stateSpace, formula)){
			// Decidable with ProB
			return "1"; 
		}
		
		if(PredicateEvaluator.isDecidableWithSolver(stateSpace, "KODKOD", formula)){
			// Deidable with KodKod
			return "2";
		}
		
		if(PredicateEvaluator.isDecidableWithSolver(stateSpace, "SMT_SUPPORTED_INTERPRETER", formula)){
			return "3";
		}
		
		// Not decidable with solvers in use
		return "0";
		
	}

	@Override
	public String generateLabelling(String predicate, Path b_machine) throws NeuroBException {
		// setup up state space
		StateSpace ss;
		try {
			ss = api.b_load(b_machine.toString());
		} catch (IOException e) {
			throw new NeuroBException("Could not access file: "+b_machine.toString(), e);
		} catch (BException e) {
			throw new NeuroBException("Could not access file: "+b_machine.toString(), e);
		}
		
		// Use other method to calculate labelling
		String labelling = generateLabelling(predicate, ss);
		
		ss.kill();
		// return
		return labelling;
	}

}
