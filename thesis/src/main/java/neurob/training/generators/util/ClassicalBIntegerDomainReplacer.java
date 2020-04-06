package neurob.training.generators.util;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.AIntSetExpression;
import de.be4.classicalb.core.parser.node.AIntegerSetExpression;
import de.be4.classicalb.core.parser.node.ANat1SetExpression;
import de.be4.classicalb.core.parser.node.ANatSetExpression;
import de.be4.classicalb.core.parser.node.ANatural1SetExpression;
import de.be4.classicalb.core.parser.node.ANaturalSetExpression;

public class ClassicalBIntegerDomainReplacer extends DepthFirstAdapter {

	@Override
	public void inAIntSetExpression(final AIntSetExpression node){
		node.replaceBy(new AIntegerSetExpression());
	}

	@Override
	public void inANatSetExpression(final ANatSetExpression node){
		node.replaceBy(new ANaturalSetExpression());
	}

	@Override
	public void inANat1SetExpression(final ANat1SetExpression node){
		node.replaceBy(new ANatural1SetExpression());
	}
	
}
