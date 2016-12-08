package neurob.core.nets.interfaces;

import java.io.IOException;
import java.nio.file.Path;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import neurob.training.generators.interfaces.TrainingDataCollector;

/**
 * Wrapper interface for DeepLearning4J nets.
 * <p> Implement your very own neural net classes with this interface, to use with {@link neurob.core.NeuroB NeuroB}.
 * </p>
 * <p> <b>Usage:</b>
 * <br> TODO: write usage example and stuff.
 * </p>
 * 
 * @author jannik
 *
 */
@Deprecated
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
	 * Returns the number of values a single label can have.
	 * <br>
	 * Either a positive integer, or -1 if the net uses regression
	 * @return
	 */
	default int getLabelSize(){ return 2;}
	
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
	 * Returns a {@link RecordReader} holding the data to train the net.
	 * @param source Path to the source data, e.g. a csv file.
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	default RecordReader getRecordReader(Path source) throws IOException, InterruptedException{
		// set up training data
		RecordReader recordReader = new CSVRecordReader(1,","); // skip first line (header line)s
		recordReader.initialize(new FileSplit(source.toFile())); // default implementation assumes a CSV file
		
		return recordReader;
	}
	
	/**
	 * Returns a specialised {@link DataSetIterator} to use with the neural net.
	 * <br>
	 * @param recordReader RecordReader to be used
	 * @return
	 * @see {@link #getRecordReader(Path)}
	 */
	DataSetIterator getDataSetIterator(RecordReader recordReader);
	
	/**
	 * Fit the training data into the model
	 * @param trainingData
	 */
	void fit(DataSet trainingData);
	
	/**
	 * Loads a neural net from the specified file
	 * @param file
	 * @return this
	 * @throws IOException 
	 */
	NeuroBNet loadFromFile(Path file) throws IOException;
	
	/**
	 * Saves the neural net in the specified file
	 * @param file
	 * @throws IOException 
	 */
	void safeToFile(Path file) throws IOException;
	
	/**
	 * Forwards the input to the neural net and returns the calculated output
	 * @param input
	 * @return
	 */
	INDArray output(INDArray input);
}
