package de.hhu.stups.neurob.core.api;

/**
 * Labelling for classification tasks.
 */
public interface ClassLabelling extends Labelling{

    /**
     * @return Number of distinct classes.
     */
    int getClassCount();

}
