package neurob.core.features.util;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.node.Start;
import neurob.exceptions.NeuroBException;

/**
 * This class is for data points in the feature space.
 * 
 * It is intended to apply an instance of {@link TheoryFeatureCollector} on an AST and
 * use the resulting object's {@link TheoryFeatureCollector#getFeatureData() getFeatureData} method.
 * 
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class TheoryFeatureData {
	// Dimensions
	public static final int featureCount = 17;
	// Helpers
	protected IdentifierRelationHandler ids;
	// Features
//	protected int fFormulaLength; // Length of formula (count of operators)
	protected int fExistsQuantifiersCount; // number of existential quantifiers
	protected int fForAllQuantifiersCount; // number of universal quantifiers
	protected int fArithmOperatorsCount; // number of arithmetic operators
	protected int fCompOperatorsCount; // number of comparison operators
	protected int fConjunctionsCount; // number of conjunctions
	protected int fDisjunctionsCount; // number of disjunctions
	protected int fNegationsCount; // number of negations
//	protected int fUniqueIdentifiersCount; // number of unique identifiers used
//	protected int fFiniteSizedDomainIdentifiersCount; // identifiers with finite domain
//	protected int fInfiniteSizedDomainIdentifiersCount; // identifiers with infinite domain
//	protected int fUnknownSizedDomainIdentifiersCount; // identifiers with unknown sized domain
	protected int fSetOperatorsCount; // number of set operators
	protected int fSetMemberCount; // number of memberships to sets
	protected int fFunctionsCount; // number of functions
	protected int fRelationOperatorsCount;
	protected int fImplicationsCount; // count implications used (=>)
	protected int fEquivalencesCount; // count equivalences used (<=>)
	// parser
	private BParser parser;
	
	public TheoryFeatureData(String predicate, BParser parser) throws NeuroBException {
		this(parser);
		collectData(predicate);
	}
	
	public TheoryFeatureData(String predicate) throws NeuroBException{
		this(predicate, new BParser());
	}
	
	public TheoryFeatureData(){
		this(new BParser());
	}
	
	public TheoryFeatureData(BParser parser) {
		this.parser = parser;
		
		ids = new IdentifierRelationHandler();
		
		// set initial values
//		fFormulaLength = 0;
		// Quantifiers
		fExistsQuantifiersCount = 0;
		fForAllQuantifiersCount = 0;
		// Operators
		fArithmOperatorsCount = 0;
		fCompOperatorsCount = 0;
		fConjunctionsCount = 0;
		fDisjunctionsCount = 0;
		fNegationsCount = 0;
		// implications
		fImplicationsCount = 0;
		fEquivalencesCount = 0;
		// identifiers
//		fUniqueIdentifiersCount = 0;
//		fFiniteSizedDomainIdentifiersCount = 0;
//		fInfiniteSizedDomainIdentifiersCount = 0;
//		fUnknownSizedDomainIdentifiersCount = 0;
		// Sets
		fSetOperatorsCount = 0;
		fSetMemberCount = 0;
		// functions
		fFunctionsCount = 0;
		fRelationOperatorsCount = 0;
	}
	
	@Override
	public String toString(){
		ArrayList<Integer> features = new ArrayList<Integer>();
		
		//features.add(fFormulaLength);
		features.add(fArithmOperatorsCount);
		features.add(fCompOperatorsCount);
		features.add(fForAllQuantifiersCount);
		features.add(fExistsQuantifiersCount);
		features.add(fConjunctionsCount);
		features.add(fDisjunctionsCount);
		features.add(fNegationsCount);
		features.add(fSetOperatorsCount);
		features.add(fSetMemberCount);
		features.add(fFunctionsCount);
		features.add(fRelationOperatorsCount);
		features.add(getUniqueIdentifiersCount());
		features.add(ids.getFiniteDomainIdentifiersCount());
		features.add(ids.getInfiniteDomainIdentifiersCount());
		features.add(ids.getUnknownDomainSizeIdentifiersCount());
		features.add(fImplicationsCount);
		features.add(fEquivalencesCount);
		
		return features.stream()
				.map(i -> i.toString())
				.collect(Collectors.joining(","));
	}
	
	public void collectData(String predicate) throws NeuroBException {
		Start ast;
		try {
			ast = parser.parse(BParser.PREDICATE_PREFIX+" "+predicate, false, parser.getContentProvider());
		} catch (Exception e) {
			throw new NeuroBException("Could not collect feature data from predicate "+predicate, e);
		}
		
		ast.apply(new TheoryFeatureCollector(this));
		
	}
	
	public final double[] toArray(){
		double[] features = new double[]{
				fArithmOperatorsCount,
				fCompOperatorsCount,
				fForAllQuantifiersCount,
				fExistsQuantifiersCount,
				fConjunctionsCount,
				fDisjunctionsCount,
				fNegationsCount,
				fSetOperatorsCount,
				fSetMemberCount,
				fFunctionsCount,
				fRelationOperatorsCount,
				getUniqueIdentifiersCount(),
				ids.getFiniteDomainIdentifiersCount(),
				ids.getInfiniteDomainIdentifiersCount(),
				ids.getUnknownDomainSizeIdentifiersCount(),
				fImplicationsCount,
				fEquivalencesCount
		};
		return features;
	}
	
	public final INDArray toNDArray(){
		return Nd4j.create(toArray());
	}
	
	/*
	 * Following the get-methods for the feature values and their corresponding increment methods
	 */

	public final int getExistsQuantifiersCount(){ return fExistsQuantifiersCount; }
	public  final void incExistsQuantifiersCount(){ fExistsQuantifiersCount++; }
	
	public final int getForAllQuantifiersCount(){ return fForAllQuantifiersCount; }
	public final void incForAllQuantifiersCount(){ fForAllQuantifiersCount++; }

	public final int getArithmOperatorsCount(){ return fArithmOperatorsCount; }
	public final void incArithmOperatorsCount(){ fArithmOperatorsCount++; }

	public final int getCompOperatorsCount(){ return fCompOperatorsCount; }
	public final void incCompOperatorsCount(){ fCompOperatorsCount++; }

	public final int getConjunctionsCount(){ return fConjunctionsCount; }
	public final void incConjunctionsCount(){ fConjunctionsCount++; }

	public final int getDisjunctionsCount(){ return fDisjunctionsCount; }
	public final void incDisjunctionsCount(){ fDisjunctionsCount++; }

	public final int getNegationsCount(){ return fNegationsCount; }
	public final void incNegationsCount(){ fNegationsCount++; }

	public final int getSetOperatorsCount(){ return fSetOperatorsCount; }
	public final void incSetOperatorsCount(){ fSetOperatorsCount++; }

	public final int getSetMemberCount(){ return fSetMemberCount; }
	public final void incSetMemberCount(){ fSetMemberCount++; }

	public final int getFunctionsCount(){ return fFunctionsCount; }
	public final void incFunctionsCount(){ fFunctionsCount++; }

	public final int getRelationOperatorsCount(){ return fRelationOperatorsCount; }
	public final void incRelationOperatorsCount(){ fRelationOperatorsCount++; }

	public final int getUniqueIdentifiersCount(){ return ids.getUniqueIdentifierCount(); }
	public final void addIdentifier(String id){ ids.addIdentifier(id);}

	public final int getImplicationsCount(){ return fImplicationsCount; }
	public final void incImplicationsCount(){ fImplicationsCount++;}

	public final int getEquivalencesCount(){ return fEquivalencesCount; }
	public final void incEquivalencesCount(){ fEquivalencesCount++;}

	public final void setIdentifierDomain(String id, boolean hasLowerBound, boolean hasUpperBound) {
		ids.addBoundariesToIdentifier(id, hasLowerBound, hasUpperBound);
	}
	public final void setUpperBoundRelationToIdentifier(String id){
		ids.addBoundariesToIdentifier(id, false, true);
	}
	public final void setUpperBoundRelationToIdentifier(String id, String restrictingID){
		// TODO
	}
	public final void setLowerBoundRelationToIdentifier(String id){
		ids.addBoundariesToIdentifier(id, true, false);
	}
	public final void setLowerBoundRelationToIdentifier(String id, String restrictingID){
		// TODO
	}

}
