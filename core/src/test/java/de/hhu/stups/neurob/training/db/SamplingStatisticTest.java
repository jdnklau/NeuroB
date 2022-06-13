package de.hhu.stups.neurob.training.db;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SamplingStatisticTest {

    @Test
    void shouldCalculatePopulationStats() {
        List<Double> samples = new ArrayList<>();
        samples.add(1000.);
        samples.add(1200.);
        samples.add(820.);
        samples.add(1300.);
        samples.add(680.);

        double expectedMean = 1000.;
        double expectedVariance = 52960.;
        double expectedStdev = Math.sqrt(expectedVariance);
        double expectedSem = expectedStdev / Math.sqrt(5);

        SamplingStatistic expected = new SamplingStatistic(
                5, expectedMean, expectedStdev, expectedSem);

        SamplingStatistic actual = new SamplingStatistic(samples, true);

        assertEquals(expected, actual);
    }


    @Test
    void shouldCalculateSampleStats() {
        List<Double> samples = new ArrayList<>();
        samples.add(1000.);
        samples.add(1200.);
        samples.add(820.);
        samples.add(1300.);
        samples.add(680.);

        double expectedMean = 1000.;
        double expectedVariance = 66200.;
        double expectedStdev = Math.sqrt(expectedVariance);
        double expectedSem = expectedStdev / Math.sqrt(5);

        SamplingStatistic expected = new SamplingStatistic(
                5, expectedMean, expectedStdev, expectedSem);

        SamplingStatistic actual = new SamplingStatistic(samples, false);

        assertEquals(expected, actual);
    }
}
