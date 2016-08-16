package neurob.core.features;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.*;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class FeatureCollector extends DepthFirstAdapter {
	private FeatureData fd;

	public FeatureCollector() {
		fd = new FeatureData();
	}
	
	
	/**
	 * @return A {@link FeatureData} object, containing the collected data.
	 */
	public FeatureData getFeatureData() { 
		return fd;
	}
	
	/*
	 * Following: the methods to extract the feature values from AST
	 */
	
	// Predicates
	@Override
	public void caseAForallPredicate(final AForallPredicate node) {
		fd.incForAllQuantifiersCount();
		node.getImplication().apply(this);
	}

	@Override
	public void caseAExistsPredicate(final AExistsPredicate node) {
		fd.incExistsQuantifiersCount();
		node.getPredicate().apply(this);
	}

}
