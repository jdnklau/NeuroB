package neurob.training.generators.util;

import java.util.ArrayList;

/**
 * <p>This class provides access to reusable methods to get different formulas from a specific input.</p>
 * <p>Mostly it rearranges an input formula to generate variations or extends the input by recombining different parts of it.</p>
 *  
 * @author jannik
 *
 */
public class FormulaGenerator {
	
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
		String properties = String.join(" & ", predicateCollector.getProperties());
		String invariants = String.join(" & ", predicateCollector.getInvariants());
		
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
		String properties = String.join(" & ", PredicateCollector.modifyDomains(predicateCollector.getProperties()));
		String invariants = String.join(" & ", PredicateCollector.modifyDomains(predicateCollector.getInvariants()));
		
		ArrayList<ArrayList<String>> allGuards = predicateCollector.getGuards();
		for(ArrayList<String> guards : allGuards){
			guards = PredicateCollector.modifyDomains(guards);
		}
		
		
		return generateExtendedGuardFormulae(properties, invariants, allGuards);
		
	}
	
	private static ArrayList<String> generateExtendedGuardFormulae(String properties, String invariants, ArrayList<ArrayList<String>> allGuards){
		ArrayList<String> formulae = new ArrayList<String>();
		
		// check for empty formulas
		if(properties.isEmpty()){
//					logger.info("\tNo properties found. Using TRUE=TRUE.");
			properties = "TRUE = TRUE";
		}
		if(invariants.isEmpty()){
//					logger.info("\tNo invariants found. Using TRUE=TRUE.");
			invariants = "TRUE = TRUE";
		}
		
		String negInvariants = "not("+invariants+")";
		String propsAndInvs = "("+String.join(" & ", properties, invariants)+")";
		String propsAndNegInvs = "("+String.join(" & ", properties, negInvariants)+")"; // properties and negated invariants
		
		// Add formulas
		formulae.add(invariants); // invariants
		formulae.add(propsAndInvs); // properties & invariants
		
		// guards
		for(ArrayList<String> guards : allGuards){
			String guard = String.join(" & ", guards);
			
			// only continue if the guards are nonempty
			if(guard.isEmpty()){
				continue;
			}
			
			String negGuard = "not("+guard+")";

			String propsAndGuard = "("+String.join(" & ", properties, guard)+")";
			String propsAndNegGuard = "("+String.join(" & ", properties, negGuard)+")";
			
			formulae.add(propsAndInvs + " & " + guard); // events active w/o violating invariants
			formulae.add(propsAndInvs + " => " + guard); // events usable with unviolated invariants

			formulae.add(propsAndNegInvs + " & " + guard); // events active despite invariant violation
			formulae.add(propsAndNegInvs + " => " + guard); // events usable despite invariant violation
			
			formulae.add(propsAndInvs + " & " + negGuard); // events not active w/o violating invariants
			formulae.add(propsAndInvs + " => " + negGuard); // events not usable with unviolated invariants

			formulae.add(propsAndNegInvs + " & " + negGuard); // events not active despite invariant violation
			formulae.add(propsAndNegInvs + " => " + negGuard); // events not usable despite invariant violation

			formulae.add(propsAndGuard + " => "+ invariants); // events only usable w/o invariant violation
			formulae.add(propsAndGuard + " => "+ negInvariants); // events only usable with invariant violation
			
			formulae.add(propsAndNegGuard + " => "+ invariants); // events never usable w/o invariant violation
			formulae.add(propsAndNegGuard + " => "+ negInvariants); // events never usable with invariant violation
		}
		
		return formulae;
	}

}
