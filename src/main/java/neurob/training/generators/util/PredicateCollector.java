package neurob.training.generators.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private List<String> invariants;
	private Map<String, List<String>> guards;
	private List<String> axioms;
	private List<String> properties;
	private List<String> assertions;
	private List<String> theorems;
	private List<String> beforeAfterPredicates;
	private Map<String, List<String>> weakestPreconditions;
	private Map<String, String> primedInvariants;
	
	private StateSpace ss;
	
	private MachineType machineType;
	
	private static final Logger log = LoggerFactory.getLogger(PredicateCollector.class);
	
	public PredicateCollector(StateSpace ss){
		this.ss = ss;
		machineType = MachineType.getTypeFromStateSpace(ss);
		
		invariants = new ArrayList<>();
		guards = new HashMap<String, List<String>>();
		axioms = new ArrayList<>();
		properties = new ArrayList<>();
		assertions = new ArrayList<>();
		theorems = new ArrayList<>();
		beforeAfterPredicates = new ArrayList<>();
		weakestPreconditions = new HashMap<>();
		primedInvariants = new HashMap<>();
		
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
				guards.put(x.getName(), event);
		}
		// axioms
		for(Axiom x : comp.getChildrenOfType(Axiom.class)){
			axioms.add(x.getFormula().getCode());
		}
		
		// set up invariants as commands for below
		Map<String, IBEvalElement> invCmds = new HashMap<>();
		for(String inv : invariants) {
			try {
				invCmds.put(inv, FormulaGenerator.generateBCommandByMachineType(ss, inv));
			} catch (NeuroBException e) {
				log.warn("\tCould not set up EvalElement from {} for weakest precondition calculation or priming", inv, e);
				continue;
			}
		}
		
		// weakest preconditions for each invariant
		for(String inv : invCmds.keySet()){
			IBEvalElement invCmd = invCmds.get(inv);
			

			List<String> wpcs = new ArrayList<>();
			for(BEvent x : comp.getChildrenOfType(BEvent.class)){
				if(x.getName().equals("INITIALISATION"))
					continue; // None for initialisation
				
				try{
					WeakestPreconditionCommand wpcc = new WeakestPreconditionCommand(x.getName(), invCmd);
					ss.execute(wpcc);
					wpcs.add(wpcc.getWeakestPrecondition().getCode());
				} catch(Exception e) {
					log.warn("\tCould not build weakest precondition for {} by event {}.", invCmd.getCode(), x.getName(), e);
				}
				
			}
			weakestPreconditions.put(inv, wpcs);
		}
		
		// Weakest precondition for all invariants conjuncted
		IBEvalElement invConjCmd = null;
		String invConj = FormulaGenerator.getStringConjunction(invariants);
		try {
			invConjCmd = FormulaGenerator.generateBCommandByMachineType(ss, invConj);
		} catch (NeuroBException e) {
			log.warn("\tCould not set up EvalElement for invariant conjunct {} for weakest precondition calculation or priming", invConj, e);
		}
		if(invConjCmd !=null){
			List<String> wpcs = new ArrayList<>();
			for(BEvent x : comp.getChildrenOfType(BEvent.class)){
				if(x.getName().equals("INITIALISATION"))
					continue; // None for initialisation
				
				try{
					WeakestPreconditionCommand wpcc = new WeakestPreconditionCommand(x.getName(), invConjCmd);
					ss.execute(wpcc);
					wpcs.add(wpcc.getWeakestPrecondition().getCode());
				} catch(Exception e) {
					log.warn("\tCould not build weakest precondition for {} by event {}.", invConjCmd.getCode(), x.getName(), e);
				}
			}
			weakestPreconditions.put(invConj, wpcs);
		}
		
		
		if(machineType != MachineType.EVENTB)
			return; // FIXME: allow usage of classical B, too
		
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

		for(String inv : invCmds.keySet()){
			IBEvalElement invCmd = invCmds.get(inv);
			try{
				primedInvariants.put(inv, FormulaGenerator.generatePrimedPredicate(ss, invCmd));
			}catch(Exception e) {
				log.warn("\tCould not build primed invariant from {}", inv, e);
			}
		}
			
	}
	
	public Map<String, List<String>> getGuards(){ return guards; }
	public List<String> getInvariants(){ return invariants; }
	public List<String> getProperties(){ return properties; }
	public List<String> getAssertions(){ return assertions; }
	public List<String> getTheorems(){ return theorems; }
	public List<String> getBeforeAfterPredicates(){ return beforeAfterPredicates; }
	public Map<String, List<String>> getWeakestPreConditions(){ return weakestPreconditions; }
	public Map<String, String> getPrimedInvariants(){ return primedInvariants; }
	public StateSpace accessStateSpace(){ return ss; }

	/**
	 * Modifies an ArrayList of predicates to have only Numbers of infinite domains.
	 * This means, that types like NAT and INT are replaced by NATURAL and INTEGER in the typing predicates.
	 * <p>
	 * Note that this is only supported if the PredicateCollector was initially called on a 
	 * Classical B statespace. Otherwise the returned list will be empty.
	 * @param invariants Predicates (usually invariants) to modify
	 * @return The modified input or an empty array list for non-classical B.
	 */
	public List<String> modifyDomains(List<String> invariants){
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
}
