package neurob.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Random;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import neurob.core.nets.PredicateSolverPredictionNet;
import neurob.core.nets.interfaces.NeuroBNet;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.interfaces.TrainingDataCollector;

/**
 * <p>Main class of NeuroB to use</p>
 * <p>Internally this class wraps a neural network of user's choice and provides additional functionallity
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
	 * @param sourceCSV
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void train(Path sourceCSV) throws IOException, InterruptedException{
		// set up training data
		RecordReader recordReader = nbn.getRecordReader(sourceCSV);
		DataSetIterator iterator = nbn.getDataSetIterator(recordReader);
		
		// get data set
        //iterator.forEachRemaining(batch -> {
		Evaluation eval = new Evaluation(nbn.getLabelSize());
		for(int i=0; i<numEpochs; i++){
			System.out.println("epoch "+i);
        	iterator.reset();
			while(iterator.hasNext()){
				DataSet batch = iterator.next();
	        	// split set
	        	batch.shuffle(seed);
	        	SplitTestAndTrain testAndTrain = batch.splitTestAndTrain(0.65);  //Use 65% of data for training
	        	DataSet trainingData = testAndTrain.getTrain();
	        	DataSet testData = testAndTrain.getTest();
	        	
	        	// normalize data
//	        	DataNormalization normalizer = new NormalizerStandardize();
//	            normalizer.fit(trainingData);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
//	            normalizer.transform(trainingData);     //Apply normalization to the training data
//	            normalizer.transform(testData);         //Apply normalization to the test data. This is using statistics calculated from the *training* set
	            
	        	nbn.fit(trainingData);
	            
	            // Evaluate results
	            Iterator<DataSet> it = testData.iterator();
	            
	            while(it.hasNext()){
	            	DataSet next = it.next();
	            	INDArray output = nbn.output(next.getFeatureMatrix());
	            	
	            	eval.eval(next.getLabels(), output);
	            }
			}
		}
		System.out.println(eval.stats());
	}
	
	/**
	 * <p>Safes the neural net used into a file</p>
	 * <p>The file's location will be <i>nnets/<name of NeuroBNet class used>/{@code name}</i>. 
	 * The convention is to use <i>.nnet</i> as file extension.</p>
	 * @param name
	 * @throws IOException 
	 */
	public void safeToFile(String name) throws IOException{
		Path nnetfile = Paths.get("nnets/"+nbn.getClass().getSimpleName()+"/"+name);
		nbn.safeToFile(nnetfile);
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
		TrainingDataCollector tdc = nbn.getTrainingDataCollector();
		TrainingSetGenerator tsg = new TrainingSetGenerator(tdc);
		// set up training data directory
		Path fullTargetDirectory = targetDirectory.resolve(nbn.getClass().getSimpleName());
		
		// generate data
		tsg.generateTrainingSet(sourceDirectory, fullTargetDirectory, excludeFile);
		// enhance logs
		tsg.logStatistics();
		tsg.logTrainingSetAnalysis(fullTargetDirectory);
		
		// generate csv
		tsg.generateCSVFromNBTrainData(fullTargetDirectory, fullTargetDirectory.resolve("data.csv"));
	}

}
