package de.hhu.stups.neurob.core.features.predicates.util;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.*;

import java.util.LinkedList;

/**
 * Traverses the AST of a B predicate.
 * Collects data for {@link de.hhu.stups.neurob.core.features.predicates.BAst275Features}
 * and builds a {@link BAstFeatureData} object.
 */
public class BAstFeatureWalker extends DepthFirstAdapter {
    private static final int DEPTH_MIN = 0; // minimal depth in predicates

    private BAstFeatureData data;
    private boolean inNegation = false;
    private int depth = 0;
    private int powDepth = 0;
    private int negDepth = 0;
    private int quantDepth = 0; // nesting of quantifiers

    public BAstFeatureWalker() {
        data = new BAstFeatureData();
    }

    public BAstFeatureData getFeatureData() {
        return data;
    }

    private void switchByNegation(Runnable noNegationAction, Runnable negationAction) {
        if (inNegation)
            negationAction.run();
        else
            noNegationAction.run();
    }

    /*
     * In the following, the AST nodes will be visited.
     * For quick reference, here is the rough order of categories
     * - in negation or not
     * - predicate depth
     * - number of conjuncts and disjuncts and negations
     * - equivalences/implications
     * - quantifiers
     * - set membership
     * - subset relations
     * - arithmetic comparisons
     * - finiteness of sets
     * - arithmetic operators
     * - identifiers and their relations
     * - power sets
     * - sets and set operators
     * - relations
     * - functions
     * - sequences
     * - closures and iterations
     */

    // PREDICATE AND NEGATION DEPTH

    @Override
    public void inANegationPredicate(final ANegationPredicate node) {
        inNegation = !inNegation;
        negDepth++;
        if (negDepth > data.getNegationMaxDepth()) {
            data.setNegationMaxDepth(negDepth);
        }
    }

    @Override
    public void outANegationPredicate(ANegationPredicate node) {
        inNegation = !inNegation;
        negDepth--;
    }

    @Override
    public void defaultIn(Node node) {
        super.defaultIn(node);
        // count depth of predicates
        // do not count basic boolean operations, as they do not actually provide to the depth
        if (node instanceof PPredicate
            && !(node instanceof AConjunctPredicate)
            && !(node instanceof ANegationPredicate)) {
            depth++;
            if (depth > data.getMaxDepth()) {
                data.setMaxDepth(depth);
            }
        }
    }

    @Override
    public void defaultOut(Node node) {
        super.defaultOut(node);
        if (node instanceof PPredicate
            && !(node instanceof AConjunctPredicate)
            && !(node instanceof ANegationPredicate)) {
            depth--;
        }
    }


    // NUMBER OF CONJUNCTS/DISJUNCTS/NEGATIONS
    @Override
    public void inAConjunctPredicate(AConjunctPredicate node) {
        if (depth == DEPTH_MIN && !inNegation) {
            // Only count the base conjuncts (not those that are part of nested conjuncts
            data.incConjunctsCount();
        }
        switchByNegation(data::incConjunctionsCount, data::incDisjunctionsCount);
        super.inAConjunctPredicate(node);
    }

    @Override
    public void outAConjunctPredicate(AConjunctPredicate node) {
        if (depth == DEPTH_MIN && !inNegation) {
            data.endCurrentConjunct();
        }
        super.outAConjunctPredicate(node);
    }

    @Override
    public void inADisjunctPredicate(ADisjunctPredicate node) {
        if (inNegation)
            data.incConjunctionsCount();
        else
            data.incDisjunctionsCount();
        super.inADisjunctPredicate(node);
    }

    @Override
    public void caseANegationPredicate(ANegationPredicate node) {
        data.incNegationCount();
        super.caseANegationPredicate(node);
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

    @Override
    public void inAForallPredicate(AForallPredicate node) {
        quantDepth++;
        if (quantDepth > data.getQuantifierMaxDepthCount()) {
            data.setQuantifierMaxDepthCount(quantDepth);
        }
        super.inAForallPredicate(node);
    }

    @Override
    public void outAForallPredicate(AForallPredicate node) {
        quantDepth--;
        super.outAForallPredicate(node);
    }

    @Override
    public void inAExistsPredicate(AExistsPredicate node) {
        quantDepth++;
        if (quantDepth > data.getQuantifierMaxDepthCount()) {
            data.setQuantifierMaxDepthCount(quantDepth);
        }
        super.inAExistsPredicate(node);
    }

    @Override
    public void outAExistsPredicate(AExistsPredicate node) {
        quantDepth--;
        super.outAExistsPredicate(node);
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
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.SET);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.SET);
        super.caseASubsetPredicate(node);
    }

    @Override
    public void caseASubsetStrictPredicate(ASubsetStrictPredicate node) {
        switchByNegation(data::incSubsetCount, data::incNotSubsetCount);
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.SET);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.SET);
        super.caseASubsetStrictPredicate(node);
    }

    @Override
    public void caseANotSubsetPredicate(ANotSubsetPredicate node) {
        switchByNegation(data::incNotSubsetCount, data::incSubsetCount);
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.SET);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.SET);
        super.caseANotSubsetPredicate(node);
    }

    @Override
    public void caseANotSubsetStrictPredicate(ANotSubsetStrictPredicate node) {
        switchByNegation(data::incNotSubsetCount, data::incSubsetCount);
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.SET);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.SET);
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

    @Override
    public void caseAGeneralSumExpression(AGeneralSumExpression node) {
        data.incArithmeticGeneralisedSumCount();
        super.caseAGeneralSumExpression(node);
    }

    @Override
    public void caseAGeneralProductExpression(AGeneralProductExpression node) {
        data.incArithmeticGeneralisedProductCount();
        super.caseAGeneralProductExpression(node);
    }

    @Override
    public void caseASuccessorExpression(ASuccessorExpression node) {
        data.incSuccCount();
        super.caseASuccessorExpression(node);
    }

    @Override
    public void caseAPredecessorExpression(APredecessorExpression node) {
        data.incPredecCount();
        super.caseAPredecessorExpression(node);
    }

    // IDENTIFIERS AND THEIR RELATIONS

    @Override
    public void caseAIdentifierExpression(AIdentifierExpression node) {
        node.getIdentifier().stream().map(Tid -> Tid.getText()).forEach(data::addIdentifier);
        super.caseAIdentifierExpression(node);
    }

    @Override
    public void caseAPrimedIdentifierExpression(APrimedIdentifierExpression node) {
        node.getIdentifier().stream().map(Tid -> Tid.getText()).forEach(data::addIdentifier);
        super.caseAPrimedIdentifierExpression(node);
    }

    /**
     * If possible, sets the left hand side (LHS) as lower bound for the right hand side (RHS).
     * <p>
     * This means, LHS < RHS.
     * Some distinctions are made based on whether any of LHS or RHS are only identifiers
     * or more complex expressions. The highest complexity still accounted for is simple
     * arithmetic, consisting of addition and multiplication.
     * </p>
     * <p>
     * Internally there are three cases
     * <ol>
     * <li>LHS and RHS are both identifiers</li>
     * <li>One of LHS or RHS is an identifer, the other is a more complex expression</li>
     * <li>Both LHS and RHS are more complex expressions</li>
     * </ol>
     * </p>
     * <p>
     * In the first case, simply the relation LHS < RHS is set.
     * </p>
     * <p>
     * The second case is treated more specially. Assume that RHS is the more complex expression
     * (the following remains invariant to renaming).
     * If RHS is more complex than a simple arithmetic expression (addition,
     * multiplication, ...), than it will not be accounted for.
     * Else, if it contains zero identifiers, than RHS will be posed as an upper boundary for
     * the domain of LHS.
     * It it contains exactly one identifier, this identifier will be posed as symbolic upper
     * boundary for LHS.
     * It it contains more than one identifier, it will not be accounted for.
     * (The last case would pose the problem of a hypergraph, and implementing those is just
     * another step towards rebuilding CLPFD, which is not the goal.)
     * </p>
     * <p>
     * The third case is simply a more general case of case two.
     * If both sides are without identifiers, the problem is discarded.
     * If exactly one side contains exactly one identifier, the other side will pose a domain
     * boundary on it.
     * If both sides only contain one identifier each, they are treated as in the first case.
     * If any side contains more than one identifier, disregard the constraint due to
     * complexity.
     * </p>
     *
     * @param left
     * @param right
     */
    private void setLowerBoundary(PExpression left, PExpression right) {
        // check whether left is an identifier
        boolean isLeftId = left instanceof AIdentifierExpression;
        boolean isRightId = right instanceof AIdentifierExpression;
        // primed?
        boolean isLeftPrimed = !isLeftId && left instanceof APrimedIdentifierExpression;
        boolean isRightPrimed = !isRightId && right instanceof APrimedIdentifierExpression;
        isLeftId = isLeftId || isLeftPrimed; // primed ids are also ids
        isRightId = isRightId || isRightPrimed; // primed ids are also ids

        // Both are identifiers
        if (isLeftId && isRightId) {
            LinkedList<TIdentifierLiteral> ids_left =
                    (isLeftPrimed) ?
                            ((APrimedIdentifierExpression) left).getIdentifier()
                            : ((AIdentifierExpression) left).getIdentifier();
            LinkedList<TIdentifierLiteral> ids_right =
                    (isRightPrimed) ?
                            ((APrimedIdentifierExpression) right).getIdentifier()
                            : ((AIdentifierExpression) right).getIdentifier();

            for (TIdentifierLiteral Tid1 : ids_left) {
                for (TIdentifierLiteral Tid2 : ids_right) {
                    data.addIdentifierLowerBound(Tid1.getText(), Tid2.getText());
                }
            }
        }
        // at most one is id
        else if (isLeftId || isRightId) {
            ArithmeticExpressionCheck checkLeft = new ArithmeticExpressionCheck(left);
            ArithmeticExpressionCheck checkRight = new ArithmeticExpressionCheck(right);

            // both are arithmetic expressions
            if (checkLeft.isSimpleArithmetic() && checkRight.isSimpleArithmetic()) {
                int leftCount = checkLeft.getIdCount();
                int rightCount = checkRight.getIdCount();

                if (leftCount > 1 || rightCount > 1) {
                    // to complex, do nothing
                    return;
                }

                if (leftCount == 0 && rightCount == 0) {
                    // no ids, do nothing
                    return;
                }

                if (leftCount == rightCount) {
                    // both have one id
                    String idLeft = checkLeft.getIds().get(0);
                    String idRight = checkRight.getIds().get(0);

                    data.addIdentifierLowerBound(idLeft, idRight);
                } else {
                    // exactly one has one id
                    boolean idIsLHS = leftCount == 1;
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
        if (inNegation)
            setLowerBoundary(right, left);
        setLowerBoundary(left, right);
    }

    @Override
    public void outALessEqualPredicate(ALessEqualPredicate node) {
        setLowerBoundaryWRTNegation(node.getLeft(), node.getRight());
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.INTEGER);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.INTEGER);
        super.outALessEqualPredicate(node);
    }

    @Override
    public void outALessPredicate(ALessPredicate node) {
        setLowerBoundaryWRTNegation(node.getLeft(), node.getRight());
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.INTEGER);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.INTEGER);
        super.outALessPredicate(node);
    }

    @Override
    public void outAGreaterEqualPredicate(AGreaterEqualPredicate node) {
        setLowerBoundaryWRTNegation(node.getRight(), node.getLeft());
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.INTEGER);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.INTEGER);
        super.outAGreaterEqualPredicate(node);
    }

    @Override
    public void outAGreaterPredicate(AGreaterPredicate node) {
        setLowerBoundaryWRTNegation(node.getRight(), node.getLeft());
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.INTEGER);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.INTEGER);
        super.outAGreaterPredicate(node);
    }

    void setNodeType(PExpression node, AdjacencyList.AdjacencyNodeTypes type) {
        if (node instanceof AIdentifierExpression) {
            ((AIdentifierExpression) node).getIdentifier()
                    .forEach(id -> data.setIdentifierType(id.getText(), type));
        }
    }

    @Override
    public void outAEqualPredicate(AEqualPredicate node) {
        if (!inNegation) {
            setLowerBoundary(node.getLeft(), node.getRight());
            setLowerBoundary(node.getRight(), node.getLeft());
        }
        super.outAEqualPredicate(node);
    }

    @Override
    public void outANotEqualPredicate(ANotEqualPredicate node) {
        if (inNegation) {
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
        if (!(left instanceof AIdentifierExpression)) {
            return;
        }

        // if not-member ship, domain is automatically set do be unbounded
        if (!(inNegation ^ membership)) {
            ((AIdentifierExpression) left).getIdentifier()
                    .stream().map(id -> id.getText())
                    .forEach(id -> data.addIdentifierDomainBoundaries(id, false, false));
            return;
        }

        // prepare to only decide for positive bounding cases
        boolean lowerBoundary = isDomainLowerBounded(right);
        boolean upperBoundary = isDomainUpperBounded(right);

        // set domain boundaries
        ((AIdentifierExpression) left).getIdentifier()
                .stream().map(id -> id.getText())
                .forEach(id -> data.addIdentifierDomainBoundaries(id, lowerBoundary, upperBoundary));
        // Set Type
        AdjacencyList.AdjacencyNodeTypes finalType = getExpressionType(right);
        ((AIdentifierExpression) left).getIdentifier()
                .stream().map(id -> id.getText())
                .forEach(id -> data.setIdentifierType(id, finalType));

    }

    private AdjacencyList.AdjacencyNodeTypes getExpressionType(PExpression expression) {
        AdjacencyList.AdjacencyNodeTypes type = AdjacencyList.AdjacencyNodeTypes.UNKNOWN;
        if (expression instanceof ANatSetExpression
            || expression instanceof ANat1SetExpression
            || expression instanceof AIntSetExpression
            || expression instanceof ANaturalSetExpression
            || expression instanceof ANatural1SetExpression
            || expression instanceof AIntervalExpression) {
            type = AdjacencyList.AdjacencyNodeTypes.INTEGER;
        } else if (expression instanceof AIdentifierExpression
                   || expression instanceof APow1SubsetExpression
                   || expression instanceof APowSubsetExpression) {
            // Set membership in an identifier implies deferred or enumerated set
            type = AdjacencyList.AdjacencyNodeTypes.SET;
        } else if (expression instanceof ABoolSetExpression) {
            type = AdjacencyList.AdjacencyNodeTypes.BOOL;
        }

        return type;
    }

    private boolean isDomainLowerBounded(PExpression expression) {
        if (expression instanceof ANatSetExpression
            || expression instanceof ANat1SetExpression
            || expression instanceof AIntSetExpression
            || expression instanceof ABoolSetExpression
            || expression instanceof ANaturalSetExpression
            || expression instanceof ANatural1SetExpression
            || expression instanceof AIntervalExpression) {
            return true;
            // todo: maybe add powerset of those above
        }
        return false;
    }

    private boolean isDomainUpperBounded(PExpression expression) {
        if (expression instanceof ANatSetExpression
            || expression instanceof ANat1SetExpression
            || expression instanceof AIntSetExpression
            || expression instanceof ABoolSetExpression
            || expression instanceof AIntervalExpression) {
            return true;
            // todo: maybe add powerset of those above
        }
        return false;
    }


    // POWER SETS

    @Override
    public void inAPowSubsetExpression(APowSubsetExpression node) {
        powDepth++;
        if (powDepth > data.getMaxPowDepth()) {
            data.setMaxPowDepth(powDepth);
        }
        super.inAPowSubsetExpression(node);
    }

    @Override
    public void outAPowSubsetExpression(APowSubsetExpression node) {
        --powDepth;
        super.outAPowSubsetExpression(node);
    }

    @Override
    public void inAPow1SubsetExpression(APow1SubsetExpression node) {
        powDepth++;
        if (powDepth > data.getMaxPowDepth()) {
            data.setMaxPowDepth(powDepth);
        }
        super.inAPow1SubsetExpression(node);
    }

    @Override
    public void outAPow1SubsetExpression(APow1SubsetExpression node) {
        --powDepth;
        super.outAPow1SubsetExpression(node);
    }

    @Override
    public void caseAPowSubsetExpression(APowSubsetExpression node) {
        data.incPowerSetCount();
        // count stacked power sets
        if (powDepth == 1) {
            /*
             * explanation:
             * if the depth thus far equals 1, we are the second power set in a line
             *     POW(POW(...))
             * As we want only to count the stacks in total, not explicitly the occurence of
             *     POW(POW(...))
             * any deeper POW shall remain uncounted
             *
             */
            data.incPowerSetHigherOrderCounts();
        }
        super.caseAPowSubsetExpression(node);
    }

    @Override
    public void caseAPow1SubsetExpression(APow1SubsetExpression node) {
        data.incPowerSetCount();
        // count stacked power sets
        if (powDepth == 1) {
            /*
             * explanation:
             * if the depth equals 1, we are the second power set in a line
             *     POW(POW(...))
             * As we want only to count the stacks in total, not explicitly the occurrence of
             *     POW(POW(...))
             * any deeper POW shall remain uncounted
             *
             */
            data.incPowerSetHigherOrderCounts();
        }
        super.caseAPow1SubsetExpression(node);
    }


    // SET OPERATIONS

    @Override
    public void caseACardExpression(ACardExpression node) {
        data.incSetCardCount();
        super.caseACardExpression(node);
    }

    @Override
    public void caseAUnionExpression(AUnionExpression node) {
        data.incSetUnionCount();
        super.caseAUnionExpression(node);
    }

    @Override
    public void caseAGeneralUnionExpression(AGeneralUnionExpression node) {
        data.incSetUnionCount();
        data.incSetGeneralUnionCount();
        super.caseAGeneralUnionExpression(node);
    }

    @Override
    public void caseAQuantifiedUnionExpression(AQuantifiedUnionExpression node) {
        data.incSetUnionCount();
        data.incSetQuantifiedUnionCount();
        super.caseAQuantifiedUnionExpression(node);
    }

    @Override
    public void caseAIntersectionExpression(AIntersectionExpression node) {
        data.incSetIntersectCount();
        super.caseAIntersectionExpression(node);
    }

    @Override
    public void caseAGeneralIntersectionExpression(AGeneralIntersectionExpression node) {
        data.incSetIntersectCount();
        data.incSetGeneralIntersectCount();
        super.caseAGeneralIntersectionExpression(node);
    }

    @Override
    public void caseAQuantifiedIntersectionExpression(AQuantifiedIntersectionExpression node) {
        data.incSetIntersectCount();
        data.incSetQuantifiedIntersectCount();
        super.caseAQuantifiedIntersectionExpression(node);
    }

    @Override
    public void caseAComprehensionSetExpression(AComprehensionSetExpression node) {
        data.incSetComprehensionCount();
        super.caseAComprehensionSetExpression(node);
    }


    // RELATIONS

    @Override
    public void caseARelationsExpression(ARelationsExpression node) {
        data.incRelationCount();
        super.caseARelationsExpression(node);
    }

    @Override
    public void caseATotalRelationExpression(ATotalRelationExpression node) {
        data.incRelationTotalCount();
        super.caseATotalRelationExpression(node);
    }

    @Override
    public void caseASurjectionRelationExpression(ASurjectionRelationExpression node) {
        data.incRelationSurjCount();
        super.caseASurjectionRelationExpression(node);
    }

    @Override
    public void caseATotalSurjectionRelationExpression(ATotalSurjectionRelationExpression node) {
        data.incRelationTotalCount();
        data.incRelationSurjCount();
        data.incRelationTotalSurjCount();
        super.caseATotalSurjectionRelationExpression(node);
    }

    @Override
    public void caseAImageExpression(AImageExpression node) {
        data.incRelationalImageCount();
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseAImageExpression(node);
    }

    @Override
    public void caseAReverseExpression(AReverseExpression node) {
        data.incRelationInverseCount();
        setNodeType(node.getExpression(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseAReverseExpression(node);
    }

    @Override
    public void caseAOverwriteExpression(AOverwriteExpression node) {
        data.incRelationOverrideCount();
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseAOverwriteExpression(node);
    }

    @Override
    public void caseAParallelProductExpression(AParallelProductExpression node) {
        data.incRelationParallelProductCount();
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseAParallelProductExpression(node);
    }

    @Override
    public void caseADirectProductExpression(ADirectProductExpression node) {
        data.incRelationDirectProductCount();
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseADirectProductExpression(node);
    }

    @Override
    public void caseAFirstProjectionExpression(AFirstProjectionExpression node) {
        data.incProjection1Count();
        super.caseAFirstProjectionExpression(node);
    }

    @Override
    public void caseASecondProjectionExpression(ASecondProjectionExpression node) {
        data.incProjection2Count();
        super.caseASecondProjectionExpression(node);
    }

    @Override
    public void caseADomainRestrictionExpression(ADomainRestrictionExpression node) {
        data.incDomainRestrictionCount();
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseADomainRestrictionExpression(node);
    }

    @Override
    public void caseADomainSubtractionExpression(ADomainSubtractionExpression node) {
        data.incDomainSubtractionCount();
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseADomainSubtractionExpression(node);
    }

    @Override
    public void caseARangeRestrictionExpression(ARangeRestrictionExpression node) {
        data.incRangeRestrictionCount();
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseARangeRestrictionExpression(node);
    }

    @Override
    public void caseARangeSubtractionExpression(ARangeSubtractionExpression node) {
        data.incRangeSubtractionCount();
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseARangeSubtractionExpression(node);
    }

    @Override
    public void caseACompositionExpression(ACompositionExpression node) {
        data.incForwardCompositionCount();
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        setNodeType(node.getRight(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseACompositionExpression(node);
    }

    @Override
    public void caseADomainExpression(ADomainExpression node) {
        data.incDomainCount();
        super.caseADomainExpression(node);
    }

    @Override
    public void caseARangeExpression(ARangeExpression node) {
        data.incRangeCount();
        super.caseARangeExpression(node);
    }

    // FUNCTIONS

    @Override
    public void caseAPartialFunctionExpression(APartialFunctionExpression node) {
        data.incFunPartialCount();
        super.caseAPartialFunctionExpression(node);
    }

    @Override
    public void caseATotalFunctionExpression(ATotalFunctionExpression node) {
        data.incFunTotalCount();
        super.caseATotalFunctionExpression(node);
    }

    @Override
    public void caseAPartialInjectionExpression(APartialInjectionExpression node) {
        data.incFunPartialInjCount();
        super.caseAPartialInjectionExpression(node);
    }

    @Override
    public void caseATotalInjectionExpression(ATotalInjectionExpression node) {
        data.incFunTotalInjCount();
        super.caseATotalInjectionExpression(node);
    }

    @Override
    public void caseAPartialSurjectionExpression(APartialSurjectionExpression node) {
        data.incFunPartialSurjCount();
        super.caseAPartialSurjectionExpression(node);
    }

    @Override
    public void caseATotalSurjectionExpression(ATotalSurjectionExpression node) {
        data.incFunTotalSurjCount();
        super.caseATotalSurjectionExpression(node);
    }

    @Override
    public void caseAPartialBijectionExpression(APartialBijectionExpression node) {
        data.incFunPartialBijCount();
        super.caseAPartialBijectionExpression(node);
    }

    @Override
    public void caseATotalBijectionExpression(ATotalBijectionExpression node) {
        data.incFunTotalBijCount();
        super.caseATotalBijectionExpression(node);
    }

    @Override
    public void caseALambdaExpression(ALambdaExpression node) {
        data.incLambdaCount();
        super.caseALambdaExpression(node);
    }

    @Override
    public void caseAFunctionExpression(AFunctionExpression node) {
        data.incFunctionApplicationCount();
        setNodeType(node.getIdentifier(), AdjacencyList.AdjacencyNodeTypes.FUNCTION);
        super.caseAFunctionExpression(node);
    }


    // SEQUENCES

    @Override
    public void caseASequenceExtensionExpression(ASequenceExtensionExpression node) {
        data.incSeqCount();
        super.caseASequenceExtensionExpression(node);
    }

    @Override
    public void caseASeqExpression(ASeqExpression node) {
        data.incSeqCount();
        super.caseASeqExpression(node);
    }

    @Override
    public void caseASeq1Expression(ASeq1Expression node) {
        data.incSeqCount();
        super.caseASeq1Expression(node);
    }

    @Override
    public void caseAIseqExpression(AIseqExpression node) {
        data.incSeqCount();
        super.caseAIseqExpression(node);
    }

    @Override
    public void caseAIseq1Expression(AIseq1Expression node) {
        data.incSeqCount();
        super.caseAIseq1Expression(node);
    }

    @Override
    public void caseAFirstExpression(AFirstExpression node) {
        data.incFirstCount();
        super.caseAFirstExpression(node);
    }

    @Override
    public void caseALastExpression(ALastExpression node) {
        data.incLastCount();
        super.caseALastExpression(node);
    }

    @Override
    public void caseATailExpression(ATailExpression node) {
        data.incTailCount();
        super.caseATailExpression(node);
    }

    @Override
    public void caseAFrontExpression(AFrontExpression node) {
        data.incFrontCount();
        super.caseAFrontExpression(node);
    }

    @Override
    public void caseAInsertFrontExpression(AInsertFrontExpression node) {
        data.incFrontInsertionCount();
        super.caseAInsertFrontExpression(node);
    }

    @Override
    public void caseARestrictFrontExpression(ARestrictFrontExpression node) {
        data.incFrontRestrictionCount();
        super.caseARestrictFrontExpression(node);
    }

    @Override
    public void caseAInsertTailExpression(AInsertTailExpression node) {
        data.incTailInsertionCount();
        super.caseAInsertTailExpression(node);
    }

    @Override
    public void caseARestrictTailExpression(ARestrictTailExpression node) {
        data.incTailRestrictionCount();
        super.caseARestrictTailExpression(node);
    }

    @Override
    public void caseAConcatExpression(AConcatExpression node) {
        data.incConcatCount();
        super.caseAConcatExpression(node);
    }

    @Override
    public void caseAGeneralConcatExpression(AGeneralConcatExpression node) {
        data.incGeneralConcatCount();
        super.caseAGeneralConcatExpression(node);
    }

    @Override
    public void caseARevExpression(ARevExpression node) {
        data.incRevCount();
        super.caseARevExpression(node);
    }

    @Override
    public void caseAPermExpression(APermExpression node) {
        data.incPermCount();
        super.caseAPermExpression(node);
    }


    // CLOSURES AND ITERATIONS

    @Override
    public void caseAReflexiveClosureExpression(AReflexiveClosureExpression node) {
        data.incClosureCount();
        setNodeType(node.getExpression(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseAReflexiveClosureExpression(node);
    }

    @Override
    public void caseAClosureExpression(AClosureExpression node) {
        data.incClosureCount();
        setNodeType(node.getExpression(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseAClosureExpression(node);
    }

    @Override
    public void caseAIterationExpression(AIterationExpression node) {
        data.incIterateCount();
        setNodeType(node.getLeft(), AdjacencyList.AdjacencyNodeTypes.RELATION);
        super.caseAIterationExpression(node);
    }
}
