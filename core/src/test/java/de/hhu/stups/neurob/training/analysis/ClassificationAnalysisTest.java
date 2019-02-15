package de.hhu.stups.neurob.training.analysis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationAnalysisTest {

    @Test
    void shouldYieldSingleElementSetForSingleEntry() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

        Set<Integer> expected = new HashSet<>();
        expected.add(2);
        Set<Integer> actual = analysis.getKey(2);

        assertEquals(expected, actual);
    }

    @Test
    void shouldHaveNoEntries() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

        Long expected = 0L;
        Long actual = analysis.getCount(1);
        assertEquals(expected, actual);
    }

    @Test
    void shouldHaveNoEntriesWhenOtherClassWasAddedTo() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

        analysis.add(1).add(1);

        Long expected = 0L;
        Long actual = analysis.getCount(2);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseClassCounter() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

        analysis.add(1).add(1);

        Long expected = 2L;
        Long actual = analysis.getCount(1);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseCombinedClassCountForMultilabelClassification() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

        analysis.add(1, 2).add(1, 2);

        Long expected = 2L;
        Long actual = analysis.getCount(1, 2);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseCombinedClassCountForAffectedClassesOnly() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

        analysis.add(1, 2).add(1, 2).add(1, 3).add(2, 3);

        Long expected = 1L;
        Long actual = analysis.getCount(1, 3);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseIndividualClassCountForMultiLabelClassification() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

        analysis.add(1, 2).add(1, 2).add(1, 3);

        Long expected = 3L;
        Long actual = analysis.getCount(1);
        assertEquals(expected, actual);
    }

    @Test
    void shouldIncreaseClassCountForEachSubsetInMultiLabelClassificationWhenSingleAdditon() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

        analysis.add(1, 2, 3);

        assertAll("Subset sizes should match",
                () -> assertEquals(new Long(1L), analysis.getCount(1),
                        "Class 1 does not match."),
                () -> assertEquals(new Long(1L), analysis.getCount(2),
                        "Class 2 does not match."),
                () -> assertEquals(new Long(1L), analysis.getCount(3),
                        "Class 3 does not match."),
                () -> assertEquals(new Long(1L), analysis.getCount(1, 2),
                        "Class 1+2 does not match."),
                () -> assertEquals(new Long(1L), analysis.getCount(1, 3),
                        "Class 1+3 does not match."),
                () -> assertEquals(new Long(1L), analysis.getCount(2, 3),
                        "Class 2+3 does not match."),
                () -> assertEquals(new Long(1L), analysis.getCount(1, 2, 3),
                        "Class 1+2+3 does not match.")
        );
    }

    @Test
    void shouldIncreaseClassCountForEachSubsetInMultiLabelClassificationWhenMultipleAdditions() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();

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
    void shouldMergeTwoAnalyses() {
        ClassificationAnalysis<Integer> analysis = new ClassificationAnalysis<>();
        analysis.add(1).add(1).add(2).add(2);

        ClassificationAnalysis<Integer> other = new ClassificationAnalysis<>();
        other.add(2).add(3);

        analysis.mergeWith(other);

        assertAll(
                () -> assertEquals(new Long(2), analysis.getCount(1),
                        "Amount of 1 does not match"),
                () -> assertEquals(new Long(3), analysis.getCount(2),
                        "Amount of 2 does not match"),
                () -> assertEquals(new Long(1), analysis.getCount(3),
                        "Amount of 3 does not match")
        );
    }

    @Test
    void shouldResultInEqualAnalysisWhenMergedAndWhenRunInOneStep() {

        ClassificationAnalysis<Integer> oneStep = new ClassificationAnalysis<>();
        oneStep.add(1).add(1).add(2).add(2);
        oneStep.add(2).add(3);

        ClassificationAnalysis<Integer> merged = new ClassificationAnalysis<>();
        merged.add(1).add(1).add(2).add(2);
        ClassificationAnalysis<Integer> other = new ClassificationAnalysis<>();
        other.add(2).add(3);

        merged.mergeWith(other);

        assertEquals(oneStep, merged);

    }

    @Test
    void shouldNotBeEqual() {
        ClassificationAnalysis<Integer> analysis1 = new ClassificationAnalysis<>();
        analysis1.add(1);
        ClassificationAnalysis<String> analysis2 = new ClassificationAnalysis<>();
        analysis2.add("1");

        assertNotEquals(analysis1, analysis2);
    }

    @Test
    void shouldBeEqual() {
        ClassificationAnalysis<Integer> analysis1 = new ClassificationAnalysis<>();
        analysis1.add(1);
        ClassificationAnalysis<Integer> analysis2 = new ClassificationAnalysis<>();
        analysis2.add(1);

        assertEquals(analysis1, analysis2);
    }

    Set<Integer> toSet(Integer... entry) {
        return Arrays.stream(entry).collect(Collectors.toSet());
    }
}
