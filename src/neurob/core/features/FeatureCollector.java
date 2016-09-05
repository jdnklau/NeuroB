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
	 * - Identifiers
	 * - Sets
	 * - Functions
	 * - Relations
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
	
	// identifiers
	@Override
	public void caseAIdentifierExpression(final AIdentifierExpression node){
		for(TIdentifierLiteral id : node.getIdentifier()){
			fd.addIdentifier(id.getText());
		}
	}
	
	// Sets
	@Override
	public void caseAMemberPredicate(final AMemberPredicate node){
		fd.incSetMemberCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseANotMemberPredicate(final ANotMemberPredicate node){
		fd.incSetMemberCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseASubsetPredicate(final ASubsetPredicate node){
		fd.incSetMemberCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseANotSubsetPredicate(final ANotSubsetPredicate node){
		fd.incSetMemberCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseASubsetStrictPredicate(final ASubsetStrictPredicate node){
		fd.incSetMemberCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseANotSubsetStrictPredicate(final ANotSubsetStrictPredicate node){
		fd.incSetMemberCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}

	@Override
	public void caseAUnionExpression(final AUnionExpression node){
		fd.incSetOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseAIntersectionExpression(final AIntersectionExpression node){
		fd.incSetOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseASetSubtractionExpression(final ASetSubtractionExpression node){
		fd.incSetOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	
	// Functions
	@Override
	public void caseAPartialFunctionExpression(final APartialFunctionExpression node){
		fd.incFunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseATotalFunctionExpression(final ATotalFunctionExpression node){
		fd.incFunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseAPartialInjectionExpression(final APartialInjectionExpression node){
		fd.incFunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseATotalInjectionExpression(final ATotalInjectionExpression node){
		fd.incFunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseAPartialSurjectionExpression(final APartialSurjectionExpression node){
		fd.incFunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseATotalSurjectionExpression(final ATotalSurjectionExpression node){
		fd.incFunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}

	@Override
	public void caseAPartialBijectionExpression(final APartialBijectionExpression node){
		fd.incFunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseATotalBijectionExpression(final ATotalBijectionExpression node){
		fd.incFunctionsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	
	// Relations
	@Override
	public void caseADomainRestrictionExpression(final ADomainRestrictionExpression node){
		fd.incRelationOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseADomainSubtractionExpression(final ADomainSubtractionExpression node){
		fd.incRelationOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseARangeRestrictionExpression(final ARangeRestrictionExpression node){
		fd.incRelationOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseARangeSubtractionExpression(final ARangeSubtractionExpression node){
		fd.incRelationOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseAOverwriteExpression(final AOverwriteExpression node){
		fd.incRelationOperatorsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
}
