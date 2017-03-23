/**
 * 
 */
package neurob.training.generators.labelling;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.datavec.api.records.reader.RecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.statespace.StateSpace;
import neurob.core.util.ProblemType;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.PredicateDumpTranslator;
import neurob.training.generators.interfaces.PredicateLabelGenerator;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateEvaluator;

/**
 * @author jannik
 *
 */
public class SolverTimerGenerator implements PredicateLabelGenerator, PredicateDumpTranslator {
	private int samplingSize;
	
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
	public SolverTimerGenerator(int samplingSize){
		if(samplingSize<1)
			throw new IllegalArgumentException("samplingSize must be at least 1, but is "+samplingSize);
		
		this.samplingSize = samplingSize;
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
	
	@Override
	public ProblemType getProblemType(){
		return ProblemType.REGRESSION;
	}

	/* (non-Javadoc)
	 * @see neurob.training.generators.interfaces.LabelGenerator#generateLabelling(java.lang.String, de.prob.statespace.StateSpace)
	 */
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
		
		// normalise times
		// if a solver can not decide the predicate, it should be samplingSize*(-1)/samplingSize = -1
		return getLabellingByTimes(ProBTime/samplingSize, KodKodTime/samplingSize, Z3Time/samplingSize);
		
	}
	
	private double[] getLabellingByTimes(double ProBTime, double KodKodTime, double Z3Time){
		//return Long.toString(ProBTime)+","+Long.toString(KodKodTime)+","+Long.toString(Z3Time);
		
		// convert appropriately to milliseconds
		// 1 ms = 1e-3 s
		// 1 ns = 1e-9 s
		// => 1ms = 1e6 ns		
		double ProBTimeMilliSeconds = (ProBTime < 0) ? -1 : ProBTime / 1e6;
		double KodKodTimeMilliSeconds = (KodKodTime < 0) ? -1 : KodKodTime / 1e6;
		double Z3TimeMilliSeconds = (Z3Time < 0) ? -1 : Z3Time / 1e6;
		
		return new double[]{ProBTimeMilliSeconds, KodKodTimeMilliSeconds,Z3TimeMilliSeconds};
	}

	/* (non-Javadoc)
	 * @see neurob.training.generators.interfaces.LabelGenerator#generateLabelling(java.lang.String, java.nio.file.Path)
	 */
//	@Override
//	public String generateLabelling(String predicate, Path b_machine) throws NeuroBException {
//		// setup up state space
//		StateSpace ss;
//		try {
//			ss = api.b_load(b_machine.toString());
//		} catch (IOException e) {
//			throw new NeuroBException("Could not access file: "+b_machine.toString(), e);
//		} catch (ModelTranslationError e) {
//			throw new NeuroBException("Could not translate model: "+b_machine.toString(), e);
//		}
//		
//		// Use other method to calculate labelling
//		String labelling = generateLabelling(predicate, ss);
//		
//		ss.kill();
//		// return
//		return labelling;
//	}
	
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

	@Override
	public double[] translateToLabelArray(List<Long> labellings) {
		return getLabellingByTimes(labellings.get(0), labellings.get(1), labellings.get(2));
	}

}
