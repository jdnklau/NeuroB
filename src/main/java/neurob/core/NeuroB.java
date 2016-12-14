package neurob.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Random;

import org.datavec.api.records.reader.RecordReader;
import org.deeplearning4j.eval.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.nets.NeuroBNet;
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
	// RNG
	private long seed = 12345;
	private Random rnd = new Random(seed);
	// Training
	private int numEpochs = 15;

	public NeuroB(NeuroBNet neuroBNet) {
		// link neural net
		nbn = neuroBNet;
		
	}
	
	public void setEpochs(int epochs){
		numEpochs = epochs;
	}
	
	/**
	 * Trains the neural net with a CSV file.
	 * <p>
	 * The test CSV is then used to evaluate the trained network.
	 * @param trainCSV
	 * @param testCSV
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void train(Path trainCSV, Path testCSV) throws IOException, InterruptedException{
		train(trainCSV);
		test(testCSV);
	}
	
	/**
	 * Trains the neural net with a CSV file.
	 * @param trainCSV
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void train(Path trainCSV) throws IOException, InterruptedException{
		int batchSize = 1000;
		log.info("Beginning with training on {}: Using {} epochs and a batch size of {}", trainCSV, numEpochs, batchSize);
		
		// set up training data
		DataSetIterator iterator = nbn.getDataSetIterator(trainCSV, batchSize);
		
		// set up normalizer
		while(iterator.hasNext()){
			DataSet batch = iterator.next();
        	nbn.fitNormalizer(batch);
		}
		
		// train net on training data
		for(int i=0; i<numEpochs; i++){
			log.info("Training epoch {}", i+1);
        	iterator.reset();
			while(iterator.hasNext()){
				DataSet trainingData = iterator.next(); 
	        	nbn.fit(trainingData);
			}
		}
		
		log.info("Done with training {} epochs", numEpochs);
		log.info("******************************");
	}
	
	public void test(Path testCSV) throws IOException, InterruptedException{
		log.info("Evaluating the training results");
		
		int batchSize = 1000;
		DataSetIterator iterator = nbn.getDataSetIterator(testCSV, batchSize);
		
		// Evaluate on test set
		Evaluation eval = new Evaluation(nbn.getOutputSize());
		iterator.reset();
		while(iterator.hasNext()){
			DataSet testData = iterator.next();
            
            nbn.getNormalizer().transform(testData);         //Apply normalization to the test data. This is using statistics calculated from the *training* set        	
        	
            // Evaluate results
            Iterator<DataSet> it = testData.iterator();
            
            while(it.hasNext()){
            	DataSet next = it.next();
            	INDArray output = nbn.output(next.getFeatureMatrix());
            	
            	eval.eval(next.getLabels(), output);
            }
		}
		
		// log evaluation results
		log.info("\tAccuracy: {}", eval.accuracy());
		log.info("\tPrecision: {}", eval.precision());
		log.info("\tRecall: {}", eval.recall());
		log.info("\tF1 score: {}", eval.f1());
		log.info("******************************");
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
	 * 
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory, Path excludeFile) {
		// set up generator
		TrainingSetGenerator tsg = nbn.getTrainingSetGenerator();
		// set up training data directory
		Path fullTargetDirectory = targetDirectory.resolve(nbn.getDataPathName());
		
		// generate data
		tsg.generateTrainingSet(sourceDirectory, fullTargetDirectory, excludeFile);
		// enhance logs
		tsg.logStatistics();
		tsg.logTrainingSetAnalysis(fullTargetDirectory);
		
		// generate csv
		tsg.generateTrainAndTestCSVfromNBTrainData(fullTargetDirectory, fullTargetDirectory.resolve("train_data.csv"), fullTargetDirectory.resolve("test_data.csv"), 0.65);
	}

}
