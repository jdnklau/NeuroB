package neurob.training.generators.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.node.Start;
import de.prob.animator.command.BeforeAfterPredicateCommand;
import de.prob.animator.command.PrimePredicateCommand;
import de.prob.animator.command.WeakestPreconditionCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.classicalb.Assertion;
import de.prob.model.classicalb.PrettyPrinter;
import de.prob.model.classicalb.Property;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Axiom;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Guard;
import de.prob.model.representation.Invariant;
import de.prob.statespace.StateSpace;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;

public class PredicateCollector {
	private ArrayList<String> invariants;
	private ArrayList<String> preds;
	private ArrayList<ArrayList<String>> guards;
	private ArrayList<String> axioms;
	private ArrayList<String> properties;
	private ArrayList<String> assertions;
	private ArrayList<String> theorems;
	private ArrayList<String> beforeAfterPredicates;
	private ArrayList<String> weakestPreconditions;
	private ArrayList<String> primedInvariants;
	
	private MachineType machineType;
	
	private static final Logger log = LoggerFactory.getLogger(PredicateCollector.class);
	
	public PredicateCollector(StateSpace ss){
		preds = new ArrayList<String>();
		invariants = new ArrayList<String>();
		guards = new ArrayList<ArrayList<String>>();
		axioms = new ArrayList<String>();
		properties = new ArrayList<String>();
		assertions = new ArrayList<String>();
		theorems = new ArrayList<String>();
		beforeAfterPredicates = new ArrayList<String>();
		weakestPreconditions = new ArrayList<String>();
		primedInvariants = new ArrayList<String>();

		machineType = MachineType.getTypeFromStateSpace(ss);
		
		collectPredicates(ss);
	}
	
	private void collectPredicates(StateSpace ss){
		AbstractElement comp = ss.getMainComponent();
		// properties
		for(Property x : comp.getChildrenOfType(Property.class)){
			properties.add(x.getFormula().getCode());
		}
		
		// add invariants
		for(Invariant x : comp.getChildrenOfType(Invariant.class)){
			if(x.isTheorem())
				theorems.add(x.getFormula().getCode());
			else
				invariants.add(x.getFormula().getCode());
			
		}
		
		for(Assertion x : comp.getChildrenOfType(Assertion.class)){
			assertions.add(x.getFormula().getCode());
		}
		
		// for each event collect guards
		for(BEvent x : comp.getChildrenOfType(BEvent.class)){
			ArrayList<String> event = new ArrayList<String>();
			for(Guard g : x.getChildrenOfType(Guard.class)){
				event.add(g.getFormula().getCode());
			}
			if(!event.isEmpty())
				guards.add(event);
		}
		// axioms
		for(Axiom x : comp.getChildrenOfType(Axiom.class)){
			axioms.add(x.getFormula().getCode());
		}
		
		// weakest preconditions
		for(BEvent x : comp.getChildrenOfType(BEvent.class)){
			if(x.getName().equals("INITIALISATION"))
				continue; // None for initialisation
			
			WeakestPreconditionCommand wpcc;
			for(String inv : invariants){
				IEvalElement invariant;
				switch(machineType){
				case EVENTB:
					invariant = new EventB(inv);
					break;
				default:
				case CLASSICALB:
					invariant = new ClassicalB(inv);
					break;
				}				
				
				wpcc = new WeakestPreconditionCommand(x.getName(), invariant);
				try{
					ss.execute(wpcc);
					weakestPreconditions.add(wpcc.getWeakestPrecondition().getCode());
				} catch(Exception e) {
					log.warn("\tCould not build weakest precondition for {} by event {}: {}", inv, x.getName(), e.getMessage(), e);
				}
				
				
			}
			
			// weakest pre condition for all invariants
			IEvalElement invariant;
			String inv = FormulaGenerator.getStringConjunction(invariants);
			try {
				invariant = FormulaGenerator.generateBCommandByMachineType(machineType, inv);
			} catch (NeuroBException e) {
				log.warn("\t{}", e.getMessage(), e);
				continue; // next entry in loop
			}				
			
			wpcc = new WeakestPreconditionCommand(x.getName(), invariant);
			try{
				ss.execute(wpcc);
				weakestPreconditions.add(wpcc.getWeakestPrecondition().getCode());
			} catch(Exception e) {
				log.warn("\tCould not build weakest precondition for {} by event {}: {}", inv, x.getName(), e.getMessage(), e);
			}
			
		}
		
		// Before/After predicates
		for(BEvent x : comp.getChildrenOfType(BEvent.class)){
			if(x.getName().equals("INITIALISATION"))
				continue; // None for initialisation
			
			BeforeAfterPredicateCommand bapc = new BeforeAfterPredicateCommand(x.getName());
			try{
				ss.execute(bapc);
				beforeAfterPredicates.add(bapc.getBeforeAfterPredicate().getCode());
			} catch(Exception e) {
				log.warn("\tCould not build Before After Predicate for event {}: {}", x.getName(), e.getMessage(), e);
			}
		
		}

		for(String inv : invariants){
			IEvalElement invariant;
			switch(machineType){
			case EVENTB:
				invariant = new EventB(inv);
				break;
			default:
			case CLASSICALB:
				invariant = new ClassicalB(inv);
				break;
			}
			
			PrimePredicateCommand ppc = new PrimePredicateCommand(invariant);
			try{
				ss.execute(ppc);
				primedInvariants.add(ppc.getPrimedPredicate().getCode());
			}catch(Exception e) {
				log.warn("\tCould not build primed invariant from {}: {}", inv, e.getMessage(), e);
			}
		}
			
	}

	public ArrayList<ArrayList<String>> getGuards(){ return guards; }
	public ArrayList<String> getInvariants(){ return invariants; }
	public ArrayList<String> getProperties(){ return properties; }
	public ArrayList<String> getAssertions(){ return assertions; }
	public ArrayList<String> getTheorems(){ return theorems; }
	public ArrayList<String> getBeforeAfterPredicates(){ return beforeAfterPredicates; }
	public ArrayList<String> getWeakestPreConditions(){ return weakestPreconditions; }
	public ArrayList<String> getPrimedInvariants(){ return primedInvariants; }

	/**
	 * Modifies an ArrayList of predicates to have only Numbers of infinite domains.
	 * This means, that types like NAT and INT are replaced by NATURAL and INTEGER in the typing predicates.
	 * <p>
	 * Note that this is only supported if the PredicateCollector was initially called on a 
	 * Classical B statespace. Otherwise the returned list will be empty.
	 * @param invariants Predicates (usually invariants) to modify
	 * @return The modified input or an empty array list for non-classical B.
	 */
	public ArrayList<String> modifyDomains(ArrayList<String> invariants){
		ArrayList<String> modifiedList = new ArrayList<String>();
		
		switch(machineType){
		case CLASSICALB:
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
			break;
		
		case EVENTB:
			// TODO
		default:
			break;
			
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
