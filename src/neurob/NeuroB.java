package neurob;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.FeatureCollector;
import neurob.core.features.FeatureData;
import neurob.core.nets.interfaces.NeuroBNet;

public class NeuroB {
	private BParser bparser;
	private FeatureCollector featureCollector;
	private FeatureData fd;
	private NeuroBNet nbn;
	// RNG
	private long seed = 12345;
	private Random rnd = new Random(seed);

	public NeuroB(NeuroBNet neuroBNet) {
		// set up parser
		bparser = new BParser();
		// set up feature handling
		featureCollector = new FeatureCollector();
		fd = new FeatureData();
		// link neural net
		nbn = neuroBNet;
		
	}
	
	public void processPredicate(String pred){
		try {
			// get AST from predicate
			System.out.println(pred);
			Start ast = bparser.parse(pred, false);
			
			// get features
			ast.apply(featureCollector);
			fd = featureCollector.getFeatureData();
			
			// print results
			System.out.println(fd);
			
		} catch (BException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

}
