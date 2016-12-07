package neurob.core.features;

import java.util.ArrayList;
import java.util.List;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.Features;
import neurob.exceptions.NeuroBException;

public class PredicateFeatures implements Features {
	
	public static final int featureDimension = 17; // Dimension of feature vectors
	private ArrayList<String> features; // The stored features
	
	
	public PredicateFeatures() {
		// Initialise fields
		features = new ArrayList<String>();
	}

	@Override
	public void reset(){
		features = new ArrayList<String>();
	}

	@Override
	public void addData(String predicate) throws NeuroBException {
		PredicateFeatureData pfd;
		try {
			pfd = new PredicateFeatureData(predicate);
		} catch (BException e) {
			throw new NeuroBException("Could not generate feature string from predicate: "+predicate, e);
		}
		
		features.add(pfd.toString());
	}

	@Override
	public List<String> getFeatureStrings() {
		return features;
	}
	
	@Override
	public int getfeatureDimension() {
		// TODO Auto-generated method stub
		return featureDimension;
	}

}
