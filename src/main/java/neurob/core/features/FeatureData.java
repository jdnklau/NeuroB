package neurob.core.features;

import java.util.ArrayList;
import java.util.stream.Collectors;

import de.be4.classicalb.core.parser.node.TIdentifierLiteral;
import neurob.core.features.helpers.IdentifierRelationHandler;

/**
 * This class is for data points in the feature space.
 * 
 * It is intended to apply an instance of {@link FeatureCollector} on an AST and
 * use the resulting object's {@link FeatureCollector#getFeatureData() getFeatureData} method.
 * 
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class FeatureData {
	// Dimensions
	public static final int featureCount = 15;
	// Helpers
	private IdentifierRelationHandler ids;
	// Features
	private int fFormulaLength; // Length of formula (count of operators)
	private int fExistsQuantifiersCount; // number of existential quantifiers
	private int fForAllQuantifiersCount; // number of universal quantifiers
	private int fArithmOperatorsCount; // number of arithmetic operators
	private int fCompOperatorsCount; // number of comparison operators
	private int fConjunctionsCount; // number of conjunctions
	private int fDisjunctionsCount; // number of disjunctions
	private int fUniqueIdentifiersCount; // number of unique identifiers used
	private int fFiniteSizedDomainIdentifiersCount; // identifiers with finite domain
	private int fInfiniteSizedDomainIdentifiersCount; // identifiers with infinite domain
	private int fUnknownSizedDomainIdentifiersCount; // identifiers with unknown sized domain
	private int fSetOperatorsCount; // number of set operators
	private int fSetMemberCount; // number of memberships to sets
	private int fFunctionsCount; // number of functions
	private int fRelationOperatorsCount;

	public FeatureData() {
		ids = new IdentifierRelationHandler();
		
		// set initial values
		fFormulaLength = 0;
		// Quantifiers
		fExistsQuantifiersCount = 0;
		fForAllQuantifiersCount = 0;
		// Operators
		fArithmOperatorsCount = 0;
		fCompOperatorsCount = 0;
		fConjunctionsCount = 0;
		fDisjunctionsCount = 0;
		// identifiers
		fUniqueIdentifiersCount = 0;
		fFiniteSizedDomainIdentifiersCount = 0;
		fInfiniteSizedDomainIdentifiersCount = 0;
		fUnknownSizedDomainIdentifiersCount = 0;
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
		
		features.add(fFormulaLength);
		features.add(fArithmOperatorsCount);
		features.add(fCompOperatorsCount);
		features.add(fForAllQuantifiersCount);
		features.add(fExistsQuantifiersCount);
		features.add(fConjunctionsCount);
		features.add(fDisjunctionsCount);
		features.add(fSetOperatorsCount);
		features.add(fSetMemberCount);
		features.add(fFunctionsCount);
		features.add(fRelationOperatorsCount);
		features.add(getUniqueIdentifiersCount());
		features.add(ids.getFiniteDomainIdentifiersCount());
		features.add(ids.getInfiniteDomainIdentifiersCount());
		features.add(ids.getUnknownDomainSizeIdentifiersCount());
		
		return features.stream()
				.map(i -> i.toString())
				.collect(Collectors.joining(","));
	}
	
	/**
	 * Returns a String, that lists a detailed overview of the single features and their values.
	 * @return
	 */
	public String getFeatureOverviewText(){
		String s = "Formula Length: ? (NYI)\n"
				+ "Arithmetic Operators: "+fArithmOperatorsCount+ "\n"
				+ "Comparison Operators: "+fCompOperatorsCount+ "\n"
				+ "Universal Quantifiers: "+fForAllQuantifiersCount+ "\n"
				+ "Existential Quantifiers: "+fExistsQuantifiersCount+"\n"
				+ "Conjunctions: "+fConjunctionsCount+"\n"
				+ "Disjunctions: "+fDisjunctionsCount+"\n"
				+ "Unique Identifiers: "+getUniqueIdentifiersCount()+"\n";
		return s;
	}
	
	/*
	 * Following the get-methods for the feature values and their corresponding increment methods
	 */

	public int getExistsQuantifiersCount(){ return fExistsQuantifiersCount; }
	public void incExistsQuantifiersCount(){ fExistsQuantifiersCount++; }
	
	public int getForAllQuantifiersCount(){ return fForAllQuantifiersCount; }
	public void incForAllQuantifiersCount(){ fForAllQuantifiersCount++; }
	
	public int getArithmOperatorsCount(){ return fArithmOperatorsCount; }
	public void incArithmOperatorsCount(){ fArithmOperatorsCount++; }
	
	public int getCompOperatorsCount(){ return fCompOperatorsCount; }
	public void incCompOperatorsCount(){ fCompOperatorsCount++; }
	
	public int getConjunctionsCount(){ return fConjunctionsCount; }
	public void incConjunctionsCount(){ fConjunctionsCount++; }
	
	public int getDisjunctionsCount(){ return fDisjunctionsCount; }
	public void incDisjunctionsCount(){ fDisjunctionsCount++; }
	
	public int getSetOperatorsCount(){ return fSetOperatorsCount; }
	public void incSetOperatorsCount(){ fSetOperatorsCount++; }
	
	public int getSetMemberCount(){ return fSetMemberCount; }
	public void incSetMemberCount(){ fSetMemberCount++; }
	
	public int getFunctionsCount(){ return fFunctionsCount; }
	public void incFunctionsCount(){ fFunctionsCount++; }
	
	public int getRelationOperatorsCount(){ return fRelationOperatorsCount; }
	public void incRelationOperatorsCount(){ fRelationOperatorsCount++; }
	
	public int getUniqueIdentifiersCount(){ return ids.getUniqueIdentifierCount(); }
	public void addIdentifier(String id){ ids.addIdentifier(id);}

	public void setIdentifierDomain(String id, boolean hasLowerBound, boolean hasUpperBound) {
		ids.addBoundariesToIdentifier(id, hasLowerBound, hasUpperBound);
	}
	public void setUpperBoundRelationToIdentifier(String id){
		ids.addBoundariesToIdentifier(id, false, true);
	}
	public void setUpperBoundRelationToIdentifier(String id, String restrictingID){
		// TODO
	}
	public void setLowerBoundRelationToIdentifier(String id){
		ids.addBoundariesToIdentifier(id, true, false);
	}
	public void setLowerBoundRelationToIdentifier(String id, String restrictingID){
		// TODO
	}

}
