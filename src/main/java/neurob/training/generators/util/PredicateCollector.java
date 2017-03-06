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
import de.prob.animator.command.WeakestPreconditionCommand;
import de.prob.animator.domainobjects.IBEvalElement;
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
	private ArrayList<ArrayList<String>> guards;
	private ArrayList<String> axioms;
	private ArrayList<String> properties;
	private ArrayList<String> assertions;
	private ArrayList<String> theorems;
	private ArrayList<String> beforeAfterPredicates;
	private ArrayList<String> weakestPreconditions;
	private ArrayList<String> primedInvariants;
	
	private StateSpace ss;
	
	private MachineType machineType;
	
	private static final Logger log = LoggerFactory.getLogger(PredicateCollector.class);
	
	public PredicateCollector(StateSpace ss){
		this.ss = ss;
		machineType = MachineType.getTypeFromStateSpace(ss);
		
		invariants = new ArrayList<String>();
		guards = new ArrayList<ArrayList<String>>();
		axioms = new ArrayList<String>();
		properties = new ArrayList<String>();
		assertions = new ArrayList<String>();
		theorems = new ArrayList<String>();
		beforeAfterPredicates = new ArrayList<String>();
		weakestPreconditions = new ArrayList<String>();
		primedInvariants = new ArrayList<String>();
		
		collectPredicates();
		
	}
	
	private void collectPredicates(){
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
		
		// set up invariants as commands for below
		ArrayList<IBEvalElement> invCmds = new ArrayList<>();
		for(String inv : invariants) {
			try {
				invCmds.add(FormulaGenerator.generateBCommandByMachineType(ss, inv));
			} catch (NeuroBException e) {
				log.warn("\tCould not set up EvalElement from {} for weakest precondition calculation or priming", inv, e);
				continue;
			}
		}
		// full invariant
		IBEvalElement invConjCmd = null;
		String invConj = FormulaGenerator.getStringConjunction(invariants);
		try {
			invConjCmd = FormulaGenerator.generateBCommandByMachineType(ss, invConj);
		} catch (NeuroBException e) {
			log.warn("\tCould not set up EvalElement for invariant conjunct {} for weakest precondition calculation or priming", invConj, e);
		}
		
		// weakest preconditions
		for(BEvent x : comp.getChildrenOfType(BEvent.class)){
			if(x.getName().equals("INITIALISATION"))
				continue; // None for initialisation
			
			WeakestPreconditionCommand wpcc;
			for(IBEvalElement invariant : invCmds){
				try{
					wpcc = new WeakestPreconditionCommand(x.getName(), invariant);
					ss.execute(wpcc);
					weakestPreconditions.add(wpcc.getWeakestPrecondition().getCode());
				} catch(Exception e) {
					log.warn("\tCould not build weakest precondition for {} by event {}.", invariant.getCode(), x.getName(), e);
				}
			}
			
			// weakest pre condition for all invariants
			if(invConjCmd != null){
				try{
					wpcc = new WeakestPreconditionCommand(x.getName(), invConjCmd);
					ss.execute(wpcc);
					weakestPreconditions.add(wpcc.getWeakestPrecondition().getCode());
				} catch(Exception e) {
					log.warn("\tCould not build weakest precondition for {} by event {}.", invConjCmd.getCode(), x.getName(), e);
				}
			}
			
		}
		
		// Before/After predicates
		for(BEvent x : comp.getChildrenOfType(BEvent.class)){
			if(x.getName().equals("INITIALISATION"))
				continue; // None for initialisation
			
			try{
				BeforeAfterPredicateCommand bapc = new BeforeAfterPredicateCommand(x.getName());
				ss.execute(bapc);
				beforeAfterPredicates.add(bapc.getBeforeAfterPredicate().getCode());
			} catch(Exception e) {
				log.warn("\tCould not build Before After Predicate for event {}.", x.getName(), e);
			}
		
		}

//		for(IBEvalElement invariant : invCmds){			
//			try{
//				primedInvariants.add(FormulaGenerator.generatePrimedPredicate(ss, invariant));
//			}catch(Exception e) {
//				log.warn("\tCould not build primed invariant from {}", invariant.getCode(), e);
//			}
//		}
			
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
		rng = new Random(seed); // invariants and primed invariants should be in the same order
		Collections.shuffle(primedInvariants, rng);
		
		Collections.shuffle(properties, rng);
		Collections.shuffle(axioms, rng);
		for(ArrayList<String> g : guards){
			Collections.shuffle(g, rng);
		}
		Collections.shuffle(theorems, rng);
		Collections.shuffle(assertions, rng);
		Collections.shuffle(beforeAfterPredicates, rng);
	}
}
