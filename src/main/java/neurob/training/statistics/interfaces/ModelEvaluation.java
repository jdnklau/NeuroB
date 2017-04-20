package neurob.training.statistics.interfaces;

import java.io.IOException;
import java.nio.file.Path;

import org.deeplearning4j.eval.IEvaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import neurob.core.nets.NeuroBNet;
import neurob.exceptions.NeuroBException;

@SuppressWarnings("rawtypes")
public abstract class ModelEvaluation<T extends IEvaluation> {
	protected final NeuroBNet nbn;
	
	/**
	 * Number of epochs that were evaluated.
	 */
	protected int epochsSeen;
	
	/**
	 * The best evaluated epoch seen thus far.
	 */
	protected int bestEpochSeen;
	
	public ModelEvaluation(NeuroBNet model, Path csvFile) throws IOException {
		this.nbn = model;
		epochsSeen = 0;
		bestEpochSeen = -1;
		setup(csvFile);
	}
	
	/**
	 * Sets up the statistics file as CSV with header line. 
	 * @param csv CSV to be generated
	 * @throws IOException
	 */
	protected abstract void setup(Path csv) throws IOException;

	public int getEpochsSeen(){return epochsSeen;}
	public int getBestEpochSeen(){return bestEpochSeen;}
	
	/**
	 * Increases internal epoch counter by 1, evaluates the model on the training and test set
	 * and checks whether the new epoch reached the best performance on the test set or not.
	 * <p>
	 * Saves the generated results as a line to the csv file.
	 * @param trainingSet Location of training set, to calculate training error
	 * @param testSet Location of test set, to calculate test error
	 * @throws NeuroBException
	 */
	public abstract void evaluateAfterEpoch(Path trainingSet, Path testSet) throws NeuroBException;
	
	/**
	 * Like {@link #evaluateModel(Path)}, but also adds detailed information to log.
	 * @param testSet
	 * @return Eval object that evaluated the given model
	 * @throws NeuroBException
	 */
	public abstract T evaluateAfterTraining(Path testSet) throws NeuroBException;
	
	/**
	 * Evaluates the model on the given test set.
	 * @param testSet
	 * @return Eval object, that evaluated the given model
	 */
	public abstract T evaluateModel(Path testSet) throws IOException, InterruptedException;
	
	/**
	 * Evaluates the model in the given test set and feeds the data into the {@link IEvaluation eval}
	 * object.
	 * @param testSet
	 * @param eval
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected T evaluateModel(Path testSet, T eval) throws IOException, InterruptedException{
		int batchSize = 100;
		DataSetIterator iterator = nbn.getDataSetIterator(testSet, batchSize);
		nbn.applyNormalizer(iterator); //Apply normalization learned from training set

		// evaluate test set
		while(iterator.hasNext()){
			DataSet testData = iterator.next();
        	INDArray output = nbn.output(testData.getFeatureMatrix());        	
        	eval.eval(testData.getLabels(), output);
		}
		
		return eval;
	}
}
