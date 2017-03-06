package neurob.training.generators.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.prob.animator.command.PrimePredicateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.statespace.StateSpace;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;

/**
 * <p>This class provides access to reusable methods to get different formulas from a specific input.</p>
 * <p>Mostly it rearranges an input formula to generate variations or extends the input by recombining different parts of it.</p>
 *  
 * @author jannik
 *
 */
public class FormulaGenerator {
	
	/**
	 * Creates an {@link IBEvalElement} for command creation for ProB2 with respect to the machine type.
	 * <p>
	 * If you are using a state space of a B machine, it is advised to use {@link #generateBCommandByMachineType(StateSpace, String)}
	 * instead
	 * @param mt Machine type the command should get parsed in
	 * @param formula Formula to create an evaluation element from
	 * @return
	 * @throws NeuroBException
	 * @see {@link #generateBCommandByMachineType(StateSpace, String)}
	 */
	public static IBEvalElement generateBCommandByMachineType(MachineType mt, String formula) throws NeuroBException{
		IBEvalElement cmd;
		try {
			switch(mt){
			case EVENTB:
				cmd = new EventB(formula);
				break;
			default:
			case CLASSICALB:
				cmd = new ClassicalB(formula);
			}
		} catch(Exception e){
			throw new NeuroBException("Could not set up command for evaluation from formula "+formula, e);
		}
		return cmd;
	}
	
	/**
	 * Creates an {@link IBEvalElement} for command creation for ProB2 with respect to a given StateSpace.
	 * <p>
	 * If you got no StateSpace, use {@link #generateBCommandByMachineType(MachineType, String)}
	 * @param ss StateSpace over which the eval element will be created
	 * @param formula Formula to create an evaluation element from
	 * @return
	 * @throws NeuroBException
	 * @see {@link #generateBCommandByMachineType(MachineType, String)}
	 */
	public static IBEvalElement generateBCommandByMachineType(StateSpace ss, String formula) throws NeuroBException{
		try {
			return (IBEvalElement) ss.getModel().parseFormula(formula);
		} catch(Exception e){
			throw new NeuroBException("Could not set up command for evaluation from formula "+formula, e);
		}
	}
	
	/**
	 * Takes a given predicate and primes the identifiers.
	 * <p>
	 * Example: "x>y & y>x" will be translated into "x'>y' & y'>x'"
	 * <p>
	 * This is mainly useful for before-after predicates.
	 * @param ss
	 * @param predicate
	 * @return
	 * @throws NeuroBException
	 */
	public static String generatePrimedPredicate(StateSpace ss, String predicate) throws NeuroBException{
		return generatePrimedPredicate(ss, generateBCommandByMachineType(ss, predicate));
	}
	
	/**
	 * Takes a given {@link IBEvalElement} and primes the identifiers.
	 * <p>
	 * Example: "x>y & y>x" will be translated into "x'>y' & y'>x'"
	 * <p>
	 * This is mainly useful for before-after predicates.
	 * @param ss
	 * @param evalElement
	 * @return
	 * @throws NeuroBException
	 */
	public static String generatePrimedPredicate(StateSpace ss, IBEvalElement evalElement) throws NeuroBException{
		try{
			PrimePredicateCommand ppc = new PrimePredicateCommand(evalElement);
			ss.execute(ppc);
			return ppc.getPrimedPredicate().getCode();
		}catch(Exception e) {
			throw new NeuroBException("Could not build primed predicate from "+ evalElement.getCode(), e);
		}
	}
	
	/**
	 * <p>Generates multiple formulas for each event found.</p>
	 * <p>Expects a {@link PredicateCollector}, that already was used on a machine to collect data.
	 * The found predicates are then combined to generate multiple formulae revolving around guards and events.
	 * <br>
	 * Generated formulae are e.g.:
	 * <ul>
	 * <li>invariants</li>
	 * <li>properties & invariants</li>
	 * <li>properties & invariants & guard</li>
	 * <li>properties & invariants => guard</li>
	 * <li>properties & invariants & not guard</li>
	 * <li>properties & invariants => not guard</li>
	 * <li>properties & guard => invariants</li>
	 * <li>properties & guard => not invariants</li>
	 * <li>properties & not guard => invariants</li>
	 * <li>properties & not guard => not invariants</li>
	 * </ul></p>
	 * <p>Usage example:
	 * <pre>
	 * {@code
	 * // load machine and main component ...
	 * PredicateCollector pc = new PredicateCollector(mainComponent);
	 * ArrayList<String> formulae = FormulaGenerator.extendedGuardFormulae(pc);
	 * for(String formula : formulae) {
	 *     // do stuff
	 * }
	 * </pre></p>
	 * @param predicateCollector An already used {@link PredicateCollector}
	 * @return An ArrayList containing all formulae constructed from the predicate collector
	 */
	public static ArrayList<String> extendedGuardFormulae(PredicateCollector predicateCollector){
		String properties = getPropertyString(predicateCollector);
		String invariants = getInvariantString(predicateCollector);
		
		return generateExtendedGuardFormulae(properties, invariants, predicateCollector.getGuards());
		
	}
	
	/**
	 * <p>Generates multiple formulas for each event found.</p>
	 * <p>See {@link #extendedGuardFormulae(PredicateCollector)} for details. This method
	 * differs only by calling {@link PredicateCollector#modifyDomains(ArrayList)} on all predicates beforehand.
	 * </p>
	 * @param predicateCollector
	 * @return
	 * @see #extendedGuardFormulae(PredicateCollector)
	 * @see PredicateCollector#modifyDomains(ArrayList)
	 */
	public static ArrayList<String> extendedGuardFomulaeWithInfiniteDomains(PredicateCollector predicateCollector){
		String properties = getPropertyString(predicateCollector);
		String invariants = getInvariantString(predicateCollector);
		
		ArrayList<ArrayList<String>> allGuards = predicateCollector.getGuards();
		for(ArrayList<String> guards : allGuards){
			guards = predicateCollector.modifyDomains(guards);
		}
		
		
		return generateExtendedGuardFormulae(properties, invariants, allGuards);
		
	}
	
	/**
	 * Generates multiple formulae containing each possible pairing of guards
	 * @param predicateCollector
	 * @return
	 */
	public static ArrayList<String> multiGuardFormulae(PredicateCollector predicateCollector){
		ArrayList<String> formulae = new ArrayList<String>();
		
		String propsAndInvsPre = getPropsAndInvsPre(predicateCollector);
		
		List<String> allGuards = predicateCollector.getGuards()
				.stream()
				.map(FormulaGenerator::getStringConjunction)
				.collect(Collectors.toList());
		
		int guardCount = allGuards.size();
		
		// pairwise iterate over guards
		for(int i=0; i<guardCount; i++){
			for(int j=i+1; j<guardCount; j++){
				String g1 = allGuards.get(i);
				String g2 = allGuards.get(j);

				formulae.add(propsAndInvsPre + "(" + g1 + " => " + g2 + ")");
				formulae.add(propsAndInvsPre + "(" + g1 + " <=> " + g2 + ")");
				formulae.add(propsAndInvsPre + "(" + g1 + " => not(" + g2 + "))" );
				formulae.add(propsAndInvsPre + "(" + g1 + " <=> not(" + g2 + "))" );
			}
		}
		
				
		
		return formulae;
	}
	
	public static ArrayList<String> assertionsAndTheorems(PredicateCollector predicateCollector){
		String propsAndInv = getPropertyAndInvariantString(predicateCollector);
		ArrayList<String> formulae = new ArrayList<>();

		ArrayList<String> assertionsList = predicateCollector.getAssertions();
		ArrayList<String> theoremsList = predicateCollector.getTheorems();
		String allAssertions = getStringConjunction(assertionsList);
		String allTheorems = getStringConjunction(theoremsList);
		
		if(propsAndInv.isEmpty()){
			for(String a : assertionsList){
				formulae.add(a);
			}
			for(String a : theoremsList){
				formulae.add(a);
			}
			
			if(!allAssertions.isEmpty())
				formulae.add(allAssertions);
			if(!allTheorems.isEmpty())
				formulae.add(allTheorems);
			
			return formulae;
		}
		
		// proof assertions
		for(String a : assertionsList){
			formulae.add(propsAndInv + " & " + a);
			formulae.add(propsAndInv + " => " + a);
			formulae.add(propsAndInv + " <=> " + a);
		}
		if(!allAssertions.isEmpty()){
			formulae.add(propsAndInv + " & " + allAssertions);
			formulae.add(propsAndInv + " => " + allAssertions);
			formulae.add(propsAndInv + " <=> " + allAssertions);
		}
		
		// proof theorems
		for(String a : theoremsList){
			formulae.add(propsAndInv + " & " + a);
			formulae.add(propsAndInv + " => " + a);
			formulae.add(propsAndInv + " <=> " + a);
		}
		if(!allTheorems.isEmpty()){
			formulae.add(propsAndInv + " & " + allTheorems);
			formulae.add(propsAndInv + " => " + allTheorems);
			formulae.add(propsAndInv + " <=> " + allTheorems);
		}
		
		return formulae;
	}
	
	/**
	 * Takes a list of Strings and joins them with " & " as delimiter.
	 * Each conjunct will be wrapped in parenthesis
	 * @param conjuncts
	 * @return
	 */
	public static String getStringConjunction(List<String> conjuncts){
		String conj = String.join(") & (", conjuncts);
		return (conj.isEmpty()) ? "" : "("+conj+")";
	}
	
	private static String getPropertyString(PredicateCollector predicateCollector){
		return getStringConjunction(predicateCollector.getProperties());
	}
	
	private static String getInvariantString(PredicateCollector predicateCollector){
		return getStringConjunction(predicateCollector.getInvariants());
	}
	
	private static String getPropertyPre(PredicateCollector predicateCollector){
		String properties = getPropertyString(predicateCollector);
		
		if(properties.isEmpty()){
			return "";
		} else {
			return properties+" & ";
		}
	}
	
	private static String getInvariantsPre(PredicateCollector predicateCollector){
		String invariants = getInvariantString(predicateCollector);
		
		if(invariants.isEmpty()){
			return "";
		} else {
			return invariants+" & ";
		}
	}
	
	private static String getPropsAndInvsPre(PredicateCollector predicateCollector){
		return getPropertyPre(predicateCollector)+getInvariantsPre(predicateCollector);
	}
	
	private static String getPropertyAndInvariantString(PredicateCollector predicateCollector){
		String inv =  getInvariantString(predicateCollector);
		
		if(inv.isEmpty())
			return getPropertyString(predicateCollector);
		
		
		return getPropertyPre(predicateCollector) + inv;
	}
	
	private static ArrayList<String> generateExtendedGuardFormulae(String properties, String invariants, ArrayList<ArrayList<String>> allGuards){
		ArrayList<String> formulae = new ArrayList<String>();
		
		// check for empty formulas
		boolean emptyProperties = false;
		String propertyPre;
		if(properties.isEmpty()){
			emptyProperties = true;
			propertyPre = "";
		} else {
			propertyPre = properties+" & ";
		}
		
		
		boolean emptyInvariants = false;
		String invariantsPre;
		String negInvariants;
		if(invariants.isEmpty()){
			emptyInvariants = true;
			invariantsPre = "";
			negInvariants = "";
		} else {
			formulae.add(invariants); // invariants
			invariantsPre = invariants+" & ";
			negInvariants= "not("+invariants+")";
		}
		
		
		String propsAndInvs = (invariants.isEmpty()) ? properties : propertyPre + invariants;
		String propsAndNegInvs = (invariants.isEmpty()) ? properties : propertyPre + negInvariants; 
		
		
		// guards
		for(ArrayList<String> guards : allGuards){
			String guard = getStringConjunction(guards);
			
			// only continue if the guards are nonempty
			if(guard.isEmpty()){
				continue;
			}
			
			String negGuard = "not("+guard+")";

			String propsAndGuard;
			propsAndGuard = propertyPre + guard;
			String propsAndNegGuard = propertyPre + negGuard;
			
			formulae.add(propertyPre + invariantsPre + guard); // events active w/o violating invariants
			// following code only makes sense if invariants or properties are not empty
			if(emptyInvariants && emptyProperties){
				continue;
			}

			formulae.add(propsAndInvs + " => " + guard); // events usable with unviolated invariants
			formulae.add(propsAndInvs + " <=> " + guard); // events usable iff invariants unviolated
			
			formulae.add(propsAndInvs + " & " + negGuard); // events not active w/o violating invariants
			formulae.add(propsAndInvs + " => " + negGuard); // events not usable with unviolated invariants
			formulae.add(propsAndInvs + " <=> " + negGuard); // events not usable iff invariants unviolated
			

			formulae.add(propsAndGuard + " => "+ invariants); // events only usable w/o invariant violation
			
			formulae.add(propsAndNegGuard + " => "+ invariants); // events never usable w/o invariant violation

			if(emptyInvariants){
				// incomming formulae would be repetitive, so skip them
				continue;
			}
			
			formulae.add(propsAndNegInvs + " & " + guard); // events active despite invariant violation
			formulae.add(propsAndNegInvs + " => " + guard); // events usable despite invariant violation
			formulae.add(propsAndNegInvs + " <=> " + guard); // events usable despite invariant violation

			formulae.add(propsAndNegInvs + " & " + negGuard);// events not active with invariant violation
			formulae.add(propsAndNegInvs + " => " + negGuard); // events not usable with invariant violation
			formulae.add(propsAndNegInvs + " <=> " + negGuard); // events not usable with invariant violation
			
			formulae.add(propsAndNegGuard + " => "+ negInvariants); // events never usable with invariant violation

			formulae.add(propsAndGuard + " => "+ negInvariants); // events only usable with invariant violation
		}
		
		return formulae;
	}

}
