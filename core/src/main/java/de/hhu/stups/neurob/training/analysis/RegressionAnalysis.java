package de.hhu.stups.neurob.training.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Single valued regression analysis.
 * Samples the minimum, 1st quartile, median, 3rd quartile, maximum and average.
 */
public class RegressionAnalysis<N extends Number> {
    private List<N> samples;
    private N minimum;
    private N maximum;
    private Double average;
    /** Whether the list is sorted. Quartiles can only be taken if the list is sorted. */
    Boolean isSorted;

    public RegressionAnalysis() {
        this.samples = new ArrayList<>();
        this.isSorted = true;
    }

    /**
     * Returns the number of samples collected.
     *
     * @return
     */
    public int getSampleCount() {
        return samples.size();
    }

    public N getMinimum() {
        return minimum;
    }

    public N getMaximum() {
        return maximum;
    }

    public Double getAverage() {
        return average;
    }

    public Double getMedian() {
        return getMedianOfSlice(samples, 0,samples.size()-1);
    }

    public Double getFirstQuartile() {
        // Find index for splicing
        int size = samples.size();
        int index = (size %2 == 0) ? size/2-1 : size/2;

        return getMedianOfSlice(samples, 0, index);
    }

    public Double getThirdQuartile() {
        // Find index for splicing
        int size = samples.size();
        int index = size/2;

        return getMedianOfSlice(samples, index, size-1);
    }

    Double getMedianOfSlice(List<N> samps, int startIndex, int endIndex) {
        assureSorted();

        // End and start should make sense.
        if (endIndex < startIndex) {
            int tmp = endIndex;
            endIndex = startIndex;
            startIndex = tmp;
        }

        // Find middle index.
        int size = endIndex - startIndex + 1; // + 1 because indices count from one
        int index = size / 2 + startIndex; // + start for offset.

        // Median is dependent on number of elements in list.
        if (size == 0) {
            return null;
        } else if (size % 2 == 1) {
            return samps.get(index).doubleValue();
        } else {
            return (samps.get(index - 1).doubleValue() + samps.get(index).doubleValue()) / 2;
        }
    }

    /**
     * If the samples are not sorted, do so.
     */
    void assureSorted() {
        if (!isSorted) {
            samples.sort(this::compare);
            isSorted = true;
        }
    }

    /**
     * Adds a sample to the analysis.
     * <p>
     * Returns itself for method chaining.
     *
     * @param sample
     *
     * @return Reference to this analysis.
     */
    public RegressionAnalysis<N> add(N sample) {
        isSorted = false; // Due to ArrayList implementation.
        samples.add(sample);
        adjustMinimum(sample);
        adjustMaximum(sample);
        adjustAverage(sample);
        return this;
    }

    /**
     * Sets new minimum if sample is the smallest so far.
     *
     * @param sample
     */
    void adjustMinimum(N sample) {
        if (minimum == null || greaterThan(minimum, sample)) {
            minimum = sample;
        }
    }

    /**
     * Sets new maximum if sample is the greatest so far.
     *
     * @param sample
     */
    void adjustMaximum(N sample) {
        if (maximum == null || greaterThan(sample, maximum)) {
            maximum = sample;
        }
    }

    boolean greaterThan(N lhs, N rhs) {
        return compare(lhs, rhs) > 0;
    }

    int compare(N lhs, N rhs) {
        long l1 = lhs.longValue();
        long l2 = rhs.longValue();
        if (l1 != l2)
            return (l1 > l2 ? 1 : -1);
        return Double.compare(lhs.doubleValue(), rhs.doubleValue());

    }

    void adjustAverage(N sample) {
        if (average == null) {
            average = sample.doubleValue();
            return;
        }

        // Adjust average by adding the correct fraction to it.
        average += (sample.doubleValue() - average) / (samples.size());
    }

}
