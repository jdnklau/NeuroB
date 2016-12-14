package neurob.core.features.interfaces;

import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;

import de.prob.statespace.StateSpace;
import neurob.exceptions.NeuroBException;

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
	 * Generates the features of the given predicate and returns them as an {@link INDArray},
	 * without adding it to the intern feature list.
	 * 
	 * @param predicate
	 * @return The generated feature array
	 * @throws NeuroBException
	 * @see #addData(String)
	 * @see #generateFeatureArray(String)
	 * @see #generateFeatureString(String)
	 * @see #getFeatureStrings()
	 * 
	 */
	public INDArray generateFeatureNDArray(String predicate) throws NeuroBException;
	
	/**
	 * Generates the features of the given predicate and returns them as an array of {@code double[]},
	 * without adding it to the intern feature list.
	 * 
	 * @param predicate
	 * @return The generated feature array
	 * @throws NeuroBException
	 * @see #addData(String)
	 * @see #generateFeatureNDArray(String)
	 * @see #generateFeatureString(String)
	 * @see #getFeatureStrings()
	 * 
	 */
	public double[] generateFeatureArray(String predicate) throws NeuroBException;
	
	/**
	 * Generates the features of the given predicate and returns them as a string,
	 * without adding it to the intern feature list.
	 * 
	 * @param predicate
	 * @return The generated feature string
	 * @throws NeuroBException
	 * @see #addData(String)
	 * @see #getFeatureStrings()
	 * @see #generateFeatureArray(String)
	 * @see #generateFeatureNDArray(String)
	 * 
	 */
	public String generateFeatureString(String predicate) throws NeuroBException;
	
	/**
	 * Input a given string and add its features as data to this feature object.
	 * <br>
	 * Use {@link #getFeatureStrings()} to access the output computed.
	 * @param predicate
	 * @see #getFeatureStrings()
	 * @throws NeuroBException
	 */
	public void addData(String predicate) throws NeuroBException;
	
	/**
	 * Access the features generated by this class.
	 * <br> The features are stored in a list; implementation of this list is up to 
	 * the individual implementing subclasses of this interface.
	 * <p>
	 * To generate the features, make use of the {@link #setData(StateSpace))} or {@link #addData(String)} methods.
	 * </p>
	 * @return A list of {@link String}s, representing the features generated
	 * @see #addData(StateSpace)
	 * @see #addData(List)
	 * @see #addData(String)
	 */
	public List<String> getFeatureStrings();
	
	/**
	 * Returns the dimension of produced feature vectors
	 * @return
	 */
	public int getfeatureDimension();
	
	/**
	 * Resets the input data
	 */
	public void reset();
}