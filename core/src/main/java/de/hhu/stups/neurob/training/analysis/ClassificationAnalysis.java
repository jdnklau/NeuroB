package de.hhu.stups.neurob.training.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analysis for classification tasks.
 * <p>
 * Supports binary classification, multiclass classification,
 * and multi-label classification.
 * <p>
 * Keeps track of how many instances belong two which class.
 * <p>
 * In case of multi-label classification, the various subsets are further tracked as
 * individual classes.
 * For instance, if the sample {@code x} belongs to the classes {@code c1, c2},
 * it will be counted for the class c1 as in the multiclass classification,
 * and also for the pair (c1, c2), but not for the pair (c1, c3).
 *
 * @param <C> Type of Class labels.
 */
public class ClassificationAnalysis<C> {
    /** Classes to be used. Implies an ordering. */
    private Map<Set<C>, Long> classCounters;


    public ClassificationAnalysis() {
        this.classCounters = new HashMap<>();
    }

    /**
     * Returns the classes as set used for mapping the class counter.
     *
     * @param cs
     *
     * @return
     */
    Set<C> getKey(C... cs) {
        return Arrays.stream(cs).collect(Collectors.toSet());
    }

    /**
     * Returns the sample count for the given class.
     *
     * @param clss
     *
     * @return
     */
    public Long getCount(C... clss) {
        Set<C> key = getKey(clss);
        return getCount(key);
    }

    /**
     * Returns the sample count for the given class.
     *
     * @param clss
     *
     * @return
     */
    public Long getCount(Set<C> clss) {
        return classCounters.getOrDefault(clss, 0L);
    }

    /**
     * Counts a sample for the given class(es).
     * <p>
     * Returns itself for method chaining.
     *
     * @param sampleClass
     *
     * @return Reference to this analysis.
     */
    public ClassificationAnalysis<C> add(C... sampleClass) {
        getAllSubsets(sampleClass).forEach(this::increaseCount);
        return this;
    }

    void increaseCount(Set<C> sampleClass) {
        Long counter = getCount(sampleClass);
        classCounters.put(sampleClass, counter + 1);
    }

    Stream<Set<C>> getAllSubsets(C... cs) {
        return getAllSubsets(Arrays.stream(cs).collect(Collectors.toList()));
    }

    Stream<Set<C>> getAllSubsets(List<C> cs) {
        int length = cs.size();

        if (length <= 1) {
            return Stream.of(new HashSet<>(cs));
        }

        C head = cs.get(0);
        List<C> tail1 = new ArrayList<>(cs.subList(1, length));
        List<C> tail2 = new ArrayList<>(tail1); // Need two due to tailWithHead modifying tail1

        Stream<Set<C>> headOnly = getAllSubsets(head);
        Stream<Set<C>> tailWithHead = getAllSubsets(tail1).map(l -> {
            l.add(head);
            return l;
        });
        Stream<Set<C>> tailWithoutHead = getAllSubsets(tail2);

        return Stream.of(headOnly, tailWithoutHead, tailWithHead)
                .flatMap(s -> s);

    }

}
