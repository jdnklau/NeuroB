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
	
	@Override
	public void caseStart(Start node){
		// Just set feature counters to zero, so we can use the same collector over and over again for different predicates
		fd = new FeatureData();
		super.caseStart(node);
	}
	
//	@Override
//	public void defaultIn(final Node node){
//		System.out.println(node.getClass());
//	}
	
	/**************************************************************************
	 * Following: the methods to extract the feature values from AST
	 * - Quantifiers
	 * - Arithmetic Operators
	 * - Comparison Operators
	 * - Logical Operators
	 */
	
	// Quantifiers
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
	
	// Arithmetic Operators
	@Override
	public void caseAAddExpression(final AAddExpression node){
		fd.incArithmOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseAMinusOrSetSubtractExpression(final AMinusOrSetSubtractExpression node){
		fd.incArithmOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseAMultOrCartExpression(final AMultOrCartExpression node){
		fd.incArithmOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseADivExpression(final ADivExpression node){
		fd.incArithmOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	
	// comparisons
	@Override
	public void caseAEqualPredicate(final AEqualPredicate node){
		fd.incCompOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseANotEqualPredicate(final ANotEqualPredicate node){
		fd.incCompOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseAGreaterPredicate(final AGreaterPredicate node){
		fd.incCompOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseALessPredicate(final ALessPredicate node){
		fd.incCompOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	
	// logical operators
	@Override
	public void caseAConjunctPredicate(final AConjunctPredicate node){
		fd.incConjunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseADisjunctPredicate(final ADisjunctPredicate node){
		fd.incDisjunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	

}
