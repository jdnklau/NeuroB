package neurob.training.generators.util;

/**
 * Class used to translate entries of predicate dumps into Java Objects
 * @author jannik
 *
 */
public class PredicateDumpData extends DumpData {
	
	public PredicateDumpData(String predicateDumpString){
		super(predicateDumpString);
		
	}
	
	/**
	 * Returns the predicate from the predicate dump entry.
	 * @return
	 * @deprecated 
	 * 		Use {@link #getSource()} instead. This method
	 * 		remains from legacy code, of a time before 
	 * 		the {@link DumpData} class existed.
	 */
	@Deprecated
	public String getPredicate(){ return getSource(); }
}
