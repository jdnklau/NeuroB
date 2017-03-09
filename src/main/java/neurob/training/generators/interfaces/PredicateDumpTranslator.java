package neurob.training.generators.interfaces;

import java.util.List;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.util.PredicateDumpData;

public interface PredicateDumpTranslator {
	
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
		return translateToCSVFeatureString(fg, pdd.getPredicate()) + "," + translateToCSVLabelString(pdd.getLabellings());
	}
	
	/**
	 * Expects the predicate of a given predicate dump entry, 
	 * and returns its feature representation as comma separated String,
	 * that can be used for CSV entries.
	 * @param fg
	 * @param predicate
	 * @return
	 * @throws NeuroBException
	 */
	default
	public String translateToCSVFeatureString(FeatureGenerator fg, String predicate) throws NeuroBException{
		return fg.generateFeatureString(predicate);
	}
	
	/**
	 * Expects the labelling of a given predicate dump entry, 
	 * and returns its desired representation as comma separated String,
	 * that can be used for CSV entries.
	 * 
	 * @param predicateDumpString
	 * @return
	 */
	public String translateToCSVLabelString(List<Long> labellings);
}
