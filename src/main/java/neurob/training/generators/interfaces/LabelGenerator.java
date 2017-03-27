package neurob.training.generators.interfaces;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.datavec.api.records.reader.RecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.ProblemType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.util.DumpData;

public interface LabelGenerator {
	
	/**
	 * Returns an identifying string of this generator.
	 * <p>
	 * This is e.g. used at training set generation, to save the training data generated with this features into a directory,7
	 * that uniquely identifies with this string.
	 * @return
	 */
	default public String getDataPathIdentifier(){
		return this.getClass().getSimpleName();
	}
	
	/**
	 * Returns the number of different classes the label generator differentiates between.
	 * <p>
	 * Either a positive number or {@code -1} for regression.
	 * @return Number of different classes
	 */
	public int getClassCount();
	
	/**
	 * Returns the size of the labelling vector generated.
	 * This is usually identical with {@link #getClassCount()}, as  one-hot vectors are used
	 * @return
	 */
	public int getLabelDimension();
	
	/**
	 * Returns the type of problem the label generator tries to tackle.
	 * <p>
	 * See {@link ProblemType} for more information about the possible values.
	 * @return
	 * @see {@link ProblemType}
	 */
	public ProblemType getProblemType();
	
	/**
	 * Generates the labelling for the given predicate with respect to the given StateSpace.
	 * @param predicate The predicate to calculate the labelling for
	 * @param stateSpace The state space, that may be taken into account whilst label generation
	 * @return The generated label vector
	 * @see #generateLabellingString(String, StateSpace)
	 * @throws NeuroBException
	 */
	public double[] generateLabelling(String predicate, StateSpace stateSpace) throws NeuroBException;
	
	/**
	 * Generates the labelling for the given predicate with respect to the given StateSpace
	 * as String.
	 * 
	 * @param predicate The predicate to calculate the labelling for
	 * @param stateSpace The state space, that may be taken into account whilst label generation
	 * @return The generated label vector as comma separated string
	 * @see #generateLabelling(String, StateSpace)
	 * @throws NeuroBException
	 */
	default
	public String generateLabellingString(String predicate, StateSpace stateSpace) throws NeuroBException {
		return String.join(",", 
					Arrays.stream(generateLabelling(predicate, stateSpace))
					.mapToObj(String::valueOf)
					.collect(Collectors.toList()));
	}
	
	/**
	 * Translates the labelling captured by the dump data
	 * to the format produced by this class.
	 * <p>
	 * Output should be identical with {@link #generateLabelling(String, StateSpace)}
	 * @param dumpData
	 * @return
	 */
	public double[] translateLabelling(DumpData dumpData);
	
	/**
	 * Translates the labelling captured by the dump data
	 * to the format produced by this class as comma separated string.
	 * @param dumpData
	 * @return
	 */
	default
	public String translateLabellingString(DumpData dumpData){
		return String.join(",", 
				Arrays.stream(translateLabelling(dumpData))
				.mapToObj(String::valueOf)
				.collect(Collectors.toList()));
	}
	
	/**
	 * Returns a DataSetIterator needed in training step of the neural network in use.
	 * @param recordReader
	 * @param batchSize
	 * @param featureDimension Size of the feature vector, e.g. number of its entries
	 * @return
	 */
	default public DataSetIterator getDataSetIterator(RecordReader recordReader, int batchSize, int featureDimension){
		DataSetIterator iterator = new RecordReaderDataSetIterator(
				recordReader,
				batchSize,
				featureDimension,	// starting index of the label values in the csv
				getLabelDimension()		// Number of different classes
			);
		return iterator;
	}
	
	/**
	 * Couples this label generator with an arbitrary feature generator to
	 * a fitting {@link TrainingDataGenerator}
	 * @return
	 */
	public TrainingDataGenerator getTrainingDataGenerator(FeatureGenerator fg);
	

}
