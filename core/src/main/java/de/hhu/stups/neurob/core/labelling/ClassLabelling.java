package de.hhu.stups.neurob.core.labelling;

/**
 * Labelling for classification tasks.
 */
public abstract class ClassLabelling extends Labelling {

    /**
     * @return Number of distinct classes.
     */
    abstract int getClassCount();

}
