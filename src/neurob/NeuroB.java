package neurob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import neurob.core.nets.interfaces.NeuroBNet;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.interfaces.TrainingDataCollector;

public class NeuroB {
	private NeuroBNet nbn;
	// RNG
	private long seed = 12345;
	private Random rnd = new Random(seed);

	public NeuroB(NeuroBNet neuroBNet) {
		// link neural net
		nbn = neuroBNet;
		
	}
	
	/**
	 * Trains the neural net with a CSV file.
	 * @param sourceCSV
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void train(Path sourceCSV) throws IOException, InterruptedException{
		// set up training data
		RecordReader recordReader = new CSVRecordReader(1,","); // skip first line (header line)s
		recordReader.initialize(new FileSplit(sourceCSV.toFile(), rnd));
		
		DataSetIterator iterator = new RecordReaderDataSetIterator(
				recordReader,
				1000,						// batch size
				nbn.getNumberOfInputs(),	// index of the label values in the csv		
				nbn.getNumberOfOutputs()	// number of outputs
			);
		// get data set
        iterator.forEachRemaining(batch -> {
        	// split set
        	batch.shuffle(seed);
        	SplitTestAndTrain testAndTrain = batch.splitTestAndTrain(0.65);  //Use 65% of data for training
        	DataSet trainingData = testAndTrain.getTrain();
        	DataSet testData = testAndTrain.getTest();
        	
        	// normalize data
        	DataNormalization normalizer = new NormalizerStandardize();
            normalizer.fit(trainingData);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
            normalizer.transform(trainingData);     //Apply normalization to the training data
            normalizer.transform(testData);         //Apply normalization to the test data. This is using statistics calculated from the *training* set
            
            nbn.fit(trainingData);
            // TODO: validate with training data, maybe?
        });
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
	 * 
	 * 
	 * @param sourceDirectory Directory from which the machine files are read
	 * @param targetDirectory Directory in which the *.nbtrain files will be put 
	 * 
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory) {
		// set up generator
		TrainingDataCollector tdc = nbn.getTrainingDataCollector();
		TrainingSetGenerator tsg = new TrainingSetGenerator(tdc);
		// set up training data directory
		Path fullTargetDirectory = targetDirectory.resolve(tdc.getClass().toString());
		
		// generate data
		tsg.generateTrainingSet(sourceDirectory, fullTargetDirectory);
		// enhance logs
		tsg.logStatistics();
		tsg.logTrainingSetAnalysis(fullTargetDirectory);
		
		// generate csv
		tsg.generateCSVFromNBTrainData(fullTargetDirectory, fullTargetDirectory.resolve("data.csv"));
	}

}
