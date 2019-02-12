package de.hhu.stups.neurob.training.analysis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationAnalysisTest {

    @Test
    void shouldYieldSingleListForSingleEntry() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        List<Integer> expected = new ArrayList<>();
        expected.add(2);
        List<Integer> actual = analysis.getKey(2);

        assertEquals(expected, actual);
    }

    @Test
    void shouldYieldKeysInInitialisationOrder() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        List<Integer> expected = new ArrayList<>();
        expected.add(1);
        expected.add(3);
        List<Integer> actual = analysis.getKey(3, 1);

        assertEquals(expected, actual);
    }

    @Test
    void shouldHaveNoEntries() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        Long expected = 0L;
        Long actual = analysis.getCount(1);
        assertEquals(expected, actual);
    }

    @Test
    void shouldHaveNoEntriesWhenOtherClassWasAddedTo() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        analysis.add(1).add(1);

        Long expected = 0L;
        Long actual = analysis.getCount(2);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseClassCounter() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        analysis.add(1).add(1);

        Long expected = 2L;
        Long actual = analysis.getCount(1);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseCombinedClassCountForMultilabelClassification() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        analysis.add(1, 2).add(1, 2);

        Long expected = 2L;
        Long actual = analysis.getCount(1, 2);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseCombinedClassCountForAffectedClassesOnly() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        analysis.add(1, 2).add(1, 2).add(1, 3).add(2, 3);

        Long expected = 1L;
        Long actual = analysis.getCount(1, 3);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseIndividualClassCountForMultiLabelClassification() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        analysis.add(1, 2).add(1, 2).add(1, 3);

        Long expected = 3L;
        Long actual = analysis.getCount(1);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseClassCountForEachSubsetInMultiLabelClassification() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        analysis.add(1, 2).add(1, 2).add(1, 3).add(2).add(2, 3).add(1, 2, 3);

        assertAll("Subset sizes should match",
                () -> assertEquals(new Long(4L), analysis.getCount(1),
                        "Class 1 does not match."),
                () -> assertEquals(new Long(5L), analysis.getCount(2),
                        "Class 2 does not match."),
                () -> assertEquals(new Long(3L), analysis.getCount(3),
                        "Class 3 does not match."),
                () -> assertEquals(new Long(3L), analysis.getCount(1, 2),
                        "Class 1+2 does not match."),
                () -> assertEquals(new Long(2L), analysis.getCount(1, 3),
                        "Class 1+3 does not match."),
                () -> assertEquals(new Long(2L), analysis.getCount(2, 3),
                        "Class 2+3 does not match."),
                () -> assertEquals(new Long(1L), analysis.getCount(1, 2, 3),
                        "Class 1+2+3 does not match.")
        );
    }

    @Test
    void shouldGetSingleValueAsSubset() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        List<Integer> expected = new ArrayList<>();
        expected.add(1);
        List<Integer> actual = analysis.getAllSubsets(1)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    @Test
    void shouldGetSubsetsOfTuple() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        List<List<Integer>> expected = new ArrayList<>();
        expected.add(toList(1));
        expected.add(toList(2));
        expected.add(toList(1, 2));

        List<List<Integer>> actual = analysis.getAllSubsets(1, 2).collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    @Test
    void shouldGetSubsetsOfTriple() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>(1, 2, 3);

        List<List<Integer>> expected = new ArrayList<>();
        expected.add(toList(1));
        expected.add(toList(2));
        expected.add(toList(3));
        expected.add(toList(2, 3));
        expected.add(toList(1, 2));
        expected.add(toList(1, 3));
        expected.add(toList(1, 2, 3));

        List<List<Integer>> actual = analysis.getAllSubsets(1, 2, 3).collect(Collectors.toList());

        assertEquals(expected, actual);
    }

    List<Integer> toList(Integer... entry) {
        return Arrays.stream(entry).collect(Collectors.toList());
    }
}
