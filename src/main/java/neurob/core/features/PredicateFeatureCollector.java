package neurob.core.features;

import java.util.LinkedList;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.*;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class PredicateFeatureCollector extends DepthFirstAdapter {
	private PredicateFeatureData fd;
	private boolean linked;
	private boolean inNegation;

	public PredicateFeatureCollector() {
		this(new PredicateFeatureData());
	}
	
	public PredicateFeatureCollector(PredicateFeatureData linkedFD) {
		fd = linkedFD;
		linked = true;
		inNegation = false;
	}
	
	
	/**
	 * @return A {@link PredicateFeatureData} object, containing the collected data.
	 */
	public PredicateFeatureData getFeatureData() { 
		return fd;
	}
	
	@Override
	public void caseStart(Start node){
		// If not linked to a specific feature data object, create a new one
		if(!linked){
			// Just set feature counters to zero, so we can use the same collector over and over again for different predicates
			fd = new PredicateFeatureData();
		} else {
			linked = false; // If this is to be used again, the link is no longer valid
		}
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
	 * - Implication and equivalence
	 * - Identifiers
	 * - Sets
	 * - Named Sets
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
		handleGreaterComparison(node.getLeft(), node.getRight());
	}
	@Override
	public void caseAGreaterEqualPredicate(AGreaterEqualPredicate node) {
		fd.incCompOperatorsCount();
		handleGreaterComparison(node.getLeft(), node.getRight());
	}
	@Override
	public void caseALessPredicate(final ALessPredicate node){
		fd.incCompOperatorsCount();
		handleLesserComparison(node.getLeft(), node.getRight());
	}
	@Override
	public void caseALessEqualPredicate(final ALessEqualPredicate node){
		fd.incCompOperatorsCount();
		handleLesserComparison(node.getLeft(), node.getRight());
	}
	/**
	 * Sets boundaries accordingly to identifiers
	 * @param left
	 * @param right
	 */
	private void handleGreaterComparison(PExpression left, PExpression right){
		if(left instanceof AIdentifierExpression){
			LinkedList<TIdentifierLiteral> ids = ((AIdentifierExpression) left).getIdentifier();
			
			// integer literals
			if(right instanceof AIntegerExpression){
				// Upper bounds found
				ids.forEach(rawid -> {
					String id = rawid.toString();
					fd.setUpperBoundRelationToIdentifier(id);
				});
			}
			// TODO Check other identifiers
		} else if(right instanceof AIdentifierExpression){
			LinkedList<TIdentifierLiteral> ids = ((AIdentifierExpression) right).getIdentifier();
			
			// integer literals
			if(left instanceof AIntegerExpression){
				// Upper bounds found
				ids.forEach(rawid -> {
					String id = rawid.toString();
					fd.setUpperBoundRelationToIdentifier(id);
				});
			}
			// TODO Check other identifiers
		}
		
		left.apply(this);
		right.apply(this);
	}
	/**
	 * Sets boundaries accordingly to identifiers
	 * @param left
	 * @param right
	 */
	private void handleLesserComparison(PExpression left, PExpression right){
		if(left instanceof AIdentifierExpression){
			LinkedList<TIdentifierLiteral> ids = ((AIdentifierExpression) left).getIdentifier();
			
			// integer literals
			if(right instanceof AIntegerExpression){
				// Upper bounds found
				ids.forEach(rawid -> {
					String id = rawid.toString();
					fd.setLowerBoundRelationToIdentifier(id);
				});
			}
			// TODO Check other identifiers
		} else if(right instanceof AIdentifierExpression){
			LinkedList<TIdentifierLiteral> ids = ((AIdentifierExpression) right).getIdentifier();
			
			// integer literals
			if(left instanceof AIntegerExpression){
				// Upper bounds found
				ids.forEach(rawid -> {
					String id = rawid.toString();
					fd.setLowerBoundRelationToIdentifier(id);
				});
			}
			// TODO Check other identifiers
		}
		
		left.apply(this);
		right.apply(this);
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
	@Override
	public void caseANegationPredicate(final ANegationPredicate node){
		fd.incNegationsCount();
		node.getPredicate().apply(this);
	}
	@Override
	public void inANegationPredicate(final ANegationPredicate node){
		inNegation = !inNegation;
	}
	@Override
	public void outANegationPredicate(ANegationPredicate node) {
		inNegation = !inNegation;
	}
	
	// Implications
	@Override
	public void caseAImplicationPredicate(final AImplicationPredicate node){
		fd.incImplicationsCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	@Override
	public void caseAEquivalencePredicate(final AEquivalencePredicate node){
		fd.incEquivalencesCount();
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}
	
	// Identifiers
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
		
		// check for domain values
		PExpression left = node.getLeft();
		PExpression right = node.getRight();
		if(left instanceof AIdentifierExpression){
			((AIdentifierExpression) left).getIdentifier()
				.forEach(rawid -> {
					String id = rawid.toString();
					if(inNegation){
						// In negation the domain of no identifier is restricted, as every value could be possible
						fd.setIdentifierDomain(id, false, false);
					}
					else if(right instanceof AIntegerSetExpression){
						// Integer
						fd.setIdentifierDomain(id,false,false);
					} else if(right instanceof AIntSetExpression){
						// Restricted Integers
						fd.setIdentifierDomain(id, true, true);
					} else if(right instanceof ANatSetExpression || right instanceof ANat1SetExpression){
						// Restricted naturals
						fd.setIdentifierDomain(id, true, true);
					} else if(right instanceof ANaturalSetExpression || right instanceof ANatural1SetExpression){
						// Naturals
						fd.setIdentifierDomain(id, true, false);
					} else if(right instanceof ABoolSetExpression){
						// Booleans
						fd.setIdentifierDomain(id, true, true);
					}
				});
		}
		
		left.apply(this);
		right.apply(this);
	}
	@Override
	public void caseANotMemberPredicate(final ANotMemberPredicate node){
		fd.incSetMemberCount();
		fd.incNegationsCount();
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
		fd.incNegationsCount();
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
		fd.incNegationsCount();
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
	
	// Named Sets
	//---
	
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
