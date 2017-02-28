package neurob.training.generators.util;

import java.util.ArrayList;

import neurob.training.TrainingPredicateDumper;

/**
 * Class used to translate entries of predicate dumps into Java Objects
 * @author jannik
 *
 */
public class PredicateDumpData {
	private ArrayList<Long> labellings;
	private String predicate;
	
	public PredicateDumpData(String predicateDumpString){
		int splitAt = predicateDumpString.indexOf(':');
		
		// split string into labels and predicate
		String labelStr = predicateDumpString.substring(0, splitAt);
		predicate = predicateDumpString.substring(splitAt+1); // +1 to skip the first :
		
		// generate list of solver times
		labellings = new ArrayList<>();
		for(String entry : labelStr.split(",")){
			labellings.add(Long.parseLong(entry));
		}
		
	}
	
	/**
	 * Returns the labellings from the predicate dump entry.
	 * <p>
	 * Labels are returned for each solver in the order stated for {@link TrainingPredicateDumper}.
	 * @return
	 */
	public ArrayList<Long> getLabellings(){ return labellings; }
	/**
	 * Returns the predicate from the predicate dump entry.
	 * @return
	 */
	public String getPredicate(){ return predicate; }
}
