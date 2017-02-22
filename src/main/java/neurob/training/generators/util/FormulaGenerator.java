package neurob.training.generators.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
		String properties = String.join(" & ", predicateCollector.modifyDomains(predicateCollector.getProperties()));
		String invariants = String.join(" & ", predicateCollector.modifyDomains(predicateCollector.getInvariants()));
		
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
		
		String properties = String.join(" & ", predicateCollector.modifyDomains(predicateCollector.getProperties()));
		String invariants = String.join(" & ", predicateCollector.modifyDomains(predicateCollector.getInvariants()));
		
		// check for empty formulas
		String propertyPre;
		if(properties.isEmpty()){
			propertyPre = "";
		} else {
			propertyPre = properties+" & ";
		}
		String invariantsPre;
		if(invariants.isEmpty()){
			invariantsPre = "";
		} else {
			invariantsPre = invariants+" & ";
		}
		String propsAndInvsPre = propertyPre + invariantsPre;
		
		List<String> allGuards = predicateCollector.getGuards()
				.stream()
				.map(g -> String.join(" & ", g))
				.collect(Collectors.toList());
		
		int guardCount = allGuards.size();
		
		// pairwise iterate over guards
		for(int i=0; i<guardCount; i++){
			for(int j=i+1; j<guardCount; j++){
				String g1 = propsAndInvsPre + allGuards.get(i);
				String g2 = allGuards.get(j);

				formulae.add(g1 + " => " + g2);
				formulae.add(g1 + " <=> " + g2);
				formulae.add(g1 + " => not(" + g2 + ")" );
				formulae.add(g1 + " <=> not(" + g2 + ")" );
			}
		}
		
				
		
		return formulae;
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
		
		
		String propsAndInvs = propertyPre + invariants;
		String propsAndNegInvs = propertyPre + negInvariants;
		

		if(!emptyProperties){
			formulae.add(propsAndInvs); // properties & invariants
		}
		
		// guards
		for(ArrayList<String> guards : allGuards){
			String guard = String.join(" & ", guards);
			
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

			formulae.add(propsAndNegInvs + " & " + negGuard); // events not active with invariant violation
			formulae.add(propsAndNegInvs + " => " + negGuard); // events not usable with invariant violation
			formulae.add(propsAndNegInvs + " <=> " + negGuard); // events not usable with invariant violation
			
			formulae.add(propsAndNegGuard + " => "+ negInvariants); // events never usable with invariant violation

			formulae.add(propsAndGuard + " => "+ negInvariants); // events only usable with invariant violation
		}
		
		return formulae;
	}

}
