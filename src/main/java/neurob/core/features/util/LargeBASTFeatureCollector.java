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
	 * - identifiers and their relations
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



	// IDENTIFIERS AND THEIR RELATIONS

	@Override
	public void caseAIdentifierExpression(AIdentifierExpression node) {
		node.getIdentifier().stream().map(Tid->Tid.getText()).forEach(data::addIdentifier);
		super.caseAIdentifierExpression(node);
	}

	/**
	 * If possible, sets the left hand side (LHS) as lower bound for the right hand side (RHS).
	 * <p>
	 *     This means, LHS < RHS.
	 *     Some distinctions are made based on whether any of LHS or RHS are only identifiers
	 *     or more complex expressions. The highest complexity still accounted for is simple
	 *     arithmetic, consisting of addition and multiplication.
	 * </p>
	 * <p>
	 *     Internally there are three cases
	 *     <ol>
	 *         <li>LHS and RHS are both identifiers</li>
	 *         <li>One of LHS or RHS is an identifer, the other is a more complex expression</li>
	 *         <li>Both LHS and RHS are more complex expressions</li>
	 *     </ol>
	 * </p>
	 * <p>
	 *     In the first case, simply the relation LHS < RHS is set.
	 * </p>
	 * <p>
	 *     The second case is treated more specially. Assume that RHS is the more complex expression
	 *     (the following remains invariant to renaming).
	 *     If RHS is more complex than a simple arithmetic expression (addition,
	 *     multiplication, ...), than it will not be accounted for.
	 *     Else, if it contains zero identifiers, than RHS will be posed as an upper boundary for
	 *     the domain of LHS.
	 *     It it contains exactly one identifier, this identifier will be posed as symbolic upper
	 *     boundary for LHS.
	 *     It it contains more than one identifier, it will not be accounted for.
	 *     (The last case would pose the problem of a hypergraph, and implementing those is just
	 *     another step towards rebuilding CLPFD, which is not the goal.)
	 * </p>
	 * <p>
	 *     The third case is simply a more general case of case two.
	 *     If both sides are without identifiers, the problem is discarded.
	 *     If exactly one side contains exactly one identifier, the other side will pose a domain
	 *     boundary on it.
	 *     If both sides only contain one identifier each, they are treated as in the first case.
	 *     If any side contains more than one identifier, disregard the constraint due to
	 *     complexity.
	 * </p>
	 * @param left
	 * @param right
	 */
	private void setLowerBoundary(PExpression left, PExpression right) {
		// check whether left is an identifier
		boolean isLeftId = left instanceof AIdentifierExpression;
		boolean isRightId = right instanceof AIdentifierExpression;

		// Both are identifiers
		if(isLeftId && isRightId){
			for(TIdentifierLiteral Tid1 : ((AIdentifierExpression) left).getIdentifier()){
				for(TIdentifierLiteral Tid2 : ((AIdentifierExpression) right).getIdentifier()){
					data.addIdentifierLowerBound(Tid1.getText(), Tid2.getText());
				}
			}
		}
		// at most one is id
		else if(isLeftId || isRightId){
			ArithmeticExpressionCheck checkLeft = new ArithmeticExpressionCheck(left);
			ArithmeticExpressionCheck checkRight = new ArithmeticExpressionCheck(right);

			// both are arithmetic expressions
			if(checkLeft.isSimpleArithmetic() && checkRight.isSimpleArithmetic()){
				int leftCount = checkLeft.getIdCount();
				int rightCount = checkRight.getIdCount();

				if(leftCount > 1 || rightCount > 1){
					// to complex, do nothing
					return;
				}

				if(leftCount == 0 && rightCount == 0){
					// no ids, do nothing
					return;
				}

				if(leftCount == rightCount){
					// both have one id
					String idLeft = checkLeft.getIds().get(0);
					String idRight = checkRight.getIds().get(0);

					data.addIdentifierLowerBound(idLeft, idRight);
				}
				else {
					// exactly one has one id
					boolean idIsLHS = leftCount==1;
					ArithmeticExpressionCheck check = (idIsLHS) ? checkLeft : checkRight;

					String id = check.getIds().get(0);

					boolean lowerBound = !idIsLHS;
					boolean upperBound = idIsLHS;
					data.addIdentifierDomainBoundaries(id, lowerBound, upperBound);
				}
			}
			// else do nothing
		}
	}

	private void setLowerBoundaryWRTNegation(PExpression left, PExpression right) {
		if(inNegation)
			setLowerBoundary(right,left);
		setLowerBoundary(left, right);
	}

	@Override
	public void outALessEqualPredicate(ALessEqualPredicate node) {
		setLowerBoundaryWRTNegation(node.getLeft(), node.getRight());
		super.outALessEqualPredicate(node);
	}

	@Override
	public void outALessPredicate(ALessPredicate node) {
		setLowerBoundaryWRTNegation(node.getLeft(), node.getRight());
		super.outALessPredicate(node);
	}

	@Override
	public void outAGreaterEqualPredicate(AGreaterEqualPredicate node) {
		setLowerBoundaryWRTNegation(node.getRight(), node.getLeft());
		super.outAGreaterEqualPredicate(node);
	}

	@Override
	public void outAGreaterPredicate(AGreaterPredicate node) {
		setLowerBoundaryWRTNegation(node.getRight(), node.getLeft());
		super.outAGreaterPredicate(node);
	}

	@Override
	public void outAEqualPredicate(AEqualPredicate node) {
		if(!inNegation){
			setLowerBoundary(node.getLeft(), node.getRight());
			setLowerBoundary(node.getRight(), node.getLeft());
		}
		super.outAEqualPredicate(node);
	}

	@Override
	public void outANotEqualPredicate(ANotEqualPredicate node) {
		if(inNegation){
			setLowerBoundary(node.getLeft(), node.getRight());
			setLowerBoundary(node.getRight(), node.getLeft());
		}
		super.outANotEqualPredicate(node);
	}

	@Override
	public void outAMemberPredicate(AMemberPredicate node) {
		setIdentifierDomain(node.getLeft(), true, node.getRight());
		super.outAMemberPredicate(node);
	}

	@Override
	public void outANotMemberPredicate(ANotMemberPredicate node) {
		setIdentifierDomain(node.getLeft(), false, node.getRight());
		super.outANotMemberPredicate(node);
	}

	private void setIdentifierDomain(PExpression left, boolean membership, PExpression right) {
		if(!(left instanceof AIdentifierExpression)){
			return;
		}

		// if not-member ship, domain is automatically set do be unbounded
		if(!(inNegation^membership)){
			((AIdentifierExpression) left).getIdentifier()
					.stream().map(id->id.getText())
					.forEach(id->data.addIdentifierDomainBoundaries(id,false,false));
			return;
		}

		// prepare to only decide for positive bounding cases
		boolean lowerBoundary = isDomainLowerBounded(right);
		boolean upperBoundary = isDomainUpperBounded(right);

		// set domain boundaries
		((AIdentifierExpression) left).getIdentifier()
				.stream().map(id->id.getText())
				.forEach(id->data.addIdentifierDomainBoundaries(id,lowerBoundary,upperBoundary));
	}

	private boolean isDomainLowerBounded(PExpression expression) {
		if(expression instanceof ANatSetExpression
				|| expression instanceof ANat1SetExpression
				|| expression instanceof AIntSetExpression
				|| expression instanceof ABoolSetExpression
				|| expression instanceof ANaturalSetExpression
				|| expression instanceof  ANatural1SetExpression){
			return true;
			// todo: maybe add powerset of those above
		}
		return false;
	}

	private boolean isDomainUpperBounded(PExpression expression) {
		if(expression instanceof ANatSetExpression
				|| expression instanceof ANat1SetExpression
				|| expression instanceof AIntSetExpression
				|| expression instanceof ABoolSetExpression){
			return true;
			// todo: maybe add powerset of those above
		}
		return false;
	}
}
