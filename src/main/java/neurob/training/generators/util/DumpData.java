package neurob.training.generators.util;

import java.util.ArrayList;
import java.util.List;

public class DumpData {
	protected ArrayList<Double> labellings;
	protected String source;

	public DumpData(String dumpString) {
		int splitAt = dumpString.indexOf(':');

		// split string into labels and source
		String labelStr = dumpString.substring(0, splitAt);
		source = dumpString.substring(splitAt+1); // +1 to skip the first :

		// generate list of solver times
		labellings = new ArrayList<>();
		for(String entry : labelStr.split(",")){
			labellings.add(Double.parseDouble(entry));
		}
	}

	/**
	 * Returns the labellings from the predicate dump entry as List.
	 * <p>
	 * Labels are returned for each solver in the order stated for the respective dumper
	 * that created the sample.
	 * @return List of labellings
	 */
	public List<Double> getLabellingsList(){ return labellings; }

	/**
	 * Returns the labellings from the predicate dump entry.
	 * <p>
	 * Labels are returned for each solver in the order stated for the respective dumper
	 * that created the sample.
	 * @return List of labellings
	 */
	public double[] getLabellings(){
		return getLabellingsList().stream().mapToDouble(d->d.doubleValue()).toArray();
	}
	/**
	 * Returns the source from the dump entry, that produced the
	 * respective labelling.
	 * @return Source string, from which the labelling was produced initially.
	 */
	public String getSource(){ return source; }
}
