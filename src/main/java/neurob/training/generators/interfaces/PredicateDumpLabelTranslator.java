package neurob.training.generators.interfaces;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory.Feature;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.util.PredicateDumpData;

/**
 * Interface class to ensure utility to translate data from predicate dumps into
 * a desired label representation.
 * <p>
 * Can further make use of a {@link FeatureGenerator} to create full training samples.
 *  
 * @author jannik
 *
 */
public interface PredicateDumpLabelTranslator {
	
	/**
	 * Translates the given entry from a predicate dump to a string
	 * that can be printed to a data.csv
	 * @param fg
	 * @param predicateDumpString
	 * @return
	 * @throws NeuroBException 
	 */
	default
	public String translateToCSVDataString(FeatureGenerator fg, String predicateDumpString) throws NeuroBException{
		PredicateDumpData pdd = new PredicateDumpData(predicateDumpString);
		double[] features = translateToFeatureArray(fg, pdd.getPredicate());
		double[] labels = translateToLabelArray(pdd.getLabellings());
		
		return translateToCSVDataString(features)
				+ "," 
				+ translateToCSVDataString(labels);
	}
	
	/**
	 * Translates the given array to a comma separated string.
	 * @param data
	 * @return
	 * @throws NeuroBException
	 */
	default
	public String translateToCSVDataString(double[] data) throws NeuroBException{
		return String.join(",", 
				Arrays.stream(data).mapToObj(Double::toString)
				.collect(Collectors.toList()));
	}
	
	/**
	 * Expects the predicate of a given predicate dump entry, 
	 * and returns its feature representation as array.
	 * @param fg
	 * @param predicate
	 * @return
	 * @throws NeuroBException
	 */
	default
	public double[] translateToFeatureArray(FeatureGenerator fg, String predicate) throws NeuroBException{
		return fg.generateFeatureArray(predicate);
	}
	
	/**
	 * Expects the labelling of a given predicate dump entry, 
	 * and returns its desired representation as array.
	 * 
	 * @param predicateDumpString
	 * @return
	 */
	public double[] translateToLabelArray(List<Long> labellings);
}
