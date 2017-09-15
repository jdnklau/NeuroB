package neurob.core.nets.search;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.features.interfaces.RNNFeatures;
import neurob.core.util.ProblemType;
import neurob.training.generators.interfaces.LabelGenerator;
import org.deeplearning4j.arbiter.MultiLayerSpace;
import org.deeplearning4j.arbiter.layers.*;
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
	 * @param hiddenLayersMin Minimum number of hidden layers to use
	 * @param hiddenLayersMax Maximum number of hidden layers to use
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
			int hiddenLayersMin, int hiddenLayersMax, int hiddenSizeMin, int hiddenSizeMax,
			double learningRateMin, double learningRateMax,
			FeatureGenerator features, LabelGenerator labelling, int seed) {
		return feedForwardModel(hiddenLayersMin, hiddenLayersMax, hiddenSizeMin, hiddenSizeMax,
				learningRateMin, learningRateMax, features, labelling, 1., seed);
	}

	/**
	 * Creates a hyper parameter model space with variable size of hidden layers and learning rate.
	 * <p>
	 *     For each hidden layer, a random integer size from {@code [hiddenSizeMin, hiddenSizeMax]}
	 *     is chosen.
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
	 * <p>
	 *     {@code keepProb} handles drop out, and represents the probability to keep a node
	 *     during training. A probability of 1 results in effectively no dropout
	 * </p>
	 * @param hiddenLayersMin Minimum number of hidden layers to use
	 * @param hiddenLayersMax Maximum number of hidden layers to use
	 * @param hiddenSizeMin Lower bound of size for each hidden layer
	 * @param hiddenSizeMax Upper bound of size for each hidden layer
	 * @param learningRateMin Lower bound of the learning rate to use
	 * @param learningRateMax Upper bound of the learnign rate to use
	 * @param features {@link FeatureGenerator} to use
	 * @param labelling {@link LabelGenerator} to use
	 * @param keepProb Probability to keep a node during drop out
	 * @param seed Seed for the RNG
	 * @return MultiLayerSpace spanning all possible models
	 */
	public static MultiLayerSpace feedForwardModel(
			int hiddenLayersMin, int hiddenLayersMax, int hiddenSizeMin, int hiddenSizeMax,
			double learningRateMin, double learningRateMax,
			FeatureGenerator features, LabelGenerator labelling, double keepProb, int seed){
		// set up learning rate
		ParameterSpace<Double> lr = new ContinuousParameterSpace(learningRateMin, learningRateMax);
		// Set up hyper parameters
		MultiLayerSpace.Builder spaceBuilder = new MultiLayerSpace.Builder()
				.seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.iterations(1)
				.learningRate(lr)
				.updater(Updater.RMSPROP)//.momentum(0.9)
				.regularization(true).l2(1e-4).l1(1e-4).dropOut(keepProb);

		// set up layers
		ParameterSpace<Integer> hiddenLayers
				= new IntegerParameterSpace(hiddenLayersMin, hiddenLayersMax);
		ParameterSpace<Integer> layerSize = new IntegerParameterSpace(hiddenSizeMin, hiddenSizeMax);
		spaceBuilder.addLayer(new DenseLayerSpace.Builder()
				.nIn(features.getFeatureDimension())
				.activation("tanh")
				.weightInit(WeightInit.XAVIER)
				.nOut(layerSize)
				.build(), hiddenLayers, true);
		// FIXME: parameter true in line above corresponds to bug mentioned in the java doc comment

		// output layer configuration depending on regression or classification
		String activationFunction;
		LossFunction lossFunction;
		if(labelling.getProblemType() == ProblemType.REGRESSION){
			lossFunction = LossFunction.MSE;
			activationFunction = "identity"; // FIXME: maybe wrong string; unsure as Arbiter does not update to the enums of DL4J
		}
		else {
			if(labelling.getClassCount() > 2){
				lossFunction = LossFunction.MCXENT;
			} else {
				lossFunction = LossFunction.NEGATIVELOGLIKELIHOOD;
			}
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
	 *     There must be at least 1 fully connected layer.
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
	 * @param convolutionLayersMin Minimum number of convolution layers
	 * @param convolutionLayersMax Maximum number of convolution layers
	 * @param filtersMin Minimum amount of filters per convolution layer
	 * @param filtersMax Maximum amount of filters per convolution layer
	 * @param filterSizeMin Minimum filter size
	 * @param filterSizeMax Maximum filter size
	 * @param fullyConnectedLayersMin Minimum amount of fully connected layers after convolution
	 * @param fullyConnectedLayersMax Maximum amount of fully connected layers after convolution
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
			int convolutionLayersMin, int convolutionLayersMax, int filtersMin, int filtersMax,
			int filterSizeMin, int filterSizeMax,
			int fullyConnectedLayersMin, int fullyConnectedLayersMax,
			int fullyConnectedSizeMin, int fullyConnectedSizeMax,
			double learningRateMin, double learningRateMax,
			ConvolutionFeatures features, LabelGenerator labelling, int seed) {
		return convolutionalModel(convolutionLayersMin, convolutionLayersMax,
				filtersMin, filtersMax, filterSizeMin, filterSizeMax,
				fullyConnectedLayersMin, fullyConnectedLayersMax,
				fullyConnectedSizeMin, fullyConnectedSizeMax,
				learningRateMin, learningRateMax,
				features, labelling, 1.,  seed);
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
	 *     There must be at least 1 fully connected layer.
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
	 * <p>
	 *     {@code keepProb} handles drop out, and represents the probability to keep a node
	 *     during training. A probability of 1 results in effectively no dropout
	 * </p>
	 * @param convolutionLayersMin Minimum number of convolution layers
	 * @param convolutionLayersMax Maximum number of convolution layers
	 * @param filtersMin Minimum amount of filters per convolution layer
	 * @param filtersMax Maximum amount of filters per convolution layer
	 * @param filterSizeMin Minimum filter size
	 * @param filterSizeMax Maximum filter size
	 * @param fullyConnectedLayersMin Minimum amount of fully connected layers after convolution
	 * @param fullyConnectedLayersMax Maximum amount of fully connected layers after convolution
	 * @param fullyConnectedSizeMin Lower bound for size of each fully connected layer
	 * @param fullyConnectedSizeMax Upper bound for size of each fully connected layer
	 * @param learningRateMin Lower bound of the learning rate to use
	 * @param learningRateMax Upper bound of the learnign rate to use
	 * @param features {@link FeatureGenerator} to use
	 * @param labelling {@link LabelGenerator} to use
	 * @param keepProb Probability to keep a node during dropout
	 * @param seed Seed for the RNG
	 * @return MultiLayerSpace spanning all possible models
	 */public static MultiLayerSpace convolutionalModel(
			int convolutionLayersMin, int convolutionLayersMax, int filtersMin, int filtersMax,
			int filterSizeMin, int filterSizeMax,
			int fullyConnectedLayersMin, int fullyConnectedLayersMax,
			int fullyConnectedSizeMin, int fullyConnectedSizeMax,
			double learningRateMin, double learningRateMax,
			ConvolutionFeatures features, LabelGenerator labelling, double keepProb, int seed){
		// set up learning rate
		ParameterSpace<Double> lr = new ContinuousParameterSpace(learningRateMin, learningRateMax);
		// Set up hyper parameters
		MultiLayerSpace.Builder spaceBuilder = new MultiLayerSpace.Builder()
				.seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.iterations(1)
				.learningRate(lr)
				.updater(Updater.RMSPROP)//.momentum(0.9)
				.regularization(true).l2(1e-4).l1(1e-4).dropOut(keepProb);

		// set up parameter space for filter size
		List<int[]> filterSizes = new ArrayList<>();
		for(int k=filterSizeMin; k<=filterSizeMax; k++){
			filterSizes.add(new int[]{k,k});
		}
		ParameterSpace<int[]> filterSize = new DiscreteParameterSpace<>(filterSizes);

		// set conv layers
		ParameterSpace<Integer> convolutionLayers
				= new IntegerParameterSpace(convolutionLayersMin, convolutionLayersMax);
		ParameterSpace<Integer> layerSize = new IntegerParameterSpace(filtersMin, filtersMax);
		spaceBuilder.addLayer(new ConvolutionLayerSpace.Builder()
				.kernelSize(filterSize)
				.nIn(features.getFeatureChannels())
				.activation("relu") // FIXME: unsure whether "relu" or "RELU"; Arbiter does not use theDL4J enum for now
				.weightInit(WeightInit.XAVIER)
				.nOut(layerSize)
				.build(), convolutionLayers, true);
		// FIXME: parameter true in line above corresponds to bug mentioned in the java doc comment

		// fully connected layers
		ParameterSpace<Integer> fullyConnectedLayers
				= new IntegerParameterSpace(fullyConnectedLayersMin, fullyConnectedLayersMax);
		layerSize = new IntegerParameterSpace(fullyConnectedSizeMin, fullyConnectedSizeMax);
		spaceBuilder.addLayer(new DenseLayerSpace.Builder()
				.nOut(layerSize)
				.activation("relu")
				.weightInit(WeightInit.XAVIER)
				.build(), fullyConnectedLayers, true);

		// output layer configuration depending on regression or classification
		String activationFunction;
		LossFunction lossFunction;
		if(labelling.getProblemType() == ProblemType.REGRESSION){
			lossFunction = LossFunction.MSE;
			activationFunction = "identity"; // FIXME: maybe wrong string; unsure as Arbiter does not update to the enums of DL4J
		}
		else {
			if(labelling.getClassCount() > 2)
				lossFunction = LossFunction.MCXENT;
			else
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
		return space;
	}

	/**
	 * Creates a hyper parameter model space for a recurrent neural network
	 * with variable size of hidden layers and learning rate.
	 * <p>
	 *     For each hidden layer, a random integer size from {@code [hiddenSizeMin, hiddenSizeMax]}
	 *     is chosen.
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
	 * @param hiddenLayersMin Minimum number of hidden layers to use
	 * @param hiddenLayersMax Maximum number of hidden layers to use
	 * @param hiddenSizeMin Lower bound of size for each hidden layer
	 * @param hiddenSizeMax Upper bound of size for each hidden layer
	 * @param learningRateMin Lower bound of the learning rate to use
	 * @param learningRateMax Upper bound of the learning rate to use
	 * @param features {@link FeatureGenerator} to use
	 * @param labelling {@link LabelGenerator} to use
	 * @param seed Seed for the RNG
	 * @return MultiLayerSpace spanning all possible models
	 */
	public static MultiLayerSpace recurrentModel(
			int hiddenLayersMin, int hiddenLayersMax, int hiddenSizeMin, int hiddenSizeMax,
			double learningRateMin, double learningRateMax,
			RNNFeatures features, LabelGenerator labelling, int seed){
		// set up learning rate
		ParameterSpace<Double> lr = new ContinuousParameterSpace(learningRateMin, learningRateMax);
		// Set up hyper parameters
		MultiLayerSpace.Builder spaceBuilder = new MultiLayerSpace.Builder()
				.seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.iterations(1)
				.learningRate(lr)
				.updater(Updater.RMSPROP)//.momentum(0.9)
				.regularization(true).l2(1e-4).l1(1e-4);

		// set up layers
		ParameterSpace<Integer> hiddenLayers
				= new IntegerParameterSpace(hiddenLayersMin, hiddenLayersMax);
		ParameterSpace<Integer> layerSize = new IntegerParameterSpace(hiddenSizeMin, hiddenSizeMax);
		spaceBuilder.addLayer(new GravesLSTMLayerSpace.Builder()
				.nIn(features.getFeatureDimension())
				.activation("tanh")
				.weightInit(WeightInit.XAVIER)
				.nOut(layerSize)
				.build(), hiddenLayers, true);
		// FIXME: parameter true in line above corresponds to bug mentioned in the java doc comment

		// output layer configuration depending on regression or classification
		String activationFunction;
		LossFunction lossFunction;
		if(labelling.getProblemType() == ProblemType.REGRESSION){
			lossFunction = LossFunction.MSE;
			activationFunction = "identity"; // FIXME: maybe wrong string; unsure as Arbiter does not update to the enums of DL4J
		}
		else {
			if(labelling.getClassCount() > 2){
				lossFunction = LossFunction.MCXENT;
			} else {
				lossFunction = LossFunction.NEGATIVELOGLIKELIHOOD;
			}
			activationFunction = "softmax";
		}

		// the output layer itself
		spaceBuilder.addLayer(new RnnOutputLayerSpace.Builder()
				.nIn(layerSize)
				.nOut(labelling.getLabelDimension())
				.activation(activationFunction).lossFunction(lossFunction)
				.build());

		// build and return
		return spaceBuilder.pretrain(false).backprop(true).build();
	}
}
