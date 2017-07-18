package neurob.core.features.util;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.node.Start;
import de.prob.model.representation.Machine;
import de.prob.statespace.StateSpace;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;

/**
 * This class represents a feature vector for a given predicate of the B method.
 * The features calculated are
 * <ul>
 *     <li>to be done</li>
 * </ul>
 * @author Jannik Dunkelau
 */
public class LargeBASTFeatureData {
	private final BParser bParser;
	private final StateSpace ss;
	private LargeBASTFeatureCollector collector;
	private final MachineType mtype;

	// count of data from predicate
	private int conjunctsCount = 1; // at least one conjunct in predicate
	private int conjunctionsCount = 0; // Number of conjunctions &
	private int disjunctionsCount = 0; // Number of disjunctions or
	private int implicationsCount = 0; // Number of implications =>
	private int equivalencesCount = 0; // Number of equivalences <=>
	private int uniQuantifierCount = 0; // universal quantifiers
	private int exQuantifierCount = 0; // existential quantifiers
	private int negationCount = 0; // number of negations
	private int equalityCount = 0; // number of equalities =
	private int inequalityCount = 0; // number of equalities /=
	private int memberCount = 0; // Number of set memberships :
	private int notMemberCount = 0; // Number of negated set memberships /:
	private int subsetCount = 0; // Number of (strict) subsets <: and <<:
	private int notSubsetCount = 0; // Number of negated (strict) subsets /<: and /<<:
	private int sizeComparisonCount = 0; // Number of <, <=, >, >=
	private int booleanLiteralsCount = 0; // Number of TRUE and FALSE
	private int booleanConversionCount = 0; // Number of BOOL(...) calls
	private int finiteSetRequirementsCount = 0; // Number of FIN(...) calls
	private int infiniteSetRequirementsCount = 0; // Number of FIN(...) calls in negation
	private int arithmeticAdditionCount = 0; // Number of + and - operations
	private int arithmeticMultiplicationCount = 0; // Number of * operations
	private int arithmeticDivisionCount = 0; // Number of / operations
	private int arithmeticModuloCount = 0; // Number of modulo operations
	private int arithmeticExponentialCount = 0; // Number of ** operations
	private int arithmeticMinCount = 0; // Number of min calls
	private int arithmeticMaxCount = 0; // Number of max calls
	private int arithmeticGeneralisedSumCount = 0; // Number of generalised sums
	private int arithmeticGeneralisedProductCount = 0; // Number of generalised products
	private int succCount = 0; // Number of successor calls
	private int predecCount = 0; // Number of predecessor calls
	private int powerSetCount = 0; // Number of POW(...) calls
	private int powerSetHigherOrderCounts = 0; // Number of POW(POW(...)) calls
	private int setCardCount = 0; // Number of CARD(...) counts
	private int setUnionCount = 0; // Number of set unions
	private int setIntersectCount = 0; // Number of set intersections
	private int setGeneralUnionCount = 0; // Number of unions over a set of sets
	private int setGeneralIntersectCount = 0; // Number of intersects over a set of sets
	private int setQuantifiedUnionCount = 0; // Number of unions over something
	private int setQuantifiedIntersectCount = 0; // Number of intersects over something
	private int setSubtractionCount = 0; // Number of set subtractions \
	private int setComprehensionCount = 0; // Number of comprehension sets
	private int relationCount = 0; // Number of relations defined <->
	private int relationTotalCount = 0; // Number of total relations <<->
	private int relationSurjCount = 0; // Number of surjective relations <->>
	private int relationTotalSurjCount = 0; // Number of total surjective relations <<->>
	private int relationalImageCount = 0; // Number of relational images r[S]
	private int relationInverseCount = 0; // Number of relational inverses r~
	private int relationOverrideCount = 0; // Number of relational overrides <+
	private int relationDirectProductCount = 0; // Number of direct products ><
	private int relationParallelProductCount = 0; // Number of parallel products ||
	private int domainCount = 0; // Number of domain(...) calls
	private int rangeCount = 0; // Number of range(...) calls
	private int projection1Count = 0; // Number of pr1 projections
	private int projection2Count = 0; // Number of pr2 projections
	private int forwardCompositionCount = 0; // Number of forward compositions ;
//	private int backwardCompositionCount = 0; // Number of backward compositions circ (EventB only)
	private int domainRestrictionCount = 0; // Number of domain restriction <|
	private int domainSubtractionCount = 0; // Number of domain subtractions <<|
	private int rangeRestrictionCount = 0; // Number of range restrictions |>
	private int rangeSubtractionCount = 0; // Number of range subtractions |>>
	private int funPartialCount = 0; // Number of partial function definitions +->
	private int funTotalCount = 0; // Number of total function definitions -->
	private int funPartialInjCount = 0; // Number of partial injective function definitions >+>
	private int funTotalInjCount = 0; // Number of total injective function definitions >->
	private int funPartialSurjCount = 0; // Number of partial surjective function definitions +->>
	private int funTotalSurjCount = 0; // Number of total surjective function definitions -->>
	private int funPartialBijCount = 0; // Number of partial bijection function definitions >+>>
	private int funTotalBijCount = 0; // Number of total bijection function definitions >->>
	private int lambdaCount = 0; // Number of lambda abstractions %
	private int functionApplicationCount = 0; // Number of function applications
	private int seqCount = 0; // Number of sequences [...]
	private int iseqCount = 0; // number of isequences
	private int sizeCount = 0; // number of size(...) calls
	private int firstCount = 0; // Number of first(...) calls
	private int tailCount = 0; // Number of tail(...) calls
	private int lastCount = 0; // Number of last(...) calls
	private int frontCount = 0; // Number of front(...) calls
	private int revCount = 0; // Number of sequence reversions
	private int permCount = 0; // Number of sequence permutations
	private int concatCount = 0; // Number of concatenations
	private int frontInsertionCount = 0; // Number of insert_front nodes in AST
	private int tailInsertionCount = 0; // Number of insert_tail nodes in AST
	private int frontRestrictionCount = 0; // Number of restrict_front nodes in AST
	private int tailRestrictionCount = 0; // Number of restrict_tail nodes in AST
	private int generalConcatCount = 0; // Number of general concatenations
	private int closureCount = 0; // Number of closure(...) or closure1(...) calls
	private int iterateCount = 0; // Number of iterate(...) calls
	private IdentifierRelationsHandler identifiers; // Handling of identifiers


	public LargeBASTFeatureData(String predicate) throws NeuroBException {
		this(predicate, new BParser());
	}

	public LargeBASTFeatureData(String predicate, MachineType mt) throws NeuroBException {
		this(predicate, new BParser(), mt);
	}

	public LargeBASTFeatureData(String predicate, BParser bParser) throws NeuroBException{
		this(predicate, bParser, MachineType.CLASSICALB);
	}

	public LargeBASTFeatureData(String predicate, BParser bParser, MachineType mtype) throws NeuroBException {
		this(predicate, bParser, mtype, null);
	}

	public LargeBASTFeatureData(String predicate, BParser bParser, MachineType mtype, StateSpace ss) throws NeuroBException {
		this.bParser = bParser;
		this.mtype = mtype;
		identifiers = new IdentifierRelationsHandler();
		this.ss = ss;
		collectData(predicate);
	}

	private void collectData(String predicate) throws NeuroBException {
		Start ast;
		try {
			ast = bParser.parse(BParser.PREDICATE_PREFIX+" "+predicate, false,
					bParser.getContentProvider());
		} catch (BCompoundException e) {
			throw new NeuroBException("Could not collect features from predicate "+predicate, e);
		}
		collector = new LargeBASTFeatureCollector(this);
		ast.apply(collector);
	}

	public LargeBASTFeatureCollector getFeatureCollector(){
		return collector;
	}

	public int getMaxDepth(){ return collector.getMaxDepth(); }

	public int getConjunctsCount(){ return conjunctsCount; }
	public void incConjunctsCount(){ conjunctsCount++; }

	public int getConjunctionsCount(){ return conjunctionsCount; }
	public void incConjunctionsCount(){ conjunctionsCount++; }

	public int getDisjunctionsCount(){ return disjunctionsCount; }
	public void incDisjunctionsCount(){ disjunctionsCount++; }

	public int getImplicationsCount(){ return implicationsCount; }
	public void incImplicationsCount(){ implicationsCount++; }

	public int getEquivalencesCount(){ return equivalencesCount; }
	public void incEquivalencesCount(){ equivalencesCount++; }

	public int getUniversalQuantifiersCount(){ return uniQuantifierCount; }
	public void incUniversalQuantifiersCount(){ uniQuantifierCount++; }

	public int getExistentialQuantifiersCount(){ return exQuantifierCount; }
	public void incExistentialQuantifiersCount(){ exQuantifierCount++; }

	public int getQuantifierMaxDepthCount(){ return collector.getQuantMaxDepth(); }

	public int getEqualityCount(){ return equalityCount; }
	public void incEqualityCount(){ equalityCount++; }

	public int getInequalityCount(){ return inequalityCount; }
	public void incInequalityCount(){ inequalityCount++; }

	public int getNegationCount(){ return negationCount; }
	public void incNegationCount(){ negationCount++; }

	public int getNegationMaxDepth(){ return collector.getNegMaxDepth(); }

	public int getMemberCount(){ return memberCount; }
	public void incMemberCount(){ memberCount++; }
	public int getNotMemberCount(){ return notMemberCount; }
	public void incNotMemberCount(){ notMemberCount++; }

	public int getSubsetCount(){ return subsetCount; }
	public void incSubsetCount(){ subsetCount++; }
	public int getNotSubsetCount(){ return notSubsetCount; }
	public void incNotSubsetCount(){ notSubsetCount++; }

	public int getSizeComparisonCount(){ return sizeComparisonCount; }
	public void incSizeComparisonCount(){ sizeComparisonCount++; }

	public int getBooleanLiteralsCount(){ return booleanLiteralsCount; }
	public void incBooleanLiteralsCount(){ booleanLiteralsCount++; }

	public int getBooleanConversionCount(){ return booleanConversionCount; }
	public void incBooleanConversionCount(){ booleanConversionCount++; }

	public int getFiniteSetRequirementsCount(){ return finiteSetRequirementsCount;}
	public void incFiniteSetRequirementsCount(){ finiteSetRequirementsCount++; }
	public int getInfiniteSetRequirementsCount(){ return infiniteSetRequirementsCount;}
	public void incInfiniteSetRequirementsCount(){ infiniteSetRequirementsCount++; }

	public int getArithmeticAdditionCount(){return arithmeticAdditionCount;}
	public void incArithmeticAdditionCount(){ arithmeticAdditionCount++;}
	public int getArithmeticMultiplicationCount(){return arithmeticMultiplicationCount;}
	public void incArithmeticMultiplicationCount(){ arithmeticMultiplicationCount++;}
	public int getArithmeticDivisionCount(){return arithmeticDivisionCount;}
	public void incArithmeticDivisionCount(){ arithmeticDivisionCount++;}
	public int getArithmeticModuloCount(){return arithmeticModuloCount;}
	public void incArithmeticModuloCount(){ arithmeticModuloCount++;}
	public int getArithmeticExponentialCount(){ return arithmeticExponentialCount;}
	public void incArithmeticExponentialCount(){ arithmeticExponentialCount++; }

	public int getArithmeticMinCount(){return arithmeticMinCount; }
	public void incArithmeticMinCount(){arithmeticMinCount++; }
	public int getArithmeticMaxCount(){return arithmeticMaxCount; }
	public void incArithmeticMaxCount(){arithmeticMaxCount++; }

	public int getArithmeticGeneralisedSumCount(){ return arithmeticGeneralisedSumCount; }
	public void incArithmeticGeneralisedSumCount(){ arithmeticGeneralisedSumCount++; }

	public int getArithmeticGeneralisedProductCount(){ return arithmeticGeneralisedProductCount; }
	public void incArithmeticGeneralisedProductCount(){ arithmeticGeneralisedProductCount++; }

	public int getSuccCount(){ return succCount; }
	public void incSuccCount(){ succCount++; }
	public int getPredecCount(){ return predecCount; }
	public void incPredecCount(){ predecCount++; }

	/**
	 * Returns the number of added identifiers
	 * @return Number of added identifiers
	 */
	public int getIdentifiersCount(){ return identifiers.getIdCount();}

	/**
	 * Get the amount of relations existing between added identifiers
	 * @return relations between identifiers
	 */
	public int getIdentifierRelationsCount(){ return identifiers.getIdRelationsCount();}

	/**
	 * Returns the amount of identifiers, that have other identifiers as symbolic
	 * lower and upper boundaries. This does not imply that the domain of the identifier
	 * is bounded as well.
	 * @return Number of identifiers with lower and upper bound by other identifiers
	 */
	public int getIdentifierBoundedCount(){ return identifiers.getBoundedIdCount();}

	/**
	 * Returns the amount of identifiers, that have other identifiers either as symbolic
	 * lower or upper boundaries. This does not imply that the domain of the identifier
	 * is bounded as well.
	 * @return Number of identifiers with lower or upper bound (not both) by other identifiers
	 */
	public int getIdentifierSemiBoundedCount(){ return identifiers.getSemiBoundedIdCount();}


	/**
	 * Returns the amount of identifiers, that have no other identifiers as symbolic
	 * lower or upper boundaries. This does not imply that the domain of the identifier
	 * is bounded as well.
	 * @return Number of identifiers without lower and upper bound by other identifiers
	 */
	public int getIdentifierUnboundedCount(){ return identifiers.getUnboundedIdCount();}

	/**
	 * @return Amount of identifiers with bounded domains
	 */
	public int getIdentifierBoundedDomainCount(){ return identifiers.getBoundedDomainsCount();}

	/**
	 * @return Amount of identifiers with either lower or upper bounded domains
	 */
	public int getIdentifierSemiBoundedDomainCount(){
		return identifiers.getSemiBoundedDomainsCount();
	}

	/**
	 * @return Amount of identifiers with neither lower nor upper bounded domains
	 */
	public int getIdentifierUnboundedDomainCount(){ return identifiers.getUnboundedDomainsCount();}

	/**
	 * Add an identifier to be accounted in the feature vector
	 * @param id Name of the identifier
	 */
	public void addIdentifier(String id){ identifiers.addIdentifier(id);}

	/**
	 * Add a lower bounding relation between the two identifiers: id1 < id2
	 * @param id1 Lower bound
	 * @param id2 Upper bound
	 */
	public void addIdentifierLowerBound(String id1, String id2){
		identifiers.addLowerBoundRelation(id1, id2);
	}

	/**
	 * Add an upper bounding relation between the two identifiers: id1 > id2
	 * @param id1 Upper bound
	 * @param id2 Lower bound
	 */
	public void addIdentifierUpperBound(String id1, String id2){
		identifiers.addUpperBoundRelation(id1, id2);
	}

	/**
	 * Mark id1 and id2 as in relation.
	 * @param id1 first identifier
	 * @param id2 second identifier
	 */
	public void addIdentifierRelation(String id1, String id2){
		identifiers.addIdentifierRelation(id1, id2);
	}

	/**
	 * Adds the specified boundaries to the domain of an identifier.
	 * <p>
	 *     This is additive. If an identifier has already a lower bounded domain, and
	 *     addIdentifierDomainBoundaries(id, false, true) is called, the lower bound remains.
	 * </p>
	 * @param id Identifier whose domain shall be altered
	 * @param addLowerBound Whether a lower bound shall be added
	 * @param addUpperBound Whether an upper bound shall be added
	 */
	public void addIdentifierDomainBoundaries(String id, boolean addLowerBound, boolean addUpperBound){
		identifiers.addDomainBoundaries(id, addLowerBound, addUpperBound);
	}

	public int getPowerSetCount(){ return powerSetCount; }
	public void incPowerSetCount(){ powerSetCount++; }

	public int getPowerSetHigherOrderCounts(){ return powerSetHigherOrderCounts; }
	public void incPowerSetHigherOrderCounts(){ powerSetHigherOrderCounts++; }

	public int getMaxPowDepth(){ return collector.getPowMaxDepth(); }

	public int getSetCardCount(){ return setCardCount; }
	public void incSetCardCount(){ setCardCount++; }

	public int getSetUnionCount(){ return setUnionCount; }
	public void incSetUnionCount(){ setUnionCount++; }
	public int getSetIntersectCount(){ return setIntersectCount; }
	public void incSetIntersectCount(){ setIntersectCount++; }

	public int getSetGeneralUnionCount(){ return setGeneralUnionCount; }
	public void incSetGeneralUnionCount(){ setGeneralUnionCount++; }
	public int getSetGeneralIntersectCount(){ return setGeneralIntersectCount; }
	public void incSetGeneralIntersectCount(){ setGeneralIntersectCount++; }

	public int getSetQuantifiedUnionCount(){ return setQuantifiedUnionCount; }
	public void incSetQuantifiedUnionCount(){ setQuantifiedUnionCount++; }
	public int getSetQuantifiedIntersectCount(){ return setQuantifiedIntersectCount; }
	public void incSetQuantifiedIntersectCount(){ setQuantifiedIntersectCount++; }

	public int getSetSubtractionCount(){ return setSubtractionCount; }
	public void incSetSubtractionCount(){ setSubtractionCount++; }

	public int getSetComprehensionCount(){ return setComprehensionCount; }
	public void incSetComprehensionCount(){ setComprehensionCount++; }

	public int getRelationCount(){ return relationCount; }
	public void incRelationCount(){ relationCount++; }
	public int getRelationTotalCount(){ return relationTotalCount; }
	public void incRelationTotalCount(){ relationTotalCount++; }
	public int getRelationSurjCount(){ return relationSurjCount; }
	public void incRelationSurjCount(){ relationSurjCount++; }
	public int getRelationTotalSurjCount(){ return relationTotalSurjCount; }
	public void incRelationTotalSurjCount(){ relationTotalSurjCount++; }

	public int getRelationalImageCount(){ return relationalImageCount; }
	public void incRelationalImageCount(){ relationalImageCount++; }
	public int getRelationInverseCount(){ return relationInverseCount; }
	public void incRelationInverseCount(){ relationInverseCount++; }

	public int getRelationOverrideCount(){ return relationOverrideCount; }
	public void incRelationOverrideCount(){ relationOverrideCount++; }

	public int getRelationDirectProductCount(){ return relationDirectProductCount; }
	public void incRelationDirectProductCount(){ relationDirectProductCount++; }
	public int getRelationParallelProductCount(){ return relationParallelProductCount; }
	public void incRelationParallelProductCount(){ relationParallelProductCount++; }

	public int getDomainCount(){ return domainCount; }
	public void incDomainCount(){ domainCount++; }
	public int getRangeCount(){ return rangeCount; }
	public void incRangeCount(){ rangeCount++; }

	public int getProjection1Count(){ return projection1Count; }
	public void incProjection1Count(){ projection1Count++; }
	public int getProjection2Count() { return projection2Count; }
	public void incProjection2Count() { projection2Count++; }

	public int getForwardCompositionCount() { return forwardCompositionCount; }
	public void incForwardCompositionCount() { forwardCompositionCount++; }
//	public int getBackwardCompositionCount() { return backwardCompositionCount; }
//	public void incBackwardCompositionCount() { backwardCompositionCount++; }

	public int getDomainRestrictionCount() { return domainRestrictionCount; }
	public void incDomainRestrictionCount() { domainRestrictionCount++; }
	public int getDomainSubtractionCount() { return domainSubtractionCount; }
	public void incDomainSubtractionCount() { domainSubtractionCount++; }
	public int getRangeRestrictionCount() { return rangeRestrictionCount; }
	public void incRangeRestrictionCount() { rangeRestrictionCount++; }
	public int getRangeSubtractionCount() { return rangeSubtractionCount; }
	public void incRangeSubtractionCount() { rangeSubtractionCount++; }

	public int getFunPartialCount() { return funPartialCount; }
	public void incFunPartialCount() { funPartialCount++; }
	public int getFunTotalCount() { return funTotalCount; }
	public void incFunTotalCount() { funTotalCount++; }
	public int getFunPartialInjCount() { return funPartialInjCount; }
	public void incFunPartialInjCount() { funPartialInjCount++; }
	public int getFunTotalInjCount() { return funTotalInjCount; }
	public void incFunTotalInjCount() { funTotalInjCount++; }
	public int getFunPartialSurjCount() { return funPartialSurjCount; }
	public void incFunPartialSurjCount() { funPartialSurjCount++; }
	public int getFunTotalSurjCount() { return funTotalSurjCount; }
	public void incFunTotalSurjCount() { funTotalSurjCount++; }
	public int getFunPartialBijCount() { return funPartialBijCount; }
	public void incFunPartialBijCount() { funPartialBijCount++; }
	public int getFunTotalBijCount() { return funTotalBijCount; }
	public void incFunTotalBijCount() { funTotalBijCount++; }

	public int getLambdaCount() { return lambdaCount; }
	public void incLambdaCount() { lambdaCount++; }

	public int getFunctionApplicationCount() { return functionApplicationCount; }
	public void incFunctionApplicationCount() { functionApplicationCount++; }

	public int getSeqCount() { return seqCount; }
	public void incSeqCount() { seqCount++; }
	public int getIseqCount() { return iseqCount; }
	public void incIseqCount() { iseqCount++; }

	public int getSizeCount() { return sizeCount; }
	public void incSizeCount() { sizeCount++; }

	public int getFirstCount() { return firstCount; }
	public void incFirstCount() { firstCount++; }
	public int getTailCount() { return tailCount; }
	public void incTailCount() { tailCount++; }
	public int getLastCount() { return lastCount; }
	public void incLastCount() { lastCount++; }
	public int getFrontCount() { return frontCount; }
	public void incFrontCount() { frontCount++; }

	public int getFrontInsertionCount() { return frontInsertionCount; }
	public void incFrontInsertionCount() { frontInsertionCount++; }
	public int getTailInsertionCount() { return tailInsertionCount; }
	public void incTailInsertionCount() { tailInsertionCount++; }
	public int getFrontRestrictionCount() { return frontRestrictionCount; }
	public void incFrontRestrictionCount() { frontRestrictionCount++; }
	public int getTailRestrictionCount() { return tailRestrictionCount; }
	public void incTailRestrictionCount() { tailRestrictionCount++; }

	public int getRevCount() { return revCount; }
	public void incRevCount() { revCount++; }
	public int getPermCount() { return permCount; }
	public void incPermCount() { permCount++; }

	public int getConcatCount() { return concatCount; }
	public void incConcatCount() { concatCount++; }
	public int getGeneralConcatCount() { return generalConcatCount; }
	public void incGeneralConcatCount() { generalConcatCount++; }


	public int getClosureCount() { return closureCount; }
	public void incClosureCount() { closureCount++; }
	public int getIterateCount() { return iterateCount; }
	public void incIterateCount() { iterateCount++; }
}
