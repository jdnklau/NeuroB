package neurob.training.generators.labelling;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import org.datavec.api.records.reader.RecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.PredicateEvaluator;

/**
 * For given predicates, calculates a multi-labelling that indicates for each solver, whether the predicate is decidable with it.
 * <p>
 * Via constructor control what solvers shall the predicates be tested for, 
 * then continue by inputing a predicate, to generate the labelling vector.
 * <p>
 * Usage example:
 * <pre><code>
 * // set up state space
 * StateSpace stateSpace = ...
 * 
 * // What solvers should be classified for?
 * boolean classifyProB = true;
 * boolean classifyKodKod = true;
 * boolean classifyProBZ3 = false;
 * 
 * // Create SolverClassificationGenerator instance
 * SolverClassificationGenerator labelgen = new SolverClassificationGenerator(classifyProB, classifyKodKod, classifyProBZ3);
 * 
 * // Use it to solve predicates
 * String pred = "x : NAT & x > 4";
 * labels = labelgen(pred, stateSpace); // => "1,1"
 *
 *  </code></pre>
 * @author jannik
 * @see SolverSelectionGenerator
 * @See {@link #SolverClassificationGenerator(boolean, boolean, boolean)}
 *
 */
public class SolverClassificationGenerator implements LabelGenerator {
	private Api api;
	// what to classify
	private SolverType solver;

	/**
	 * Set up a classification generator for the given predicate solver.
	 * @param solver
	 */
	@Inject 
	public SolverClassificationGenerator(SolverType solver){
		api = Main.getInjector().getInstance(Api.class);
		
		this.solver = solver;
	}
	
	
	@Override
	public String getDataPathIdentifier() {
		return LabelGenerator.super.getDataPathIdentifier() + "_" + solver.name();
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
	public String generateLabelling(String predicate, StateSpace stateSpace) throws NeuroBException {
		boolean label;
		ClassicalB formula;
		// Set up formula to solve
		try {
			formula = new ClassicalB(predicate);
		} catch(Exception e) {
			throw new NeuroBException("Could not create command from formula "+predicate, e);
		}
		
		// Use specific solver
		switch(solver){
		case	PROB:
			label = PredicateEvaluator.evaluateCommandExecution(stateSpace, formula);
			break;
		case	KODKOD:
			label = PredicateEvaluator.isDecidableWithSolver(stateSpace, "KODKOD", formula);
			break;
		case	SMT_SUPPORTED_INTERPRETER:
			label = PredicateEvaluator.isDecidableWithSolver(stateSpace, "SMT_SUPPORTED_INTERPRETER", formula);
			break;
		default:
			label = false;	
		}
		
		return (label) ? "1" : "0";
		
		
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
	
	@Override
	public DataSetIterator getDataSetIterator(RecordReader recordReader, int batchSize, int featureDimension) {
		return LabelGenerator.super.getDataSetIterator(recordReader, batchSize, featureDimension);
	}

}
