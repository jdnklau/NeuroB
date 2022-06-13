package de.hhu.stups.neurob.training.db;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SamplingStatistic {
    public final double mean;
    public final double stdev;
    public final double sem;
    public final int sampleSize;

    public SamplingStatistic(int sampleSize, double mean, double stdev, double sem) {
        this.mean = mean;
        this.stdev = stdev;
        this.sem = sem;
        this.sampleSize = sampleSize;
    }

    public SamplingStatistic(boolean isPopulation, Double... measures) {
        this(Arrays.stream(measures).collect(Collectors.toList()), isPopulation);
    }

    public SamplingStatistic(List<Double> measurements, boolean isPopulation) {
        if (measurements.size() == 0) {
            throw new IllegalArgumentException("List of measurements cannot be empty");
        } else if (measurements.size() == 1) {
            this.mean = measurements.get(0);
            this.stdev = 0.;
            this.sem = 0.;
            this.sampleSize = 1;
        } else {

            int biasCorrection = isPopulation ? 0 : 1;

            this.sampleSize = measurements.size();
            int biasCorrectedSize = sampleSize - biasCorrection;

            double sumOfMeasures = measurements.stream().reduce(0., Double::sum);
            this.mean = sumOfMeasures / sampleSize;
            double variance = measurements.stream()
                                      .map(m -> Math.pow(m - mean, 2))
                                      .reduce(0., Double::sum) / biasCorrectedSize; // Sample variance, not population
            this.stdev = Math.sqrt(variance);
            this.sem = stdev / Math.sqrt(sampleSize);
        }
    }

    public double getMean() {
        return mean;
    }

    public double getStdev() {
        return stdev;
    }

    public double getSem() {
        return sem;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SamplingStatistic that = (SamplingStatistic) o;
        return Double.compare(that.mean, mean) == 0 && Double.compare(that.stdev, stdev) == 0 && Double.compare(that.sem, sem) == 0 && sampleSize == that.sampleSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mean, stdev, sem, sampleSize);
    }

    @Override
    public String toString() {
        return "SamplingStatistic{" +
               "mean=" + mean +
               ", stdev=" + stdev +
               ", sem=" + sem +
               ", sampleSize=" + sampleSize +
               '}';
    }
}
