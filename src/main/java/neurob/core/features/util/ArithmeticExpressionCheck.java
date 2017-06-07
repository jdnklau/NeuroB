package neurob.core.features.util;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.*;

import java.util.List;

/**
 * @author Jannik Dunkelau
 */
public class ArithmeticExpressionCheck extends DepthFirstAdapter {
	private IdentifierRelationsHandler ids = new IdentifierRelationsHandler();
	boolean isSimpleArithmetic = true;

	public ArithmeticExpressionCheck(Node node){
		node.apply(this);
	}

	@Override
	public void defaultIn(Node node) {
		isSimpleArithmetic = false;
	}

	// default in sets it to false.
	// make sure arithmetic keeps it true


	@Override
	public void inAAddExpression(AAddExpression node) {
		// do nothing
	}

	@Override
	public void inAMinusOrSetSubtractExpression(AMinusOrSetSubtractExpression node) {
		// do nothing
	}

	@Override
	public void inAUnaryMinusExpression(AUnaryMinusExpression node) {
		// do nothing
	}

	@Override
	public void inAMultOrCartExpression(AMultOrCartExpression node) {
		// do nothing
	}

	@Override
	public void inADivExpression(ADivExpression node) {
		// do nothing
	}

	@Override
	public void inAModuloExpression(AModuloExpression node) {
		// do nothing
	}

	@Override
	public void inAPowerOfExpression(APowerOfExpression node) {
		// do nothing
	}

	@Override
	public void inAIdentifierExpression(AIdentifierExpression node) {
		// do nothing
	}

	@Override
	public void inAIntegerExpression(AIntegerExpression node) {
		// do nothing
		// TODO: maybe check typing of identifier to eg. distinguish between set cart or integer mul
	}

	@Override
	public void caseAIdentifierExpression(AIdentifierExpression node) {
		node.getIdentifier().stream().map(n->n.getText()).forEach(ids::addIdentifier);
		super.caseAIdentifierExpression(node);
	}

	public int getIdCount(){
		return ids.getIdCount();
	}

	public List<String> getIds(){
		return ids.getIds();
	}

	public boolean isSimpleArithmetic(){
		return isSimpleArithmetic;
	}
}
