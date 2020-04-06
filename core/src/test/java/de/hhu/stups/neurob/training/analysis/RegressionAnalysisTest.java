package de.hhu.stups.neurob.training.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegressionAnalysisTest {

    @Test
    void shouldBeUnsortedAfterAddingSample() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();
        analysis.add(1L);
        assertFalse(analysis.isSorted);
    }

    @Test
    void shouldHaveNoSamples() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        assertEquals(0, analysis.getSampleCount(), "Should be empty.");
    }

    @Test
    void shouldCountNewSamples() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();
        analysis.add(1L)
                .add(2L)
                .add(-2L);

        assertEquals(3, analysis.getSampleCount(), "Should be empty.");
    }

    @Test
    void shouldBeGreaterWhenLong() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        assertTrue(analysis.greaterThan(2L, 1L));
    }

    @Test
    void shouldBeGreaterWhenDouble() {
        RegressionAnalysis<Double> analysis = new RegressionAnalysis<>();

        assertTrue(analysis.greaterThan(2., 1.));
    }

    @Test
    void shouldFindMinimum() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(3L).add(1L).add(2L);

        Long expected = 1L;
        Long actual = analysis.getMinimum();

        assertEquals(expected, actual);
    }

    @Test
    void shouldFindMaximum() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(3L).add(5L).add(2L);

        Long expected = 5L;
        Long actual = analysis.getMaximum();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateAverageOfSingleSample() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(1L);

        Double expected = 1.0;
        Double actual = analysis.getAverage();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateAverage() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(1L).add(2L).add(3L).add(4L);

        Double expected = 2.5;
        Double actual = analysis.getAverage();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateMedianFromOddNumberOfSamples() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(3L).add(5L).add(2L);

        Double expected = 3.;
        Double actual = analysis.getMedian();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateMedianFromEvenNumberOfSamples() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(1L).add(2L).add(3L).add(4L);

        Double expected = 2.5;
        Double actual = analysis.getMedian();

        assertEquals(expected, actual);
    }

    @Test
    void shouldHaveSingeElementAsMedianWhenOnlyOneSample() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(1L);

        Double expected = 1.;
        Double actual = analysis.getMedian();

        assertEquals(expected, actual);
    }

    @Test
    void shouldSortForMedian() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();
        analysis.add(1L).add(2L).add(3L).add(4L).getMedian();
        assertTrue(analysis.isSorted);
    }

    @Test
    void shouldCalculateFirstQuartileFromOddNumberOfSamples() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(6L).add(7L).add(15L)
                .add(36L).add(39L)
                .add(40L)
                .add(41L).add(42L)
                .add(43L).add(47L).add(49L);

        Double expected = 25.5;
        Double actual = analysis.getFirstQuartile();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateFirstQuartileFromEvenNumberOfSamples() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(7L).add(15L).add(36L).add(39L).add(40L).add(41L);

        Double expected = 15.;
        Double actual = analysis.getFirstQuartile();

        assertEquals(expected, actual);
    }

    @Test
    void shouldSortForFirstQuartile() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();
        analysis.add(1L).add(2L).add(3L).add(4L).getFirstQuartile();
        assertTrue(analysis.isSorted);
    }

    @Test
    void shouldHaveSingeElementAsFirstQuartineWhenOnlyOneSample() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(1L);

        Double expected = 1.;
        Double actual = analysis.getFirstQuartile();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateThirdQuartileFromOddNumberOfSamples() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(6L).add(7L).add(15L)
                .add(36L).add(39L)
                .add(40L)
                .add(41L).add(42L)
                .add(43L).add(47L).add(49L);

        Double expected = 42.5;
        Double actual = analysis.getThirdQuartile();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateThirdQuartileFromEvenNumberOfSamples() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(7L).add(15L).add(36L).add(39L).add(40L).add(41L);

        Double expected = 40.;
        Double actual = analysis.getThirdQuartile();

        assertEquals(expected, actual);
    }

    @Test
    void shouldCalculateThirdQuartileWhenQuartileIsAverageOfTwoPoints() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(7L).add(15L).add(36L).add(40L);

        Double expected = 38.;
        Double actual = analysis.getThirdQuartile();

        assertEquals(expected, actual);
    }

    @Test
    void shouldHaveSingeElementAsThirdQuartineWhenOnlyOneSample() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();

        analysis.add(1L);

        Double expected = 1.;
        Double actual = analysis.getThirdQuartile();

        assertEquals(expected, actual);
    }

    @Test
    void shouldSortForThirdQuartile() {
        RegressionAnalysis<Long> analysis = new RegressionAnalysis<>();
        analysis.add(1L).add(2L).add(3L).add(4L).getThirdQuartile();
        assertTrue(analysis.isSorted);
    }

    @Test
    void shouldMergeTwoAnalyses() {
        RegressionAnalysis<Integer> analysis = new RegressionAnalysis<>();
        analysis.add(1).add(1).add(2).add(5);

        RegressionAnalysis<Integer> other = new RegressionAnalysis<>();
        other.add(0).add(4).add(7).add(9).add(34);

        // 0 1 1 2 4 5 7 9 29

        analysis.mergeWith(other);

        assertAll(
                () -> assertEquals(9, analysis.getSampleCount(),
                        "Sample count does not match"),
                () -> assertEquals(new Integer(0), analysis.getMinimum(),
                        "Minimum does not match"),
                () -> assertEquals(new Integer(34), analysis.getMaximum(),
                        "Maximum does not match"),
                () -> assertEquals(new Double(4), analysis.getMedian(),
                        "Median does not match"),
                () -> assertEquals(new Double(1), analysis.getFirstQuartile(),
                        "FirstQuartile does not match"),
                () -> assertEquals(new Double(7), analysis.getThirdQuartile(),
                        "ThirdQuartile does not match"),
                () -> assertEquals(7, analysis.getAverage(), Math.ulp(7.),
                        "Average does not match")
        );
    }

    @Test
    void shouldMergeToEqualAnalysisAsIfAllDataWentThroughOne() {
        RegressionAnalysis<Integer> oneStep = new RegressionAnalysis<>();
        oneStep.add(1).add(1).add(2).add(5);
        oneStep.add(0).add(4).add(7).add(9).add(34);

        RegressionAnalysis<Integer> merged = new RegressionAnalysis<>();
        merged.add(1).add(1).add(2).add(5);
        RegressionAnalysis<Integer> other = new RegressionAnalysis<>();
        other.add(0).add(4).add(7).add(9).add(34);

        merged.mergeWith(other);

        assertEquals(oneStep, merged);

    }

    @Test
    void shouldBeUnsortedAfterMerge() {
        RegressionAnalysis<Integer> analysis = new RegressionAnalysis<>();
        analysis.add(1).add(1).add(2).add(5);

        RegressionAnalysis<Integer> other = new RegressionAnalysis<>();
        other.add(0).add(4).add(7).add(9).add(34);

        analysis.getMedian(); // Should be sorted now, covered by other test case
        analysis.mergeWith(other);

        assertFalse(analysis.isSorted);
    }

    @Test
    void shouldNotBeEqual() {
        RegressionAnalysis<Integer> analysis = new RegressionAnalysis<>();
        analysis.add(1).add(1).add(2).add(5);
        RegressionAnalysis<Integer> other = new RegressionAnalysis<>();
        other.add(0).add(4).add(7).add(9).add(34);

        assertNotEquals(analysis, other);
    }

    @Test
    void shouldBeEqual() {
        RegressionAnalysis<Integer> analysis = new RegressionAnalysis<>();
        analysis.add(0).add(4).add(7).add(9).add(34);
        RegressionAnalysis<Integer> other = new RegressionAnalysis<>();
        other.add(0).add(4).add(7).add(9).add(34);

        assertEquals(analysis, other);
    }
}
