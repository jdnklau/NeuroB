/**
 * 
 */
package neurob.training.generators.labelling;

import java.io.IOException;
import java.nio.file.Path;

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
 * @author jannik
 *
 */
public class SolverTimerGenerator implements LabelGenerator {
	private int samplingSize;
	private Api api;
	
	/**
	 * Default constructor. Sets sampling size to 1
	 * @see #SolverTimerGenerator(int)
	 */
	public SolverTimerGenerator() {
		this(1);
	}
	
	/**
	 * Set a sampling size upon contruction.
	 * <p>
	 * For each solver, the predicate will be solved {@code samplingSize} times,
	 * and the measured times will be averaged out. 
	 * @param samplingSize
	 */
	@Inject
	public SolverTimerGenerator(int samplingSize){
		if(samplingSize<1)
			throw new IllegalArgumentException("samplingSize must be at least 1, but is "+samplingSize);
		
		this.samplingSize = samplingSize;
		api = Main.getInjector().getInstance(Api.class);
	}

	/* (non-Javadoc)
	 * @see neurob.training.generators.interfaces.LabelGenerator#getClassCount()
	 */
	@Override
	public int getClassCount() {
		return -1;
	}

	/* (non-Javadoc)
	 * @see neurob.training.generators.interfaces.LabelGenerator#getLabelDimension()
	 */
	@Override
	public int getLabelDimension() {
		return 3;
	}

	/* (non-Javadoc)
	 * @see neurob.training.generators.interfaces.LabelGenerator#generateLabelling(java.lang.String, de.prob.statespace.StateSpace)
	 */
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
		long ProBTime = 0;
		long KodKodTime = 0;
		long ProBZ3Time = 0;
		
		for(int sample=0; sample<samplingSize; ++sample){
			ProBTime += PredicateEvaluator.getCommandExecutionTimeInNanoSeconds(stateSpace, formula);
			KodKodTime += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, "KODKOD", formula);
			ProBZ3Time += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, "SMT_SUPPORTED_INTERPRETER", formula);
		}
		
		// normalise times
		// if a solver can not decide the predicate, it should be samplingSize*(-1)/samplingSize = -1
		ProBTime /= samplingSize;
		KodKodTime /= samplingSize;
		ProBZ3Time /= samplingSize;
		
		//return Long.toString(ProBTime)+","+Long.toString(KodKodTime)+","+Long.toString(ProBZ3Time);
		
		// convert appropriately to milliseconds
		// 1 ms = 1e-3 s
		// 1 ns = 1e-9 s
		// => 1ms = 1e6 ns		
		double ProBTimeMilliSeconds = (ProBTime < 0) ? -1 : ProBTime / 1e6;
		double KodKodTimeMilliSeconds = (KodKodTime < 0) ? -1 : KodKodTime / 1e6;
		double ProBZ3TimeMilliSeconds = (ProBZ3Time < 0) ? -1 : ProBZ3Time / 1e6;
		
		return Double.toString(ProBTimeMilliSeconds)+","+Double.toString(KodKodTimeMilliSeconds)+","+Double.toString(ProBZ3TimeMilliSeconds);
	}

	/* (non-Javadoc)
	 * @see neurob.training.generators.interfaces.LabelGenerator#generateLabelling(java.lang.String, java.nio.file.Path)
	 */
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
				true	// needs to be true, as this is regression
			);
	
		return iterator;
	}

}