package neurob.core.features.util;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.node.Start;
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
	public static final int featureCount = -1;
	private final BParser bParser;
	private LargeBASTFeatureCollector collector;

	// count of data from predicate
	private int conjunctsCount = 1; // at least one conjunct in predicate
	private int conjunctionsCount = 0; // Number of conjunctions &
	private int disjunctionsCount = 0; // Number of disjunctions or
	private int implicationsCount = 0; // Number of implications =>
	private int equivalencesCount = 0; // Number of equivalences <=>
	private int uniQuantifierCount = 0; // universal quantifiers
	private int exQuantifierCount = 0; // existential quantifiers
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
	private IdentifierRelationsHandler identifiers; // Handling of identifiers


	public LargeBASTFeatureData(String predicate) throws NeuroBException {
		this(predicate, new BParser());
	}

	public LargeBASTFeatureData(String predicate, BParser bParser) throws NeuroBException {
		this.bParser = bParser;
		identifiers = new IdentifierRelationsHandler();
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

	public int getEqualityCount(){ return equalityCount; }
	public void incEqualityCount(){ equalityCount++; }

	public int getInequalityCount(){ return inequalityCount; }
	public void incInequalityCount(){ inequalityCount++; }

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

}
