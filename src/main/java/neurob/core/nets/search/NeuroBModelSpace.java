package neurob.core.nets.search;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.ProblemType;
import neurob.training.generators.interfaces.LabelGenerator;
import org.deeplearning4j.arbiter.MultiLayerSpace;
import org.deeplearning4j.arbiter.layers.ConvolutionLayerSpace;
import org.deeplearning4j.arbiter.layers.DenseLayerSpace;
import org.deeplearning4j.arbiter.layers.OutputLayerSpace;
import org.deeplearning4j.arbiter.optimize.api.ParameterSpace;
import org.deeplearning4j.arbiter.optimize.parameter.FixedValue;
import org.deeplearning4j.arbiter.optimize.parameter.continuous.ContinuousParameterSpace;
import org.deeplearning4j.arbiter.optimize.parameter.discrete.DiscreteParameterSpace;
import org.deeplearning4j.arbiter.optimize.parameter.integer.IntegerParameterSpace;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.util.ArrayList;
import java.util.List;


/**
 * This class provides static methods to set up a hyper parameter search space easily without having
 * to deal with Deeplearning4j directly.
 * <p>
 * Of course, defining a custom model via dl4j is more powerfull.
 * <p>
 * Usage:
 * <pre><code>
 *     // set up the Model Space, then create a candidate generator
 *     MultiLayerSpace mlSpace = NeuroBModelSpace.feedForwardModel(3, 20, 30, 0.0001, 0.1,
 *             featureGenerator, labelGenerator, 1234);
 *    {@code CandidateGenerator<DL4JConfiguration>} candidates
 *             = new {@code RandomSearchGenerator<>}(mlSpace); // input model space to generator
 *     // now set up search and train multiple models
 *    {@code HyperParameterSearch<RandomSearchGenerator>} generator
 *             = new {@code HyperParameterSearch<>}(candidates,
 *                     featureGenerator, labelGenerator);
 *     // generator.train...
 * </code></pre>
 *
 * @author Jannik Dunkelau
 */
public class NeuroBModelSpace {

	/**
	 * Creates a hyper parameter model space with variable size of hidden layers and learning rate.
	 * <p>
	 *     For each hidden layer, a random integer size from {@code [hiddenSizeMin, hiddenSizeMax]}
	 *     is chosen.
	 *     The amount of hidden layers is fix and does not vary from candidate to candidate.
	 *     The learning rate will also be chosen from the continuous interval
	 *     {@code [learningRateMin, learningRateMax]}
	 * </p>
	 * <p>
	 *     <b>NOTE:</b> Each hidden layer will have the exactly same number of neurons, due a bug in
	 *     DL4J Arbiter, that makes different parameter spaces with exact same configurations
	 *     unusable.
	 *     On the other hand, copying the same layer with different values used from the
	 *     parameter spaces is not implemented yet (DL4J version 0.8.0)
	 * </p>
	 * <p>
	 *     Each hidden layer uses tanh as activation function, and is initialised by
	 *     Xavier initialisation.
	 * </p>
	 * <p>
	 *     The output layer's configuration is dependend on the {@link LabelGenerator} in use.
	 *     If the LabelGenerator spans a regression problem, the used activation function is
	 *     the identity function, the loss function is Mean Squared Error.
	 *     Otherwise, softmax and negative loglikelihood are used respectively.
	 * </p>
	 * @param hiddenLayers Number of hidden layers to use; this remains fixed for each candidate
	 * @param hiddenSizeMin Lower bound of size for each hidden layer
	 * @param hiddenSizeMax Upper bound of size for each hidden layer
	 * @param learningRateMin Lower bound of the learning rate to use
	 * @param learningRateMax Upper bound of the learnign rate to use
	 * @param features {@link FeatureGenerator} to use
	 * @param labelling {@link LabelGenerator} to use
	 * @param seed Seed for the RNG
	 * @return MultiLayerSpace spanning all possible models
	 */
	public static MultiLayerSpace feedForwardModel(
			int hiddenLayers, int hiddenSizeMin, int hiddenSizeMax,
			double learningRateMin, double learningRateMax,
			FeatureGenerator features, LabelGenerator labelling, int seed){
		// set up learning rate
		ParameterSpace<Double> lr = new ContinuousParameterSpace(learningRateMin, learningRateMax);
		// Set up hyper parameters
		MultiLayerSpace.Builder spaceBuilder = new MultiLayerSpace.Builder()
				.seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.iterations(1)
				.learningRate(lr)
				.updater(Updater.NESTEROVS).momentum(0.9)
				.regularization(true).l2(1e-4);

		// set first layer
		ParameterSpace<Integer> layerSize = new IntegerParameterSpace(hiddenSizeMin, hiddenSizeMax);
		spaceBuilder.addLayer(new DenseLayerSpace.Builder()
				.nIn(features.getFeatureDimension())
				.activation("tanh")
				.weightInit(WeightInit.XAVIER)
				.nOut(layerSize)
				.build(), new FixedValue<>(hiddenLayers), true);
		// FIXME: parameter true in line above corresponds to bug mentioned in the java doc comment

		// remaining layers but output
//		ParameterSpace<Integer> layerSizeIn;
//		for(int i=1; i<hiddenLayers; i++){
//			layerSizeIn = layerSize;
//			// set up new random size for layer
//			layerSize = new IntegerParameterSpace(hiddenSizeMin, hiddenSizeMax);
//			spaceBuilder.addLayer(new DenseLayerSpace.Builder()
//					.nIn(layerSizeIn)
//					.activation("tanh")
//					.weightInit(WeightInit.XAVIER)
//					.nOut(layerSize)
//					.build());
//		}
		// output layer configuration depending on regression or classification
		String activationFunction;
		LossFunction lossFunction;
		if(labelling.getProblemType() == ProblemType.REGRESSION){
			lossFunction = LossFunction.MSE;
			activationFunction = "identity"; // FIXME: maybe wrong string; unsure as Arbiter does not update to the enums of DL4J
		}
		else {
			lossFunction = LossFunction.NEGATIVELOGLIKELIHOOD;
			activationFunction = "softmax";
		}

		// the output layer itself
		spaceBuilder.addLayer(new OutputLayerSpace.Builder()
			.nIn(layerSize)
			.nOut(labelling.getLabelDimension())
			.activation(activationFunction).lossFunction(lossFunction)
			.build());

		// build and return
		return spaceBuilder.pretrain(false).backprop(true).build();
	}

	/**
	 * Creates a hyper parameter model space for a convolutional neural network with variable size
	 * of filters used, amount of filters learned per layer, size of fully connected layers,
	 * and learning rate.
	 * <p>
	 *     For each convolution layer, a random integer size from {@code [filtersMin, filtersMax]}
	 *     is chosen, dictating how many filters will be learned by this layer.
	 *     Also, a random integer k from {@code [filterSizeMin, filterSizeMax]} sets the filter size
	 *     to {@code k*k}, but model wide.
	 * </p>
	 * <p>
	 *     <b>NOTE:</b> Each convolution layer will have the exactly same number of filters, due to
	 *     a bug in DL4J Arbiter, that makes different parameter spaces with exact same
	 *     configurations unusable.
	 *     On the other hand, copying the same layer with different values used from the
	 *     parameter spaces is not implemented yet (DL4J version 0.8.0)
	 * </p>
	 * <p>
	 *     After the convolution layers, the model(s) will consist of fully connected layers,
	 *     each of a size from {@code [fullyConnectedSizeMin, fullyConnectedSizeMax]}.
	 * </p>
	 * <p>
	 *     The amount of layers is fix  for both, convolution and fully connected,
	 *     and does not vary between candidates candidate.
	 *     The learning rate will be chosen from the continuous interval
	 *     {@code [learningRateMin, learningRateMax]} and is set model wide
	 * </p>
	 * <p>
	 *     Both layer types use ReLU as activation function, and are initialised by
	 *     Xavier initialisation.
	 * </p>
	 * <p>
	 *     The output layer's configuration is dependend on the {@link LabelGenerator} in use.
	 *     If the LabelGenerator spans a regression problem, the used activation function is
	 *     the identity function, the loss function is Mean Squared Error.
	 *     Otherwise, softmax and negative loglikelihood are used respectively.
	 * </p>
	 * @param convolutionLayers Number of convolution layers
	 * @param filtersMin Minimum amount of filters per convolution layer
	 * @param filtersMax Maximum amount of filters per convolution layer
	 * @param filterSizeMin Minimum filter size
	 * @param filterSizeMax Maximum filter size
	 * @param fullyConnectedLayers Amount of fully connected layers after convolution; at least 1
	 * @param fullyConnectedSizeMin Lower bound for size of each fully connected layer
	 * @param fullyConnectedSizeMax Upper bound for size of each fully connected layer
	 * @param learningRateMin Lower bound of the learning rate to use
	 * @param learningRateMax Upper bound of the learnign rate to use
	 * @param features {@link FeatureGenerator} to use
	 * @param labelling {@link LabelGenerator} to use
	 * @param seed Seed for the RNG
	 * @return MultiLayerSpace spanning all possible models
	 */
	public static MultiLayerSpace convolutionalModel(
			int convolutionLayers, int filtersMin, int filtersMax,
			int filterSizeMin, int filterSizeMax,
			int fullyConnectedLayers, int fullyConnectedSizeMin, int fullyConnectedSizeMax,
			double learningRateMin, double learningRateMax,
			ConvolutionFeatures features, LabelGenerator labelling, int seed){
		// set up learning rate
		ParameterSpace<Double> lr = new ContinuousParameterSpace(learningRateMin, learningRateMax);
		// Set up hyper parameters
		MultiLayerSpace.Builder spaceBuilder = new MultiLayerSpace.Builder()
				.seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.iterations(1)
				.learningRate(lr)
				.updater(Updater.NESTEROVS).momentum(0.9)
				.regularization(true).l2(1e-4);

		// set up parameter space for filter size
		List<int[]> filterSizes = new ArrayList<>();
		for(int k=filtersMin; k<=filterSizeMax; k++){
			filterSizes.add(new int[]{k,k});
		}
		ParameterSpace<int[]> filterSize = new DiscreteParameterSpace<>(filterSizes);

		// set first conv layer
		ParameterSpace<Integer> layerSize = new IntegerParameterSpace(filtersMin, filtersMax);
		spaceBuilder.addLayer(new ConvolutionLayerSpace.Builder()
				.kernelSize(filterSize)
				.nIn(features.getFeatureChannels())
				.activation("relu") // FIXME: unsure whether "relu" or "RELU"; Arbiter does not use theDL4J enum for now
				.weightInit(WeightInit.XAVIER)
				.nOut(layerSize)
				.build(), new FixedValue<>(convolutionLayers), true);
		// FIXME: parameter true in line above corresponds to bug mentioned in the java doc comment

//		// remaining conv layers
//		ParameterSpace<Integer> layerSizeIn;
//		for(int i=1; i<convolutionLayers; i++){
//			layerSizeIn = layerSize;
//			// NOTE: decided not to vary filter size inside model
////			filterSize = new DiscreteParameterSpace<>(filterSizes); // for each layer different
//			// set up new random size for layer
//			layerSize = new IntegerParameterSpace(filtersMin, filtersMax);
//			spaceBuilder.addLayer(new ConvolutionLayerSpace.Builder()
//					.kernelSize(filterSize)
//					.nIn(layerSizeIn)
//					.activation("relu") // FIXME: unsure whether "relu" or "RELU"; Arbiter does not use theDL4J enum for now
//					.weightInit(WeightInit.XAVIER)
//					.nOut(layerSize)
//					.build());
//		}
//		// fully connected layers
//		for(int i=0; i<fullyConnectedLayers; i++){
			layerSize = new IntegerParameterSpace(fullyConnectedSizeMin, fullyConnectedSizeMax);
			spaceBuilder.addLayer(new DenseLayerSpace.Builder()
					.nOut(layerSize)
					.activation("relu")
					.weightInit(WeightInit.XAVIER)
					.build(), new FixedValue<>(fullyConnectedLayers), true);
//		}
		// output layer configuration depending on regression or classification
		String activationFunction;
		LossFunction lossFunction;
		if(labelling.getProblemType() == ProblemType.REGRESSION){
			lossFunction = LossFunction.MSE;
			activationFunction = "identity"; // FIXME: maybe wrong string; unsure as Arbiter does not update to the enums of DL4J
		}
		else {
			lossFunction = LossFunction.NEGATIVELOGLIKELIHOOD;
			activationFunction = "softmax";
		}

		// the output layer itself
		spaceBuilder.addLayer(new OutputLayerSpace.Builder()
				.nIn(layerSize)
				.nOut(labelling.getLabelDimension())
				.activation(activationFunction).lossFunction(lossFunction)
				.build());

		// build layer
		MultiLayerSpace space = spaceBuilder.pretrain(false).backprop(true)
				// NOTE: .setInputType sadly does not work correctly as of version 0.8.0
				.cnnInputSize(features.getImageHeight(), features.getImageWidth(),
						features.getFeatureChannels())
				.build();

		// build and return
		return spaceBuilder.pretrain(false).backprop(true).build();
	}


}
