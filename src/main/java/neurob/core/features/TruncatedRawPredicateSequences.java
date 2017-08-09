package neurob.core.features;

import neurob.exceptions.NeuroBException;

/**
 * Takes the raw predicates, truncates them to the first N characters only, and then
 * translates them to features for an RNN.
 *
 * @author Jannik Dunkelau
 */
public class TruncatedRawPredicateSequences extends RawPredicateSequences {
	private int tlength;

	/**
	 * Constructor setting truncate length to 1500
	 */
	public TruncatedRawPredicateSequences() {
		this(1500); // NOTE: if this value is changed, change JavaDoc also
	}

	@Override
	public String getDataPathIdentifier() {
		return this.getClass().getSimpleName()+tlength;
	}

	public int getTruncateLength(){ return tlength; }

	/**
	 * Sets the truncate length to the specified value
	 * @param tlength Length of truncated sequences
	 */
	public TruncatedRawPredicateSequences(int tlength) {
		this.tlength = tlength;
	}

	@Override
	public double[] generateFeatureArray(String source) throws NeuroBException {
		return super.generateFeatureArray(source);
	}
}
