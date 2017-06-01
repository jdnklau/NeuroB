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
	private int finiteSetRequirementsCount = 0; // Number of finite(...) calls
	private int infiniteSetRequirementsCount = 0; // Number of negated finite(...) calls
	private int partitionCount = 0; // Number of partitions


	public LargeBASTFeatureData(String predicate) throws NeuroBException {
		this(predicate, new BParser());
	}

	public LargeBASTFeatureData(String predicate, BParser bParser) throws NeuroBException {
		this.bParser = bParser;
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

	public int getFiniteSetRequirementsCount(){ return finiteSetRequirementsCount;}
	public void incFiniteSetRequirementsCount(){ finiteSetRequirementsCount++; }
	public int getInfiniteSetRequirementsCount(){ return infiniteSetRequirementsCount;}
	public void incInfiniteSetRequirementsCount(){ infiniteSetRequirementsCount++; }

	public int getPartitionCount(){ return partitionCount; }
	public void incPartitionCount(){ partitionCount++; }

}
