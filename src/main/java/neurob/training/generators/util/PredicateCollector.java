package neurob.training.generators.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.node.Start;
import de.prob.model.classicalb.PrettyPrinter;
import de.prob.model.classicalb.Property;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Axiom;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Guard;
import de.prob.model.representation.Invariant;
import neurob.exceptions.NeuroBException;

public class PredicateCollector {
	private ArrayList<String> invariants;
	private ArrayList<String> preds;
	private ArrayList<ArrayList<String>> guards;
	private ArrayList<String> axioms;
	private ArrayList<String> properties;

	public PredicateCollector(AbstractElement comp) {
		preds = new ArrayList<String>();
		invariants = new ArrayList<String>();
		guards = new ArrayList<ArrayList<String>>();
		axioms = new ArrayList<String>();
		properties = new ArrayList<String>();
		
		collectPredicates(comp);
	}
	
	private void collectPredicates(AbstractElement comp){
		// properties
		for(Property x : comp.getChildrenOfType(Property.class)){
			properties.add(x.getFormula().getCode());
		}
		
		// add invariants
		for(Invariant x : comp.getChildrenOfType(Invariant.class)){
			invariants.add(x.getFormula().getCode());
		}
		
		// for each event collect guards
		for(BEvent x : comp.getChildrenOfType(BEvent.class)){
			ArrayList<String> event = new ArrayList<String>();
			for(Guard g : x.getChildrenOfType(Guard.class)){
				event.add(g.getFormula().getCode());
			}
			guards.add(event);
		}
		// axioms
		for(Axiom x : comp.getChildrenOfType(Axiom.class)){
			axioms.add(x.getFormula().getCode());
		}
			
	}

	public ArrayList<ArrayList<String>> getGuards(){ return guards; }
	public ArrayList<String> getInvariants(){ return invariants; }
	public ArrayList<String> getProperties(){ return properties; }

	/**
	 * Modifies an ArrayList of predicates to have only Numbers of infinite domains.
	 * This means, that types like NAT and INT are replaced by NATURAL and INTEGER in the typing predicates.
	 * @param invariants Predicates (usually invariants) to modify
	 * @return
	 */
	public static ArrayList<String> modifyDomains(ArrayList<String> invariants){
		ArrayList<String> modifiedList = new ArrayList<String>();
		for(String invariant : invariants){
			Start ast;
			try {
				ast = BParser.parse(BParser.PREDICATE_PREFIX + invariant);
			} catch (BCompoundException e) {
				// do nothing but skip this
				continue;
			}
			
			ast.apply(new ClassicalBIntegerDomainReplacer());
			PrettyPrinter pp = new PrettyPrinter();
			ast.apply(pp);
			
			modifiedList.add(pp.getPrettyPrint());
		}
		return modifiedList;
	}
	
	/**
	 * Mixes the individual entries of all the predicate lists inside.
	 * <p>
	 * This has no result on the data despite changing their order.
	 * Intention is to use this for image generation from the formulae, to get a greater variance of images produced. 
	 * @param seed
	 */
	public void shuffleConjunctions(long seed){
		Random rng = new Random(seed);
		Collections.shuffle(invariants, rng);
		Collections.shuffle(properties, rng);
		Collections.shuffle(preds, rng);
		Collections.shuffle(axioms, rng);
		for(ArrayList<String> g : guards){
			Collections.shuffle(g, rng);
		}
	}
}
