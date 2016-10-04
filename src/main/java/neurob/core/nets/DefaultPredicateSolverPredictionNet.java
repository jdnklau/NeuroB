package neurob.core.nets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

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
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import neurob.core.nets.interfaces.NeuroBNet;
import neurob.training.generators.DefaultTrainingDataCollector;
import neurob.training.generators.interfaces.TrainingDataCollector;

public class DefaultPredicateSolverPredictionNet implements NeuroBNet {
	// Network related
	private MultiLayerNetwork model;
	private DefaultTrainingDataCollector tdc;
	// RNG
	private long seed;
	private Random rnd;
	
	public DefaultPredicateSolverPredictionNet() {
		// set up RNG
		// - get random seed, then use it
		rnd = new Random();
		seed = rnd.nextLong();
		rnd = new Random(seed);
		
		// set up data collector
		tdc = new DefaultTrainingDataCollector();
		
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
		return 2;
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
                        .nOut(tdc.getNumberOfLabels())
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
