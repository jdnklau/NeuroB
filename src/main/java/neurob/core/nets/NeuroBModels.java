package neurob.core.nets;

import java.util.Random;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.ProblemType;
import neurob.training.generators.interfaces.LabelGenerator;

/**
 * This class offers utility to easily access different models
 * without having to deal with Deeplearning4J directly.
 * <p>
 * Of course, defining custom models with dl4j is more powerfull,
 * but as dl4j has a relatively high entrance bar for beginners,
 * this should ease up the usage of NeuroB.
 * @author jannik
 *
 */
public class NeuroBModels {

	/**
	 * Creates a feed forward neural network.
	 * <p>
	 * This is a special case of {@link #feedForwardModel(int[], double, FeatureGenerator, LabelGenerator, int)}
	 * with a predefined size of hidden layers, namely {512, 128, 128},
	 * and a random seed.
	 * @param learningRate
	 * @param features
	 * @param labelling
	 * @return
	 * @see #feedForwardModel(int[], double, FeatureGenerator, LabelGenerator, int)
	 */
	public static NeuroBNet feedForwardModel(double learningRate, FeatureGenerator features, LabelGenerator labelling){
		return feedForwardModel(
				new int[]{512,128,128}, learningRate, features, labelling);
	}

	/**
	 * Creates a feed forward neural network.
	 * <p>
	 * This is a special case of {@link #feedForwardModel(int[], double, FeatureGenerator, LabelGenerator, int)}
	 * with a predefined size of hidden layers, namely {512, 128, 128}.
	 *
	 * @param learningRate
	 * @param features
	 * @param labelling
	 * @param seed
	 * @return
	 * @see #feedForwardModel(int[], double, FeatureGenerator, LabelGenerator, int)
	 */
	public static NeuroBNet feedForwardModel(double learningRate, FeatureGenerator features, LabelGenerator labelling, int seed){
		return feedForwardModel(
				new int[]{512,128,128}, learningRate, features, labelling, seed);
	}

	/**
	 * Creates a feed forward neural network with given structure.
	 * <p>
	 * This is a special case of {@link #feedForwardModel(int[], double, FeatureGenerator, LabelGenerator, int)}
	 * with a random seed in use.
	 * @param hiddenLayers
	 * @param learningRate
	 * @param features
	 * @param labelling
	 * @return
	 * @see #feedForwardModel(int[], double, FeatureGenerator, LabelGenerator, int)
	 */
	public static NeuroBNet feedForwardModel(int[] hiddenLayers, double learningRate, FeatureGenerator features, LabelGenerator labelling){
		return feedForwardModel(hiddenLayers, learningRate, features, labelling, new Random().nextInt());
	}

	/**
	 * Creates a neural network with given structure.
	 * <p>
	 * {@code hiddenLayers} parameter gives structure of the neural net. For each entry in the array,
	 * a hidden layer with the entry's value of neurons will be created.
	 * They are stacked onto each other according to the index in the array.
	 * <br>
	 * Each hidden layer uses tanh as activation function, and is initialised by Xavier initialisation.
	 * The given {@code seed} will be used for this.
	 * <p>
	 * For example:
	 * {1000, 500, 200} would create a neural net with three hidden layers.
	 * The first one having 1000 neurons, the second one having 500 neurons,
	 * and the third one having 200 neurons.
	 * <br>
	 * The network has to have at least one hidden layer.
	 * <p>
	 * The {@link LabelGenerator} indicates the problem position,
	 * e.g. whether it is a classification or regression task.
	 * <br>
	 * For classification problems, the output layer uses soft max as activation function,
	 * paired with negative log-likelihood as loss function.
	 * <br>
	 * For regression, the output layer uses the identity function paired with MSE as loss function.
	 * @param hiddenLayers Structure to be build
	 * @param learningRate Learning rate to be used
	 * @param features
	 * @param labelling
	 * @param seed
	 * @see NeuroBNet
	 */
	public static NeuroBNet feedForwardModel(int[] hiddenLayers, double learningRate, FeatureGenerator features, LabelGenerator labelling, int seed){
		MultiLayerNetwork model;

		ListBuilder listBuilder = new NeuralNetConfiguration.Builder()
		        .seed(seed)
		        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
		        .iterations(1)
		        .learningRate(learningRate)
		        .updater(Updater.NESTEROVS).momentum(0.9)
		        .regularization(true).l2(1e-4)
		        .list();

		// Set up layers
		if(hiddenLayers.length == 0) {
			// no hidden layers
			throw new IllegalArgumentException("NeuroBNet needs to have hidden layers, but an empty array was given.");
		}
		else {
			// hidden layers!

			int lastOut = features.getFeatureDimension();

			for(int i=0; i<hiddenLayers.length; i++){
				listBuilder = listBuilder.layer(i, new DenseLayer.Builder()
						.nIn(lastOut)
						.nOut(hiddenLayers[i])
						.activation(Activation.TANH)
						.weightInit(WeightInit.XAVIER)
						.build());
				lastOut = hiddenLayers[i];
			}

			// Output layer - depending on whether we do regression or not
			LossFunction lossFunction;
			Activation activationFunction;
			if(labelling.getProblemType() == ProblemType.REGRESSION){ // Regression
				lossFunction = LossFunction.MSE;
				activationFunction = Activation.IDENTITY;
			}
			else { // No regression
				lossFunction = LossFunction.NEGATIVELOGLIKELIHOOD;
				activationFunction = Activation.SOFTMAX;
			}
			// the layer itself
			listBuilder = listBuilder.layer(hiddenLayers.length, new OutputLayer.Builder(lossFunction)
					.nIn(lastOut)
					.nOut(labelling.getLabelDimension())
					.activation(activationFunction)
					.weightInit(WeightInit.XAVIER)
					.build())
			.pretrain(false).backprop(true);
		}

		model = new MultiLayerNetwork(listBuilder.build());
		model.init();

		return new NeuroBNet(model, features, labelling);
	}

	/**
	 * Creates a convolutional neural network model.
	 * <p>
	 * This is a special case of {@link #convolutionalModel(int[], int, int[], double, ConvolutionFeatures, LabelGenerator, int)}
	 * with two convolutional layers {32, 64} with 5*5 filters,
	 * one fully connected layer of size 128,
	 * and a randomly set seed
	 * @param learningRate
	 * @param features
	 * @param labelling
	 * @return
	 * @see #convolutionalModel(int[], int, int[], double, ConvolutionFeatures, LabelGenerator, int)
	 */
	public static NeuroBConvNet convolutionalModel(double learningRate, ConvolutionFeatures features, LabelGenerator labelling){
		return convolutionalModel(new int[]{32,64}, 5, new int[]{128}, learningRate, features, labelling, new Random().nextInt());
	}

	/**
	 * Creates a convolutional neural network model.
	 * <p>
	 * This is a special case of {@link #convolutionalModel(int[], int, int[], double, ConvolutionFeatures, LabelGenerator, int)}
	 * with two convolutional layers {32, 64} with 5*5 filters,
	 * and one fully connected layer of size 128.
	 * @param learningRate
	 * @param features
	 * @param labelling
	 * @param seed
	 * @return
	 * @see #convolutionalModel(int[], int, int[], double, ConvolutionFeatures, LabelGenerator, int)
	 */
	public static NeuroBConvNet convolutionalModel(double learningRate, ConvolutionFeatures features, LabelGenerator labelling, int seed){
		return convolutionalModel(new int[]{32,64}, 5, new int[]{128}, learningRate, features, labelling, seed);
	}

	/**
	 * Creates a convolutional neural network with given structure.
	 * <p>
	 * This is a special case of {@link #convolutionalModel(int[], int, int[], double, ConvolutionFeatures, LabelGenerator, int)}
	 * with a randomly set seed.
	 *
	 * @param convLayers
	 * @param filterSize
	 * @param fcLayers
	 * @param learningRate
	 * @param features
	 * @param labelling
	 * @return
	 * @see #convolutionalModel(int[], int, int[], double, ConvolutionFeatures, LabelGenerator, int)
	 */
	public static NeuroBConvNet convolutionalModel(int[] convLayers, int filterSize, int[] fcLayers, double learningRate, ConvolutionFeatures features, LabelGenerator labelling){
		return convolutionalModel(convLayers, filterSize, fcLayers, learningRate, features, labelling, new Random().nextInt());
	}

	/**
	 * Creates a convolutional neural network with given structure.
	 * <p>
	 * {@code convLayers} parameter gives structure of the neural net.
	 * For each entry in the array, a convolutional layer the entry's
	 * value amount of {@code filterSize * filterSize} filters will be created.
	 * They are stacked onto each other according to the index in the array.
	 * <br>
	 * There has to be at least one convolutional layer
	 * <p>
	 * {@code fcLayers} parameter gives the size and amount of fully connected
	 * layers at the end of the model. For each entry in the array,
	 * a fully connected layer will be created with neuron in the amount of
	 * the respective entry's value.
	 * <br>
	 * There has to be at least one fully connected layer.
	 * <p>
	 * Both layer types use ReLU as activation function, and Xavier initialisation.
	 * <p>
	 * For example:
	 * {16, 32, 32} would create a neural net with three stacked convolution layers and a final fully connected layer.
	 * The first one learning 16 filters, the second and third one each 32.
	 * <p>
	 * The {@link LabelGenerator} indicates the problem position,
	 * e.g. whether it is a classification or regression task.
	 * <br>
	 * For classification problems, the output layer uses soft max as activation function,
	 * paired with negative log-likelihood as loss function.
	 * <br>
	 * For regression, the output layer uses the identity function paired with MSE as loss function.
	 *
	 * @param convLayers Size and amount of convolutional layers at the beginning of the model
	 * @param filterSize Size of the convolutional filters used in hidden layers
	 * @param fcLayers Size and amount of fully connected layers at the end of the model
	 * @param learningRate Learning rate to be used
	 * @param features
	 * @param labelling
	 * @param seed
	 * @return
	 */
	public static NeuroBConvNet convolutionalModel(int[] convLayers, int filterSize, int[] fcLayers, double learningRate, ConvolutionFeatures features, LabelGenerator labelling, int seed){
		MultiLayerNetwork model;

		ListBuilder listBuilder = new NeuralNetConfiguration.Builder()
		        .seed(seed)
		        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
		        .iterations(1)
		        .learningRate(learningRate)
		        .updater(Updater.NESTEROVS).momentum(0.9)
		        .regularization(true).l2(1e-4)
		        .list();

		// Set up layers
		if(convLayers.length < 1 || fcLayers.length < 1) {
			// no hidden layers
			throw new IllegalArgumentException("NeuroBConvNet needs to have at least one convolutional and one fully connected layer");
		}
		else {
			// convolutional layers
			int lastOut = features.getFeatureChannels(); // initial value for the loop
			for(int i=0; i<convLayers.length; i++){
				listBuilder = listBuilder.layer(i, new ConvolutionLayer.Builder(filterSize,filterSize)
						.nIn(lastOut)
						.stride(1,1)
						.nOut(convLayers[i])
						.activation(Activation.RELU)
						.weightInit(WeightInit.XAVIER)
						.build());
				lastOut = convLayers[i];
			}

			// Fully connected layer
			for(int i=0; i<fcLayers.length; i++){
				listBuilder = listBuilder.layer(i+convLayers.length, new DenseLayer.Builder()
						.nOut(fcLayers[i])
						.activation(Activation.RELU)
						.weightInit(WeightInit.XAVIER)
						.build());
				lastOut = fcLayers[i];
			}

			// Output layer - depending on whether we do regression or not
			LossFunction lossFunction;
			Activation activationFunction;
			if(labelling.getProblemType() == ProblemType.REGRESSION){ // Regression
				lossFunction = LossFunction.MSE;
				activationFunction = Activation.IDENTITY;
			}
			else { // No regression
				lossFunction = LossFunction.NEGATIVELOGLIKELIHOOD;
				activationFunction = Activation.SOFTMAX;
			}
			// the layer itself
			listBuilder = listBuilder.layer(convLayers.length+1, new OutputLayer.Builder(lossFunction)
					.nOut(labelling.getLabelDimension())
					.activation(activationFunction)
					.weightInit(WeightInit.XAVIER)
					.build());
		}

		model = new MultiLayerNetwork(listBuilder
				.setInputType(InputType.convolutional(features.getImageHeight(),
						features.getImageWidth(), features.getFeatureChannels()))
				.pretrain(false).backprop(true)
				.build());
		model.init();

		return new NeuroBConvNet(model, features, labelling);
	}
}
