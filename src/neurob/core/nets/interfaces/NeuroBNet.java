package neurob.core.nets.interfaces;

import org.nd4j.linalg.dataset.DataSet;

import neurob.training.generators.interfaces.TrainingDataCollector;

/**
 * Wrapper interface for DeepLearning4J nets.
 * <p> Implement your very own neural net classes with this interface, to use with {@link neurob.NeuroB NeuroB}.
 * </p>
 * <p> <b>Usage:</b>
 * <br> TODO: write usage example and stuff.
 * </p>
 * 
 * @author jannik
 *
 */
public interface NeuroBNet {
	/**
	 * Returns the number of features the neural net handles as input vector
	 * @return
	 */
	int getNumberOfInputs();
	
	/**
	 * Returns the number of output values returned after predicting the labels of a feature vector.
	 * @return
	 */
	int getNumberOfOutputs();
	
	/**
	 * Returns an instance of the {@link neurob.training.generators.interfaces.TrainingDataCollector TrainingDataCollector}
	 * used by the net, for data generation.
	 * @return
	 */
	TrainingDataCollector getTrainingDataCollector();
	
	/**
	 * Set the seed to be used for the neural net
	 * @param seed
	 * @return The modified instance
	 */
	NeuroBNet setSeed(Long seed);
	
	/**
	 * Builds the model anew.
	 * @return
	 */
	NeuroBNet build();
	
	/**
	 * Fit the training data into the model
	 * @param trainingData
	 */
	void fit(DataSet trainingData);
}
