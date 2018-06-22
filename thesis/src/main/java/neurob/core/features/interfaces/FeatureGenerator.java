package neurob.core.features.interfaces;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import de.prob.statespace.StateSpace;
import org.datavec.api.records.reader.RecordReader;
import org.nd4j.linalg.api.ndarray.INDArray;

import neurob.exceptions.NeuroBException;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.interfaces.LabelGenerator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;

/**
 * Generates the features of a predicate.
 *
 * <p>
 * Usage example:
 * <br>
 * <pre><code>
 * // get a list of predicates
 * {@code List<String>} predicates = ...
 *
 * Features f = ... // Create instance of a class implementing the interface
 *
 * // input data
 * for(String pred : predicates){
 *     f.addData(pred)
 * }
 *
 * // Access features
 * // getFeatures() returns a {@code List<String>} object
 * for(feature : f.getFeatureStrings()){
 *     System.out.println(feature);
 * }
 * <code></pre>
 * @author jannik
 *
 */
public interface FeatureGenerator {

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
	 * Generates the features of the given source and returns them as an {@link INDArray}.
	 *
	 * @param source
	 * @return The generated feature array
	 * @throws NeuroBException
	 * @see #generateFeatureArray(String)
	 * @see #generateFeatureString(String)
	 *
	 */
	public INDArray generateFeatureNDArray(String source) throws NeuroBException;

	/**
	 * Generates the features of the given source and returns them as an array of {@code double[]}..
	 *
	 * @param source
	 * @return The generated feature array
	 * @throws NeuroBException
	 * @see #generateFeatureNDArray(String)
	 * @see #generateFeatureString(String)
	 *
	 */
	public double[] generateFeatureArray(String source) throws NeuroBException;

	/**
	 * Generates the features of the given source and returns them as a string,
	 * without adding it to the intern feature list.
	 *
	 * @param source
	 * @return The generated feature string as comma separated list
	 * @throws NeuroBException
	 * @see #generateFeatureArray(String)
	 * @see #generateFeatureNDArray(String)
	 *
	 */
	default
	public String generateFeatureString(String source) throws NeuroBException{
		return String.join(",",
				Arrays.stream(generateFeatureArray(source))
				.mapToObj(Double::toString)
				.collect(Collectors.toList()));
	}

	/**
	 * Returns the dimension of produced feature vectors
	 * @return
	 */
	public int getFeatureDimension();

	/**
	 * Sets the source file of the features to be generated next.
	 * <p>
	 * Some feature generators' work can be enhanced by providing this information.
	 * @param sourceFile
	 */
	public void setSourceFile(Path sourceFile) throws NeuroBException;

	/**
	 * Adds the state space of a corresponding machine file to the feature generator
	 * for possible further analysis.
	 * @param stateSpace
	 * @throws NeuroBException
	 */
	public void setStateSpace(StateSpace stateSpace);

	/**
	 * Returns the path to the source file over which the features are generated. May return null.
	 * @return Path to source file or null.
	 */
	public Path getSourceFile();

	/**
	 * Returns an initialised RecordReader suitable for processing a training
	 * set generated with this feature generator.
	 * @param trainingSet Training set generated by this feature generator
	 * @param batchSize Number of samples read per batch
	 * @return RecordReader for processing a training set generated by this
	 * 				feature generator
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public RecordReader getRecordReader(Path trainingSet, int batchSize)
			throws IOException, InterruptedException;

	/**
	 * Couples this feature generator with an arbitrary label generator to
	 * a fitting {@link TrainingDataGenerator}
	 * @return
	 */
	public TrainingDataGenerator getTrainingDataGenerator(LabelGenerator lg);


	/**
	 * Returns an instance of a data normaliser that is advised to use for a training set
	 * consisting of this features
	 * @return
	 */
	public DataNormalization getNewNormalizer();

}