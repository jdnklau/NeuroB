package de.hhu.stups.neurob.training.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final C[] classes;
    private Map<List<C>, Long> classCounters;


    public ClassificationAnalysis(C... classes) {
        this.classes = classes;
        this.classCounters = new HashMap<>();
    }

    /**
     * Returns the classes in an ordered list.
     * The ordering accounts for the order used by
     * {@link #ClassificationAnalysis(Object[]) initialisation}.
     *
     * @param cs
     *
     * @return
     */
    List<C> getKey(C... cs) {
        return getKey(Arrays.stream(cs).collect(Collectors.toList()));
    }

    /**
     * Returns the classes in an ordered list.
     * The ordering accounts for the order used by
     * {@link #ClassificationAnalysis(Object[]) initialisation}.
     *
     * @param cs
     *
     * @return
     */
    List<C> getKey(List<C> cs) {
        // TODO: Should the result be memoized?
        List<C> key = new ArrayList<>();
        Arrays.stream(classes)
                .filter(c -> cs.stream().anyMatch(c::equals))
                .forEach(key::add);
        return key;
    }

    /**
     * Returns the sample count for the given class.
     *
     * @param clss
     *
     * @return
     */
    public Long getCount(C... clss) {
        List<C> key = getKey(clss);
        return classCounters.getOrDefault(key, 0L);
    }

    /**
     * Returns the sample count for the given class.
     *
     * @param clss
     *
     * @return
     */
    public Long getCount(List<C> clss) {
        List<C> key = getKey(clss);
        return classCounters.getOrDefault(key, 0L);
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

    void increaseCount(List<C> sampleClass) {
        List<C> key = getKey(sampleClass);
        Long counter = getCount(sampleClass);
        classCounters.put(key, counter + 1);
    }

    Stream<List<C>> getAllSubsets(C... cs) {
        return getAllSubsets(Arrays.stream(cs).collect(Collectors.toList()));
    }

    Stream<List<C>> getAllSubsets(List<C> cs) {
        int length = cs.size();

        if (length <= 1) {
            return Stream.of(cs);
        }

        C head = cs.get(0);
        List<C> tail1 = new ArrayList<>(cs.subList(1,length));
        List<C> tail2 = new ArrayList<>(tail1); // Need two due to tailWithHead modifying tail1

        Stream<List<C>> headOnly = getAllSubsets(head);
        Stream<List<C>> tailWithHead = getAllSubsets(tail1).map(l -> {l.add(0, head); return l;});
        Stream<List<C>> tailWithoutHead = getAllSubsets(tail2);

        return Stream.of(headOnly, tailWithoutHead, tailWithHead)
                .flatMap(s -> s);

    }

}
