package neurob.core.nets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import neurob.core.nets.interfaces.NeuroBNet;
import neurob.training.generators.SolverSelectionDataCollector;
import neurob.training.generators.interfaces.TrainingDataCollector;

/**
 * This neural net classifies between which solver to choose, ProB, KodKod or ProB/Z3.
 * @author jannik
 *
 */

@Deprecated
public class PredicateSolverSelectionNet implements NeuroBNet {
	// Network related
	private MultiLayerNetwork model;
	private SolverSelectionDataCollector tdc;
	// RNG
	private long seed;
	private Random rnd;
	
	public PredicateSolverSelectionNet() {
		// set up RNG
		// - get random seed, then use it
		rnd = new Random();
		seed = rnd.nextLong();
		rnd = new Random(seed);
		
		// set up data collector
		tdc = new SolverSelectionDataCollector();
		
	}

	@Override
	public int getNumberOfInputs() {
		return tdc.getNumberOfFeatures();
	}

	@Override
	public int getNumberOfOutputs() {
		return tdc.getNumberOfLabels();
	}
	
	@Override
	public int getLabelSize(){
		return 4;
	}
	
	@Override
	public DataSetIterator getDataSetIterator(RecordReader recordReader){
		DataSetIterator iterator = new RecordReaderDataSetIterator(
				recordReader,
				1000,						// batch size
				getNumberOfInputs(),	// starting index of the label values in the csv
				getLabelSize()			// Number of different classes
			);
		return iterator;
	}

	@Override
	public void fit(DataSet trainingData) {
		model.fit(trainingData);
	}
	
	@Override
	public INDArray output(INDArray input) {
		return model.output(input);
	};

	@Override
	public NeuroBNet setSeed(Long seed) {
		this.seed = seed;
		rnd = new Random(this.seed);
		return this;
	}

	@Override
	public NeuroBNet build() {
		// set up network model
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(1)
                .learningRate(0.006)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .regularization(true).l2(1e-4)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(tdc.getNumberOfFeatures())
                        .nOut(1000)
                        .activation("sigmoid")
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(1000)
                        .nOut(getLabelSize())
                        .activation("softmax")
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .pretrain(false).backprop(true)
                .build();
		// build model
        model = new MultiLayerNetwork(conf);
        
		return this;
	}

	@Override
	public TrainingDataCollector getTrainingDataCollector() {
		return tdc;
	}

	@Override
	public NeuroBNet loadFromFile(Path file) throws IOException {
		model = ModelSerializer.restoreMultiLayerNetwork(file.toFile());
		seed = model.getDefaultConfiguration().getSeed();
		
		return this;
	}

	@Override
	public void safeToFile(Path file) throws IOException {
		ModelSerializer.writeModel(model, file.toFile(), true);
		
	}

}
