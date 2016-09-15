package neurob.core.nets.interfaces;

import org.nd4j.linalg.dataset.DataSet;

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
	 * Fit the training data into the model
	 * @param trainingData
	 */
	void fit(DataSet trainingData);
}
