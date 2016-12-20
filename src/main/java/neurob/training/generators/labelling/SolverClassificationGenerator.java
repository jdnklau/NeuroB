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
	private int labelledClasses; // How many classes are differentiated
	private Api api;
	// what to classify
	private boolean decideProB;
	private boolean decideKodKod;
	private boolean decideProBZ3;
	
	/**
	 * Set up a solver classifier, allowing to generate a multi-labelling approach for a given predicate.
	 * <p> 
	 * The multi-labelling indicates for ProB, KodKod, and a PoB/Z3 combination, whether the given predicate is
	 * decidable by the respective solver.
	 * One can decide which solvers to check for, by passing the parameters in the constructor accordingly.
	 * @param classifyProB Set to true, if the decidability of the predicate with respect to ProB should be classified
	 * @param classifyKodKod Set to true, if the decidability of the predicate with respect to KodKod should be classified
	 * @param classifyProBZ3 Set to true, if the decidability of the predicate with respect to ProB+Z3 should be classified
	 */
	@Inject
	public SolverClassificationGenerator(boolean classifyProB, boolean classifyKodKod, boolean classifyProBZ3) {
		api = Main.getInjector().getInstance(Api.class);
		
		decideProB = classifyProB;
		decideKodKod = classifyKodKod;
		decideProBZ3 = classifyProBZ3;
		
		labelledClasses = 0;
		if(decideProB) ++labelledClasses;
		if(decideKodKod) ++labelledClasses;
		if(decideProBZ3) ++labelledClasses;
		
		// Doing anything at all?
		if(labelledClasses == 0){
			throw new IllegalStateException("No solver given to classify for.");
		}
		
	}
	
	
	@Override
	public String getDataPathIdentifier() {
		String res = LabelGenerator.super.getDataPathIdentifier();
		if(decideProB)
			res += "_ProB";
		if(decideKodKod)
			res += "_KodKod";
		if(decideProBZ3)
			res += "_Z3";
		return res;
	}

	@Override
	public int getClassCount() {
		return labelledClasses;
	}

	@Override
	public int getLabelDimension() {
		return labelledClasses;
	}

	@Override
	public String generateLabelling(String predicate, StateSpace stateSpace) throws NeuroBException {
		ArrayList<Boolean> labelling = new ArrayList<>();
		ClassicalB formula;
		// Set up formula to solve
		try {
			formula = new ClassicalB(predicate);
		} catch(Exception e) {
			throw new NeuroBException("Could not create command from formula "+predicate, e);
		}
		
		// Try the solvers one by one
		if(decideProB) {
			labelling.add(PredicateEvaluator.evaluateCommandExecution(stateSpace, formula));
		}
		
		if(decideKodKod) {
			labelling.add(PredicateEvaluator.isDecidableWithSolver(stateSpace, "KODKOD", formula));
		}
		
		if(decideProBZ3) {
			labelling.add(PredicateEvaluator.isDecidableWithSolver(stateSpace, "SMT_SUPPORTED_INTERPRETER", formula));
		}
		
		// Collect solutions
		ArrayList<String> charLabels = new ArrayList<>();
		labelling.forEach(label -> charLabels.add((label) ? "1" : "0"));
		
		return String.join(",", charLabels);
		
		
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
		DataSetIterator iterator = new RecordReaderDataSetIterator(
				recordReader,
				batchSize,
				featureDimension,	// starting index of the label values in the csv
				featureDimension+getLabelDimension()-1, // final index of the label values in the csv
				true	// needs to be true, as this only goes with regression
			);
		
		return iterator;
	}

}
