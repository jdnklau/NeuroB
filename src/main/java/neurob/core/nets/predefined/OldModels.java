package neurob.core.nets.predefined;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import neurob.core.features.CodeImages;
import neurob.core.features.TheoryFeatures;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.nets.NeuroBNet;
import neurob.core.util.SolverType;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.labelling.SolverClassificationGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;

public class OldModels {
	
	
	private static NeuroBNet getOldModel(FeatureGenerator fg, LabelGenerator lg, int seed){
		MultiLayerNetwork model;
		
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(1)
                .learningRate(0.006)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .regularization(true).l2(1e-4)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(fg.getFeatureDimension())
                        .nOut(1000)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(1000)
                        .nOut(lg.getLabelDimension())
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .pretrain(false).backprop(true)
                .build();
		// build model
        model = new MultiLayerNetwork(conf);
        
        return new NeuroBNet(model, fg, lg);
	}

	public static NeuroBNet getProBPredictionNet(int seed) {
		return getOldModel(
				new TheoryFeatures(),
				new SolverClassificationGenerator(SolverType.PROB),
				seed);
	}
	
	public static NeuroBNet getProBPredictionWithCodePortfolioNet(int seed, int size){
		return getOldModel(
				new CodeImages(size),
				new SolverClassificationGenerator(SolverType.PROB),
				seed);
	}
	
	public static NeuroBNet getKodKodPredictionNet(int seed){
		return getOldModel(
				new TheoryFeatures(),
				new SolverClassificationGenerator(SolverType.KODKOD),
				seed);
	}
	
	public static NeuroBNet getKodKodPredictionWithCodePortfolioNet(int seed, int size){
		return getOldModel(
				new CodeImages(size),
				new SolverClassificationGenerator(SolverType.KODKOD),
				seed);
	}
	
//	NOTE: No longer supported
//	public static NeuroBNet getPredicateSolverPredictionNet(int seed){
//		return getOldModel(
//				new TheoryFeatures(),
//				new SolverClassificationGenerator(true, true, true),
//				seed);
//	}
//	
//	public static NeuroBNet getPredicateSolverPredictionWithCodePortfolioNet(int seed, int size){
//		return getOldModel(
//				new CodeImages(size),
//				new SolverClassificationGenerator(true, true, true),
//				seed);
//	}
	
	public static NeuroBNet getPredicateSolverSelectionNet(int seed){
		return getOldModel(
				new TheoryFeatures(),
				new SolverSelectionGenerator(),
				seed);
	}
	
	public static NeuroBNet getPredicateSolverSelectionWithCodePortfolioNet(int seed, int size){
		return getOldModel(
				new CodeImages(size),
				new SolverSelectionGenerator(),
				seed);
	}
}
