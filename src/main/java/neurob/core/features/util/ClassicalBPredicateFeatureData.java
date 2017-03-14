package neurob.core.features.util;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.node.Start;
import neurob.exceptions.NeuroBException;

public class ClassicalBPredicateFeatureData extends PredicateFeatureData {
	private BParser parser;
	
	public ClassicalBPredicateFeatureData(String predicate, BParser parser) throws NeuroBException {
		this(parser);
		collectData(predicate);
	}
	
	public ClassicalBPredicateFeatureData(String predicate) throws NeuroBException{
		this(predicate, new BParser());
	}
	
	public ClassicalBPredicateFeatureData(BParser parser) {
		super();
		this.parser = parser;
	}
	
	public ClassicalBPredicateFeatureData(){
		this(new BParser());
	}
	
	@Override
	public void collectData(String predicate) throws NeuroBException {
		Start ast;
		try {
			ast = parser.parse(BParser.PREDICATE_PREFIX+" "+predicate, false, parser.getContentProvider());
		} catch (Exception e) {
			throw new NeuroBException("Could not collect feature data from predicate "+predicate, e);
		}
		
		ast.apply(new ClassicalBPredicateFeatureCollector(this));
		
	}
}
