package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.api.Features;
import de.hhu.stups.neurob.core.api.Labelling;

import java.util.List;

public interface TrainingDataIterator<F extends Features, L extends Labelling> {

    /**
     * Returns a batch of given size.
     * <p>
     * If {@link #hasNext()} returns <code>true</code>,
     * the batch contains at least one element, and <code>batchSize</code>
     * elements at most.
     * Else if {@link #hasNext()} returns <code>false</code>,
     * the batch is empty.
     * </p>
     *
     * @param batchSize Number of samples contained in the returned batch
     * @return
     *
     * @see #hasNext()
     * @see #reset()
     */
    List<TrainingSample<F, L>> getNextBatch(int batchSize);

    /**
     * @return <code>true</code> iff there are samples left that were not yet
     *         returned.
     */
    Boolean hasNext();

    /**
     * Resets this iterator.
     * <p>
     * Calling {@link #getNextBatch(int)} will now iterate through all the
     * training data again.
     * Depending on implementation, the batch order might vary each reset.
     * </p>
     */
    void reset();

}
