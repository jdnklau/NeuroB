package neurob.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.eval.ConfusionMatrix;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.FileStatsStorage;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.nets.NeuroBNet;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingSetAnalyser;
import neurob.training.TrainingSetGenerator;

/**
 * <p>Main class of NeuroB to use</p>
 * <p>Internally this class wraps a neural network of user's choice and provides additional functionality
 * like training, trainingset generation, and more.
 * </p>
 * <p>On creating a new instance, the constructor expects an object implementing the 
 * {@link neurob.core.nets.interfaces.NeuroBNet NeuroBNet} interface. Every utility this interface provides should be accessible by this class too.
 * <br>
 * See the  following usage example to set up a new neural net with NeuroB:
 * <pre>
 * {@code 
 * // Setting up the neural net
 * NeuroBNet nbn = new DefaultPredicateSolverPredictionNet()
 *                 .setSeed(12345L) // Choose as seed (optional)
 *                 .build(); // Build the net internally (does not happen automatically
 *                 
 * // Wrap the net with NeuroB
 * NeuroB n = new NeuroB(nbn); // Done! 
 * }
 * </pre></p>
 * <p> Next step would be to train the neural net:
 * <pre>
 * {@code
 * // Generate the training data
 * // (optional; if the training data already exists this step can be skipped)
 * Path sourceDirectory = Paths.get("path/to/source/data/");
 * Path targetDirectory = Paths.get("path/to/target/directory/");
 * n.generateTrainingSet(sourceDirectory, targetDirectory);
 * // (This will take a while)
 * 
 * // After the generation, a data csv file should be created; use it to train your data
 * Path trainingCSV = Paths.get("path/to/target/data.csv");
 * n.train(trainingCSV); // Done! (Also could take a while)
 * }
 * </pre>
 * See {@link #generateTrainingSet(Path, Path) generateTrainingSet} for more information on the generation step.</p>
 * 
 * @author Jannik Dunkelau
 * @see neurob.core.nets.interfaces
 * @see #generateTrainingSet(Path, Path)
 *
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
	 * @param trainSource
	 * @param testSource
	 * @param numEpochs Number of epochs used in training
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void train(Path trainSource, Path testSource, int numEpochs) throws IOException, InterruptedException{
		train(trainSource, testSource, numEpochs, false);
	}
		
	/**
	 * Trains the neural net with a training set located at the given source.
	 * <p>
	 * The test source is then used to evaluate the trained network.
	 * @param trainSource
	 * @param testSource
	 * @param numEpochs Number of epochs used in training
	 * @param saveEpochStats Whether or not to save the evaluation results generated after each epoch to a csv file
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void train(Path trainSource, Path testSource, int numEpochs, boolean saveEpochStats) throws IOException, InterruptedException{
		log.info("Setting up target directory {}", savePath);
		Files.createDirectories(savePath);
		
		BufferedWriter epochCSV = null;
		if(saveEpochStats){
			// set up csv
			epochCSV = Files.newBufferedWriter(savePath.resolve("epochs.csv"));
			
			// set up header
			List<String> columns = new ArrayList<>();
			columns.add("Epoch");
			// training set stats
			columns.add("Training Accuracy");
			columns.add("Training Precision");
			columns.add("Training Recall");
			columns.add("Training F1 Score");
			// test set stats
			columns.add("Test Accuracy");
			columns.add("Test Precision");
			columns.add("Test Recall");
			columns.add("Test F1 Score");
			
			epochCSV.write(String.join(",", columns));
			epochCSV.newLine();
			epochCSV.flush();
		}
		
		int batchSize = 250;
		log.info("Beginning with training on {}: Using {} epochs and a batch size of {}", trainSource, numEpochs, batchSize);
		
		// set up training data
		DataSetIterator iterator = nbn.getDataSetIterator(trainSource, batchSize);
		
		// set up normaliser
		log.info("Setting up normaliser...");
		nbn.fitNormalizer(iterator);
		
		// Set up listeners
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
		
		// train net on training data
		for(int i=0; i<numEpochs; i++){
			log.info("Training epoch {}", i+1);
        	iterator.reset();
			nbn.fit(iterator);
			
			// evaluate after each epoch
			Evaluation trainEval = evaluateModel(trainSource);
			Evaluation testEval = evaluateModel(testSource);
			log.info("Done with epoch {}", i+1);
			log.info("\tTraining stats - Accuracy: {}; Precision: {}; Recall: {}; F1 score: {}",
					trainEval.accuracy(),
					trainEval.precision(),
					trainEval.recall(),
					trainEval.f1());
			log.info("\tTesting stats - Accuracy: {}; Precision: {}; Recall: {}; F1 score: {}",
					testEval.accuracy(),
					testEval.precision(),
					testEval.recall(),
					testEval.f1());
			
			// save to csv
			if(saveEpochStats && epochCSV != null){
				List<String> columns = new ArrayList<>();
				columns.add(Integer.toString(i+1));
				// training set stats
				columns.add(Double.toString(trainEval.accuracy()));
				columns.add(Double.toString(trainEval.precision()));
				columns.add(Double.toString(trainEval.recall()));
				columns.add(Double.toString(trainEval.f1()));
				// test set stats
				columns.add(Double.toString(testEval.accuracy()));
				columns.add(Double.toString(testEval.precision()));
				columns.add(Double.toString(testEval.recall()));
				columns.add(Double.toString(testEval.f1()));
				
				try {
					epochCSV.write(String.join(",", columns));
					epochCSV.newLine();
					epochCSV.flush();
				} catch (Exception e) {
					log.warn("\tCould not add evaluation results of current epoch to csv");
				}
			}
		}
		
		if(epochCSV != null)
			epochCSV.close();
		
		log.info("Done with training {} epochs", numEpochs);
		log.info("******************************");
		
		log.info("Saving model to {}", savePath);
		nbn.saveModel(savePath);

		// evaluate whole model
		test(testSource);
	}
	
	public void test(Path testSource) throws IOException, InterruptedException{
		log.info("Evaluating the trained model...");
		
		Evaluation eval = evaluateModel(testSource);
		
		// log evaluation results
		log.info("\tAccuracy: {}", eval.accuracy());
		log.info("\tPrecision: {}", eval.precision());
		log.info("\tRecall: {}", eval.recall());
		log.info("\tF1 score: {}", eval.f1());

		// log confusion matrix
		ConfusionMatrix<Integer> matrix = eval.getConfusionMatrix();
		log.info("Confusion Matrix:");
		for(int i=0; i<nbn.getClassificationSize(); i++){
			for(int j=0; j<nbn.getClassificationSize(); j++){
				log.info("\tClass {} predicted as {} a total of {} times", i, j,
						matrix.getCount(i, j));
			}
		}
		
		log.info("******************************");
	}
	
	/**
	 * Evaluates the network upon the given test set
	 * @param testSource
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected Evaluation evaluateModel(Path testSource) throws IOException, InterruptedException{
		int batchSize = 100;
		DataSetIterator iterator = nbn.getDataSetIterator(testSource, batchSize);
		nbn.applyNormalizer(iterator); //Apply normalization to the test data. This is using statistics calculated from the *training* set
		// Evaluate on test set
		// TODO: decide between regression and classification
		Evaluation eval = new Evaluation(nbn.getClassificationSize());
		
		while(iterator.hasNext()){
			DataSet testData = iterator.next();
        	INDArray output = nbn.output(testData.getFeatureMatrix());        	
        	eval.eval(testData.getLabels(), output);
		}
		
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
			TrainingSetAnalyser.logTrainingAnalysis(tsg.analyseTrainingSet(fullTargetDirectory));
		} catch (NeuroBException e) {
			log.error("Could not access target directory {} for training data analysis: {}", targetDirectory, e.getMessage(), e);
		}
	}

}
