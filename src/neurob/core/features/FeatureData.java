package neurob.core.features;



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
	private int fFormulaLength; // Length of formula (count of operators)
	private int fExistsQuantifiersCount; // number of existential quantifiers
	private int fForAllQuantifiersCount; // number of universal quantifiers
	private int fArithmOperatorsCount; // number of arithmetic operators
	private int fCompOperatorsCount; // number of comparison operators
	private int fConjunctionsCount; // number of conjunctions
	private int fDisjunctionsCount; // number of disjunctions

	public FeatureData() {
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
		
	}
	
	@Override
	public String toString(){
		String s = "Formula Length: ? (NYI)\n"
				+ "Arithmetic Operators: "+fArithmOperatorsCount+ "\n"
				+ "Comparison Operators: "+fCompOperatorsCount+ "\n"
				+ "Universal Quantifiers: "+fForAllQuantifiersCount+ "\n"
				+ "Existential Quantifiers: "+fExistsQuantifiersCount+"\n"
				+ "Conjunctions: "+fConjunctionsCount+"\n"
				+ "Disjunctions: "+fDisjunctionsCount+"\n";
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

}
