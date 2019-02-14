package de.hhu.stups.neurob.training.analysis;

/**
 * @param <S> Type of samples to be analysed
 * @param <D> Type of the analysis data implementation
 */
public interface AnalysisData<S, D extends AnalysisData<S, D>> {


    /**
     * Adds a sample to the analysis.
     * <p>
     * Returns itself for method chaining.
     *
     * @param sample
     *
     * @return Reference to this analysis.
     */
    D add(S sample);

    /**
     * Merges the data of the other analysis into this.
     * <p>
     * Returns reference to itself for method chaining.
     *
     * @param other Analysis data to merge into this.
     *
     * @return
     */
    D mergeWith(D other);
}
