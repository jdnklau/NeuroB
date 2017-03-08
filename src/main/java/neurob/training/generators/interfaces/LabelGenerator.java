package neurob.training.generators.interfaces;

import java.nio.file.Path;

import org.datavec.api.records.reader.RecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.ProblemType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.TrainingDataGenerator;

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
	 * @return The generated label vector as String
	 * @throws NeuroBException
	 */
	public String generateLabelling(String predicate, StateSpace stateSpace) throws NeuroBException;
	
	/**
	 * Generates the labelling for the given predicate with respect to the given B Machine file.
	 * <p>
	 * <b>Important</b>: It is encouraged to use {@link #generateLabelling(String, StateSpace)} instead
	 * and to handle the opening of the B Machine file by yourself; especially if you are evaluating different predicates 
	 * in the context of the same machine file.
	 * @param predicate The predicate to calculate the labelling for
	 * @param b_machine The B machine file, that may be taken into account whilst label generation
	 * @return The generated label vector as String
	 * @throws NeuroBException
	 */
	public String generateLabelling(String predicate, Path b_machine) throws NeuroBException;
	
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
