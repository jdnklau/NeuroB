package neurob.core.features.util;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.*;

/**
 * @author Jannik Dunkelau
 */
public class LargeBASTFeatureCollector extends DepthFirstAdapter {
	private static final int DEPTH_MIN = 1; // minimal depth in predicates

	private LargeBASTFeatureData data;
	private boolean inNegation = false;
	private int depth = 0;
	private int maxDepth = 0;

	public LargeBASTFeatureCollector(LargeBASTFeatureData data){
		this.data = data;
	}

	public int getMaxDepth(){
		return maxDepth;
	}

	private void switchByNegation(Runnable noNegationAction, Runnable negationAction){
		if(inNegation)
			negationAction.run();
		else
			noNegationAction.run();
	}

	/*
	 *****************************************************************
	 * In the following, the AST nodes will be visited.
	 * For quick reference, here is the rough order of categories
	 * - in negation or not
	 * - predicate depth
	 * - number of conjuncts and disjuncts
	 * - equivalences/implications
	 * - quantifiers
	 * - set membership
	 * - subset relations
	 * - arithmetic comparisons
	 * - finiteness of sets
	 * - arithmetic operators
	 * - predefined sets (NATURAL, etc)
	 */

	// PREDICATE AND NEGATION DEPTH

	@Override
	public void inANegationPredicate(final ANegationPredicate node){
		inNegation = !inNegation;
	}
	@Override
	public void outANegationPredicate(ANegationPredicate node) {
		inNegation = !inNegation;
	}

	@Override
	public void defaultIn(Node node) {
		super.defaultIn(node);
		// count depth of predicates
		// do not count basic boolen operations, as they do not actually provide to the depth
		if(node instanceof PPredicate
				&& !(node instanceof AConjunctPredicate)
				&& !(node instanceof AConjunctPredicate)
				&& !(node instanceof ANegationPredicate)) {
			depth++;
			if (depth > maxDepth) maxDepth = depth;
		}
	}
	@Override
	public void defaultOut(Node node) {
		super.defaultOut(node);
		if(node instanceof  PPredicate
				&& !(node instanceof AConjunctPredicate)
				&& !(node instanceof AConjunctPredicate)
				&& !(node instanceof ANegationPredicate)) {
			depth--;
		}
	}



	// NUMBER OF CONJUNCTS/DISJUNCTS
	@Override
	public void inAConjunctPredicate(AConjunctPredicate node) {
		if(depth==DEPTH_MIN) {
			// Only count the base conjuncts (not those that are part of nested conjuncts
			data.incConjunctsCount();
		}
		switchByNegation(data::incConjunctionsCount, data::incDisjunctionsCount);
		super.inAConjunctPredicate(node);
	}

	@Override
	public void inADisjunctPredicate(ADisjunctPredicate node) {
		if(inNegation)
			data.incConjunctionsCount();
		else
			data.incDisjunctionsCount();
		super.inADisjunctPredicate(node);
	}


	// EQUIVALENCES AND IMPLICATIONS

	@Override
	public void caseAImplicationPredicate(AImplicationPredicate node) {
		data.incImplicationsCount();
		super.caseAImplicationPredicate(node);
	}
	@Override
	public void caseAEquivalencePredicate(AEquivalencePredicate node) {
		data.incEquivalencesCount();
		super.caseAEquivalencePredicate(node);
	}

	// QUANTIFIERS

	@Override
	public void caseAForallPredicate(AForallPredicate node) {
		switchByNegation(data::incUniversalQuantifiersCount, data::incExistentialQuantifiersCount);
		super.caseAForallPredicate(node);
	}

	@Override
	public void caseAExistsPredicate(AExistsPredicate node) {
		switchByNegation(data::incExistentialQuantifiersCount, data::incUniversalQuantifiersCount);
		super.caseAExistsPredicate(node);
	}



	// EQUALITY/INEQUALITY

	@Override
	public void caseAEqualPredicate(AEqualPredicate node) {
		switchByNegation(data::incEqualityCount, data::incInequalityCount);
		super.caseAEqualPredicate(node);
	}

	@Override
	public void caseANotEqualPredicate(ANotEqualPredicate node) {
		switchByNegation(data::incInequalityCount, data::incEqualityCount);
		super.caseANotEqualPredicate(node);
	}



	// SETS: MEMBERSHIP AND SUBSETS

	@Override
	public void caseAMemberPredicate(AMemberPredicate node) {
		switchByNegation(data::incMemberCount, data::incNotMemberCount);
		super.caseAMemberPredicate(node);
	}

	@Override
	public void caseANotMemberPredicate(ANotMemberPredicate node) {
		switchByNegation(data::incNotMemberCount, data::incMemberCount);
		super.caseANotMemberPredicate(node);
	}

	@Override
	public void caseASubsetPredicate(ASubsetPredicate node) {
		switchByNegation(data::incSubsetCount, data::incNotSubsetCount);
		super.caseASubsetPredicate(node);
	}

	@Override
	public void caseASubsetStrictPredicate(ASubsetStrictPredicate node) {
		switchByNegation(data::incSubsetCount, data::incNotSubsetCount);
		super.caseASubsetStrictPredicate(node);
	}

	@Override
	public void caseANotSubsetPredicate(ANotSubsetPredicate node) {
		switchByNegation(data::incNotSubsetCount, data::incSubsetCount);
		super.caseANotSubsetPredicate(node);
	}

	@Override
	public void caseANotSubsetStrictPredicate(ANotSubsetStrictPredicate node) {
		switchByNegation(data::incNotSubsetCount, data::incSubsetCount);
		super.caseANotSubsetStrictPredicate(node);
	}



	// COMPARISONS: GREATER AND LESS

	@Override
	public void caseALessEqualPredicate(ALessEqualPredicate node) {
		data.incSizeComparisonCount();
		super.caseALessEqualPredicate(node);
	}

	@Override
	public void caseALessPredicate(ALessPredicate node) {
		data.incSizeComparisonCount();
		super.caseALessPredicate(node);
	}

	@Override
	public void caseAGreaterEqualPredicate(AGreaterEqualPredicate node) {
		data.incSizeComparisonCount();
		super.caseAGreaterEqualPredicate(node);
	}

	@Override
	public void caseAGreaterPredicate(AGreaterPredicate node) {
		data.incSizeComparisonCount();
		super.caseAGreaterPredicate(node);
	}



	// BOOLEAN LITERALS AND CONVERTIONS

	@Override
	public void caseABooleanTrueExpression(ABooleanTrueExpression node) {
		data.incBooleanLiteralsCount();
		super.caseABooleanTrueExpression(node);
	}

	@Override
	public void caseABooleanFalseExpression(ABooleanFalseExpression node) {
		data.incBooleanLiteralsCount();
		super.caseABooleanFalseExpression(node);
	}

	@Override
	public void caseAConvertBoolExpression(AConvertBoolExpression node) {
		data.incBooleanConversionCount();
		super.caseAConvertBoolExpression(node);
	}

	// FINITENESS


	@Override
	public void caseAFinSubsetExpression(AFinSubsetExpression node) {
		switchByNegation(data::incFiniteSetRequirementsCount, data::incInfiniteSetRequirementsCount);
		super.caseAFinSubsetExpression(node);
	}

	@Override
	public void caseAFin1SubsetExpression(AFin1SubsetExpression node) {
		switchByNegation(data::incFiniteSetRequirementsCount, data::incInfiniteSetRequirementsCount);
		super.caseAFin1SubsetExpression(node);
	}




	// ARITHMETIC

	@Override
	public void caseAAddExpression(AAddExpression node) {
		data.incArithmeticAdditionCount();
		super.caseAAddExpression(node);
	}

	@Override
	public void caseAMinusOrSetSubtractExpression(AMinusOrSetSubtractExpression node) {
		data.incArithmeticAdditionCount();
		super.caseAMinusOrSetSubtractExpression(node);
	}

	@Override
	public void caseAMultOrCartExpression(AMultOrCartExpression node) {
		data.incArithmeticMultiplicationCount();
		super.caseAMultOrCartExpression(node);
	}

	@Override
	public void caseADivExpression(ADivExpression node) {
		data.incArithmeticDivisionCount();
		super.caseADivExpression(node);
	}

	@Override
	public void caseAModuloExpression(AModuloExpression node) {
		data.incArithmeticModuloCount();
		super.caseAModuloExpression(node);
	}

	@Override
	public void caseAPowerOfExpression(APowerOfExpression node) {
		data.incArithmeticExponentialCount();
		super.caseAPowerOfExpression(node);
	}

	@Override
	public void caseAMinExpression(AMinExpression node) {
		data.incArithmeticMinCount();
		super.caseAMinExpression(node);
	}

	@Override
	public void caseAMaxExpression(AMaxExpression node) {
		data.incArithmeticMaxCount();
		super.caseAMaxExpression(node);
	}



	// PREDEFINED SETS

	@Override
	public void caseANaturalSetExpression(ANaturalSetExpression node) {
		super.caseANaturalSetExpression(node);
	}

	@Override
	public void caseANatural1SetExpression(ANatural1SetExpression node) {
		super.caseANatural1SetExpression(node);
	}

	@Override
	public void caseANatSetExpression(ANatSetExpression node) {
		super.caseANatSetExpression(node);
	}

	@Override
	public void caseANat1SetExpression(ANat1SetExpression node) {
		super.caseANat1SetExpression(node);
	}
}
