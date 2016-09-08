package neurob.training.generators.helpers;

import java.util.ArrayList;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Axiom;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Guard;
import de.prob.model.representation.Invariant;

public class PredicateCollector {
	private ArrayList<String> invariants;
	private ArrayList<String> preds;
	private ArrayList<ArrayList<String>> guards;
	private ArrayList<String> axioms;

	public PredicateCollector(AbstractElement comp) {
		preds = new ArrayList<String>();
		invariants = new ArrayList<String>();
		guards = new ArrayList<ArrayList<String>>();
		axioms = new ArrayList<String>();
		
		collectPredicates(comp);
	}
	
	private void collectPredicates(AbstractElement comp){
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

}
