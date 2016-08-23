package neurob.core.features.helpers;

import java.util.ArrayList;
import java.util.List;
import de.be4.classicalb.core.parser.node.APredicateParseUnit;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;

public class PredicateCollector extends DepthFirstAdapter {
	private List<APredicateParseUnit> preds; // List of found predicates

	public PredicateCollector() {
		preds = new ArrayList<APredicateParseUnit>();
	}
	
	@Override
	public void caseAPredicateParseUnit(final APredicateParseUnit node){
		super.caseAPredicateParseUnit(node);
		preds.add(node);
	}

}
