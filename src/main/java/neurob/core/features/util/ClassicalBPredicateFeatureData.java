package neurob.core.features.util;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.exceptions.NeuroBException;

public class ClassicalBPredicateFeatureData extends PredicateFeatureData {
	
	public ClassicalBPredicateFeatureData(String predicate) throws NeuroBException{
		this();
		collectData(predicate);
	}
	
	public ClassicalBPredicateFeatureData() {
		super();
	}
	
	@Override
	public void collectData(String predicate) throws NeuroBException {
		Start ast;
		try {
			ast = BParser.parse(BParser.FORMULA_PREFIX+" "+predicate);
		} catch (BCompoundException e) {
			throw new NeuroBException("Could not collect feature data from predicate "+predicate, e);
		}
		
		ast.apply(new ClassicalBPredicateFeatureCollector(this));
		
	}
}
