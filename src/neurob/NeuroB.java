package neurob;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.FeatureCollector;
import neurob.core.features.FeatureData;

public class NeuroB {
	private BParser bparser;
	private FeatureCollector featureCollector;
	private FeatureData fd;

	public NeuroB() {
		// set up parser
		bparser = new BParser();
		// set up feature handling
		featureCollector = new FeatureCollector();
		fd = new FeatureData();
	}
	
	public void processPredicate(String pred){
		try {
			// get AST from predicate
			System.out.println(pred);
			Start ast = bparser.parse(pred, false);
			
			// get features
			ast.apply(featureCollector);
			fd = featureCollector.getFeatureData();
			
			// print results
			System.out.println(fd);
			
		} catch (BException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
