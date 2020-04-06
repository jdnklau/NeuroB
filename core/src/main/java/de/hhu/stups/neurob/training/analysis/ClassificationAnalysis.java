package de.hhu.stups.neurob.training.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
public class ClassificationAnalysis<C> implements AnalysisData<C, ClassificationAnalysis<C>> {
    /** Classes to be used. Implies an ordering. */
    private Map<Set<C>, Long> classCounters;

    private static final Logger log = LoggerFactory.getLogger(ClassificationAnalysis.class);

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

        Long count = classCounters.keySet()
                .stream().filter(k -> k.containsAll(key))
                .map(classCounters::get)
                .filter(Objects::nonNull)
                .reduce(0L, (a, b) -> a + b);
        return count;
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
     * Counts a sample for the given class.
     * <p>
     * Returns itself for method chaining.
     *
     * @param sampleClass
     *
     * @return Reference to this analysis.
     */
    public ClassificationAnalysis<C> add(C sampleClass) {
        HashSet<C> set = new HashSet<C>();
        set.add(sampleClass);
        increaseCount(set);
        return this;
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
        increaseCount(getKey(sampleClass));
        return this;
    }

    /**
     * Returns the multi-labels seen so far.
     * <p>
     * Each inner set consists of labels for the combination of which
     * at least one sample exists.
     *
     * @return
     */
    public Set<Set<C>> getSeenMultilabels() {
        return classCounters.keySet();
    }

    /**
     * Returns as set containing the distinct classes seen during analysis.
     *
     * @return
     */
    public Set<C> getSeenClasses() {
        return getSeenMultilabels().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    void increaseCount(Set<C> sampleClass) {
        Long counter = getCount(sampleClass);
        classCounters.put(sampleClass, counter + 1);
    }

    /**
     * Merges the data of the other analysis into this.
     * <p>
     * Returns reference to itself for method chaining.
     *
     * @param other Analysis data to merge into this.
     *
     * @return
     */
    public synchronized ClassificationAnalysis<C> mergeWith(ClassificationAnalysis<C> other) {
        Set<Set<C>> classes = other.classCounters.keySet();

        for (Set<C> cls : classes) {
            Long count = this.getCount(cls);
            Long otherCount = other.getCount(cls);
            classCounters.put(cls, count + otherCount);
        }

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ClassificationAnalysis) {
            ClassificationAnalysis other = (ClassificationAnalysis) o;
            return this.classCounters.equals(other.classCounters);
        }
        return false;
    }
}
