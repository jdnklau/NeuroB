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
	/**
	 * Whether or not the evaluation results will be written to disk
	 */
	protected boolean savingToDisk;

	/**
	 * The best evaluation gotten so far. May stay {@code null} as long
	 * no evaluation has taken place --- call {@link #init(Path, Path)} to set
	 * initial value.
	 */
	protected T bestEvaluation;

	public ModelEvaluation(NeuroBNet model) {
		this.nbn = model;
		epochsSeen = 0;
		bestEpochSeen = 0;
		savingToDisk = false;
	}

	/**
	 * Initialises the evaluation, preferably before any epoch was trained.
	 * <p>
	 * Sets initial results for the randomly chosen parameters in the model
	 * over the data set given, but if already some evaluation results are
	 * stored, behaviour remains undefined.
	 * <p>
	 * If {@link #enableSavingToDisk(Path)} was called before,
	 * this method will write a line to the epochs CSV.
	 *
	 * @param trainSet Location of training set, to calculate training error
	 * @param testSet Location of test set, to calculate test error
	 * @return The resulting evaluation
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public T init(Path trainSet, Path testSet) throws IOException, InterruptedException{
		if(bestEvaluation == null){
			T trainEval = evaluateModel(trainSet, "Initialisation, Training");
			bestEvaluation = evaluateModel(testSet, "Initialisation, Testing");
			if(savingToDisk)
				writeEvaluationToCSV(trainEval, bestEvaluation);
		}
		return bestEvaluation;
	}

	/**
	 * If not already done so, sets this evaluation to a mode in which
	 * the statistics generated per epoch are saved line wise to the specified
	 * CSV file.
	 * <p>
	 * The file will be created or overridden, if it already exists.
	 * @param csv CSV the data will be written to
	 */
	public final void enableSavingToDisk(Path csv) throws IOException {
		savingToDisk = true;
		setupCSV(csv);
	}

	/**
	 * @return True iff {@link #enableSavingToDisk(Path)} was called at least once.
	 */
	public final boolean isSavingToDiskEnabled(){ return savingToDisk;}

	/**
	 * Sets up the statistics file as CSV with header line.
	 * @param csv CSV to be generated
	 * @throws IOException
	 */
	protected abstract void setupCSV(Path csv) throws IOException;

	public final int getEpochsSeen(){return epochsSeen;}
	public final int getBestEpochSeen(){return bestEpochSeen;}

	/**
	 * Increases internal epoch counter by 1, evaluates the model on the training and test set
	 * and checks whether the new epoch reached the best performance on the test set or not.
	 * <p>
	 * Saves the generated results as a line to the csv file, if {@link #enableSavingToDisk(Path)}
	 * was called prior.
	 * @param trainingSet Location of training set, to calculate training error
	 * @param testSet Location of test set, to calculate test error
	 * @return Evaluation object for test set
	 * @throws NeuroBException
	 */
	public final T evaluateAfterEpoch(Path trainingSet, Path testSet) throws NeuroBException{
		epochsSeen++; // Looking at a new epoch

		// evaluate on train and test set
		T trainEval;
		T testEval;
		try {
			trainEval = evaluateModel(trainingSet, "Training");
			testEval = evaluateModel(testSet, "Testing");
		} catch (IOException | InterruptedException e) {
			throw new NeuroBException("Could not evaluate training or test set.", e);
		}

		// check for best epoch
		compareWithBestEpoch(testEval);

		// save to disk if demanded
		if(savingToDisk){
			try {
				writeEvaluationToCSV(trainEval, testEval);
			} catch (IOException e) {
				throw new NeuroBException("Could not write evaluation results to CSV.", e);
			}
		}

		return testEval;
	}

	/**
	 * If {@link #enableSavingToDisk(Path)} was called prior, saves the
	 * evaluation results on training and/or testing set to the CSV file as a
	 * single line, followed by a new line character.
	 * @param trainEval
	 * @param testEval
	 */
	protected abstract void writeEvaluationToCSV(T trainEval, T testEval)
			throws IOException;

	/**
	 * Compares the evaluation statistics gathered with the ones of the best
	 * performing epoch thus far and sets the new best epoch accordingly.
	 * @param testEval
	 */
	protected abstract void compareWithBestEpoch(T testEval);

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
	 * Evaluates the model on the given test set.
	 * Also provides logging information, which is gathered under the given caption.
	 * The caption shall enable to distinguish multiple evaluations on different test sets.
	 * @param testSet
	 * @param caption
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public abstract T evaluateModel(Path testSet, String caption) throws IOException, InterruptedException;

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
		int batchSize = 32;
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

	/**
	 * Returns true if this evaluation stored better results than the
	 * compared instance.
	 * @param compareWith Evaluation to compare performance results with
	 * @return true if this evaluation's results are better
	 */
	public boolean performsBetterThan(ModelEvaluation<T> compareWith){
		if(bestEvaluation != null){
			if(compareWith.getBestEvaluation() == null)
				return true;
			// both evaluations gathered are not null
			return performsBetterThan(bestEvaluation,
					compareWith.getBestEvaluation());
		}

		return false;
	}

	/**
	 * Returns true if this evaluation stored better results than the compared
	 * instance.
	 * @param compareWith Evaluation to compare performance results with
	 * @return true if this evaluation's results are better
	 */
	protected boolean performsBetterThan(T compareWith){
		return bestEvaluation != null
				&& performsBetterThan(bestEvaluation, compareWith);
	}

	/**
	 * Returns true if the performance measured in the first evaluation is
	 * better than the one in the second.
	 * @param first
	 * @param second
	 * @return True if first is better than second
	 */
	protected abstract boolean performsBetterThan(T first, T second);

	public T getBestEvaluation(){ return bestEvaluation;}
}
