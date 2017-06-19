package neurob.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.eval.IEvaluation;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.FileStatsStorage;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.nets.NeuroBNet;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingSetAnalyser;
import neurob.training.TrainingSetGenerator;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.statistics.ClassificationModelEvaluation;
import neurob.training.statistics.RegressionModelEvaluation;
import neurob.training.statistics.interfaces.ModelEvaluation;

/**
 * <p>Main class of NeuroB to use</p>
 * <p>Internally this class wraps a neural network of user's choice and provides additional functionality
 * like training, trainingset generation, and more.
 * </p>
 * TODO: Add example use
 * @author Jannik Dunkelau
 */
public class NeuroB {
	private static final Logger log = LoggerFactory.getLogger(NeuroB.class);

	private NeuroBNet nbn;

	private boolean dl4jUIEnabled;

	/**
	 * Path to which the NeuroB data will be saved
	 */
	private final Path savePath;

	public NeuroB(NeuroBNet neuroBNet) {
		// set up with default target directory
		this(neuroBNet,
				Paths.get("trained_models/")
				.resolve(neuroBNet.getDataPathName())
				.resolve(ZonedDateTime.now()
						.format(DateTimeFormatter.ISO_INSTANT)));
	}

	public NeuroB(NeuroBNet neuroBNet, Path modelDirectory){
		// link neural net
		nbn = neuroBNet;
		dl4jUIEnabled = false;

		savePath = modelDirectory;
	}

	public NeuroBNet getNeuroBNet(){
		return nbn;
	}

	/**
	 * Trains the neural net with a training set located at the given source.
	 * <p>
	 * The test source is then used to evaluate the trained network.
	 * If for 5 consecutive epochs no performance gain on the test set could be measured,
	 * the training stops.
	 * @param trainSource
	 * @param testSource
	 * @param numEpochs Number of epochs used in training
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public ModelEvaluation train(Path trainSource,Path testSource,
			int numEpochs) throws IOException, InterruptedException{
		return train(trainSource, testSource, numEpochs, false);
	}

	/**
	 * Trains the neural net with a training set located at the given source.
	 * <p>
	 * The test source is then used to evaluate the trained network.
	 * If for 5 consecutive epochs no performance gain on the test set could be measured,
	 * the training stops.
	 * @param trainSource
	 * @param testSource
	 * @param numEpochs Number of epochs used in training
	 * @param saveEpochStats Whether or not to save the evaluation results generated after each epoch to a csv file
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public ModelEvaluation train(Path trainSource,Path testSource,
			int numEpochs, boolean saveEpochStats)
					throws IOException, InterruptedException{
		return train(trainSource, testSource, numEpochs, saveEpochStats, 5);
	}

	/**
	 * Trains the neural net with a training set located at the given source.
	 * <p>
	 * The test source is then used to evaluate the trained network.
	 * <p>
	 * If for {@code earlyStoppingEpochs} consecutive epochs no performance gain on the test set could be measured,
	 * the training stops.
	 * @param trainSource
	 * @param testSource
	 * @param numEpochs Number of epochs used in training
	 * @param saveEpochStats Whether or not to save the evaluation results generated after each epoch to a csv file
	 * @param earlyStoppingEpochs Number of consecutive epochs without performance gain before training is interrupted
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public ModelEvaluation train(Path trainSource,Path testSource,
			int numEpochs, boolean saveEpochStats, int earlyStoppingEpochs)
			throws IOException, InterruptedException{
		return train(trainSource, testSource, 250, numEpochs, saveEpochStats, earlyStoppingEpochs);
	}

	/**
	 * Trains the neural net with a training set located at the given source.
	 * <p>
	 * The test source is then used to evaluate the trained network.
	 * <p>
	 * If for {@code earlyStoppingEpochs} consecutive epochs no performance gain on the test set could be measured,
	 * the training stops.
	 * @param trainSource
	 * @param testSource
	 * @param batchSize
	 * @param numEpochs Number of epochs used in training
	 * @param saveEpochStats Whether or not to save the evaluation results generated after each epoch to a csv file
	 * @param earlyStoppingEpochs Number of consecutive epochs without performance gain before training is interrupted
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public ModelEvaluation train(Path trainSource,Path testSource, int batchSize,
			int numEpochs, boolean saveEpochStats, int earlyStoppingEpochs)
			throws IOException, InterruptedException{
		log.info("Setting up target directory {}", savePath);
		Files.createDirectories(savePath);
		log.trace("\tDone: Setting up target directory");

		// get evaluation unit
		ModelEvaluation eval = modelEvaluationByProblemType();
		if(saveEpochStats)
			eval.enableSavingToDisk(savePath.resolve("epochs.csv"));

		log.info("Beginning with training on {}: Using {} epochs and a batch size of {}", trainSource, numEpochs, batchSize);

		// set up training data iterator and listeners
		DataSetIterator iterator = setupTrainingSetIterator(trainSource, batchSize);
		setupModelTrainingListeners();

		// initial evaluation before any training happened
		eval.init(trainSource, testSource);

		// train net on training data
		int bestEpochSaved = 0;
		int trainedEpochs = 0; // count how many epochs were actually trained
		for(int i=1; i<=numEpochs; i++){
			log.info("Training epoch {}", i);
        	iterator.reset();
			nbn.fit(iterator);
			trainedEpochs++;

			// evaluate after each epoch
			try {
				eval.evaluateAfterEpoch(trainSource, testSource);
			} catch (NeuroBException e) {
				log.warn("Could not calculate training and testing errors after epoch.", e);
			}

			// save best model
			if(eval.getBestEpochSeen() > bestEpochSaved){
				log.info("Improved performance with latest epoch {} (former best epoch was {})",
						eval.getBestEpochSeen(), bestEpochSaved);
				log.info("\tSaving model to {}", savePath);
				nbn.saveModel(savePath);
				bestEpochSaved = eval.getBestEpochSeen();
			} else {
				log.info("Best epoch thus far: #{}", eval.getBestEpochSeen());
				// early stopping
				if(i-bestEpochSaved >= earlyStoppingEpochs){
					log.warn("No performance gain for {} consecutive epochs; stopping training.",
							earlyStoppingEpochs);
					break;
				}
			}

		}

		log.info("Done with training {} epochs", trainedEpochs);
		log.info("******************************");

		// evaluate whole model
		test(testSource);

		return eval;
	}

	/**
	 * Attaches listeners to the model for training observation
	 */
	private void setupModelTrainingListeners() {
		List<IterationListener> listeners = new ArrayList<>();
		if(dl4jUIEnabled){
			UIServer uiServer = UIServer.getInstance();

			StatsStorage statsStorage = new InMemoryStatsStorage();
			uiServer.attach(statsStorage);

			listeners.add(new StatsListener(statsStorage));

			log.info("DL4J UI is available at http://localhost:9000/");
		}
		// - save stats for later use
		StatsStorage statsStorage = new FileStatsStorage(savePath.resolve("training_stats.dl4j").toFile());
		listeners.add(new StatsListener(statsStorage));
		listeners.add(new PerformanceListener(75, true));
		nbn.setListeners(listeners);
	}

	/**
	 * Sets up the training set iterator and normalizer.
	 * @param trainingSource
	 * @param batchSize
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private DataSetIterator setupTrainingSetIterator(Path trainingSource, int batchSize) throws IOException, InterruptedException{
		// set up data set iterator
		DataSetIterator iterator = nbn.getDataSetIterator(trainingSource, batchSize);
		// set up normaliser
		log.info("Setting up normaliser...");
		nbn.fitNormalizer(iterator);
		log.trace("\tDone: setting up normaliser");

		return iterator;
	}

	@SuppressWarnings("rawtypes")
	private ModelEvaluation modelEvaluationByProblemType()
			throws IOException {
		switch(nbn.getProblemType()){
		default: // NOTE: Defaulting to classification
		case CLASSIFICATION:
			return new ClassificationModelEvaluation(nbn);
		case REGRESSION:
			return new RegressionModelEvaluation(nbn);
		}
	}

	@SuppressWarnings("rawtypes")
	public ModelEvaluation test(Path testSource) throws IOException, InterruptedException{
		log.info("Evaluating the trained model...");

		ModelEvaluation eval = modelEvaluationByProblemType();
		try {
			eval.evaluateAfterTraining(testSource);
		} catch (NeuroBException e) {
			log.error("Could not evaluate model on test set.", e);
		}

		log.info("******************************");

		return eval;
	}

	/**
	 * Evaluates the network upon the given test set
	 * @param testSource
	 * @return
	 * @throws NeuroBException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	protected IEvaluation evaluateModel(Path testSource) throws NeuroBException, IOException, InterruptedException{
		int batchSize = 100;
		DataSetIterator iterator = nbn.getDataSetIterator(testSource, batchSize);
		nbn.applyNormalizer(iterator); //Apply normalization to the test data. This is using statistics calculated from the *training* set
		// Evaluate on test set
		// TODO: decide between regression and classification
		IEvaluation eval = modelEvaluationByProblemType().evaluateAfterTraining(testSource);

		return eval;
	}

	/**
	 * Use this prior to training to enable the use of the Deeplearning4j training UI.
	 * This allows to monitor training stats more closely and in real time.
	 * @param enable Whether it should be enabled or not
	 */
	public void enableDL4JUI(boolean enable){
		dl4jUIEnabled = enable;
	}

	/**
	 * <p>Generates the training data by iterating over all *.mch files in the given source directory
	 * and generates corresponding *.nbtrain files in the target directory.
	 * </p>
	 * <p>The given source directory will be searched recursively with respect to sub-directories.
	 * </p>
	 * <p>In the target directory a subdirectory will be created, named after the neural net class used.
	 * Inside the subdirectory the hierarchy of the source will be mirrored, but with corresponding .nbtrain
	 * files instead of the machines. This way a direct mapping between the machines and their generated features is possible.
	 * <br>
	 * Also in this directory, a File <i>data.csv</i> will be created, in which all the genrated training data vectors are listed linewise,
	 * for simpler loading into the DataSet format of DeepLearning4J.
	 * </p>
	 * <p>The exclude file contains paths, relative to its directory, to files and directories to exclude in the training set
	 * generation step. If none shall be used, <code>null</code> can be used as value.
	 *
	 * @param sourceDirectory Directory from which the machine files are read
	 * @param targetDirectory Directory in which the *.nbtrain files will be put
	 * @param excludeFile {@code null} or path to excludes file
	 * @throws NeuroBException
	 *
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory, Path excludeFile) throws NeuroBException {
		// set up generator
		TrainingSetGenerator tsg = nbn.getTrainingSetGenerator();
		// set up training data directory
		Path fullTargetDirectory = targetDirectory.resolve(nbn.getDataPathName());

		// generate data
		tsg.generateTrainingSet(sourceDirectory, fullTargetDirectory, excludeFile);
		// enhance logs
		tsg.logStatistics();

		try {
			TrainingAnalysisData analysis = tsg.analyseTrainingSet(fullTargetDirectory);
			TrainingSetAnalyser.logTrainingAnalysis(analysis);
			TrainingSetAnalyser.writeTrainingAnalysis(analysis, fullTargetDirectory);
		} catch (NeuroBException e) {
			log.error("Could not access target directory {} for training data analysis: {}", targetDirectory, e.getMessage(), e);
		} catch (IOException e) {
			log.error("Could not write analysis results to disk.", e);
		}
	}

}
