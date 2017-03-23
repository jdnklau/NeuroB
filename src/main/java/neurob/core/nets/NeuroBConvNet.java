package neurob.core.nets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.core.util.ProblemType;
import neurob.training.generators.interfaces.LabelGenerator;

public class NeuroBConvNet extends NeuroBNet {

	public NeuroBConvNet(MultiLayerNetwork model, ConvolutionFeatures features, LabelGenerator labelling) {
		super(model, features, labelling);
		// Necessary to restrict the feature generator to ConvolutionFeatures
	}

	public NeuroBConvNet(int[] hiddenLayers, double learningRate, ConvolutionFeatures features, LabelGenerator labelling) {
		this(hiddenLayers, learningRate, features, labelling, new Random().nextInt());
	}
	
	/**
	 * Creates a convolutional neural network with given structure.
	 * <p>
	 * {@code hiddenLayers} parameter gives structure of the neural net. For each entry in the array, a convolution layer with the entry's 
	 * value of 5*5 filters will be created. The last entry is the size of a fully connected layer at the end of the network.
	 * They are stacked onto each other according to the index in the array.
	 * <br>
	 * For example:
	 * {16, 32, 32, 256} would create a neural net with three stacked convolution layers and a final fully connected layer. 
	 * The first one learning 16 filters, the second and third one each 32.
	 * The fully connected layer would have 256 neurons.
	 * <br>
	 * The network has to have at least one convolution layer, thus {@code hiddenLayers} needs to have at least two entries.
	 * @param hiddenLayers Structure to be build
	 * @param learningRate Learning rate to be used
	 * @param features
	 * @param labelling
	 * @param seed
	 */
	public NeuroBConvNet(int[] hiddenLayers, double learningRate, ConvolutionFeatures features, LabelGenerator labelling, int seed) {
		super(features, labelling);
		
		ListBuilder listBuilder = new NeuralNetConfiguration.Builder()
		        .seed(seed)
		        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
		        .iterations(1)
		        .learningRate(learningRate)
		        .updater(Updater.NESTEROVS).momentum(0.9)
		        .regularization(true).l2(1e-4)
		        .list();
		        
		// Set up layers
		if(hiddenLayers.length < 2) { 
			// no hidden layers
			throw new IllegalArgumentException("NeuroBConvNet needs to have at least two entries for the hidden layer sizes");
		}
		else {
			// hidden layers!
			
			int lastOut = features.getFeatureChannels();
			
			for(int i=0; i<hiddenLayers.length; i++){
				listBuilder = listBuilder.layer(i, new ConvolutionLayer.Builder(5,5)
						.nIn(lastOut)
						.stride(1,1)
						.nOut(hiddenLayers[i])
						.activation(Activation.RELU)
						.weightInit(WeightInit.XAVIER)
						.build());
				lastOut = hiddenLayers[i];
			}
			
			// Fully connected layer
			listBuilder = listBuilder.layer(hiddenLayers.length, new DenseLayer.Builder()
					.nIn(lastOut*25) // filters * filter size
					.nOut(hiddenLayers[hiddenLayers.length-1])
					.activation(Activation.RELU)
					.weightInit(WeightInit.XAVIER)
					.build());
			
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
			listBuilder = listBuilder.layer(hiddenLayers.length+1, new OutputLayer.Builder(lossFunction)
					.nIn(lastOut)
					.nOut(labelling.getLabelDimension())
					.activation(activationFunction)
					.weightInit(WeightInit.XAVIER)
					.build())
			.pretrain(false).backprop(true);
		}
        
		setUpNormalizer();
		
		this.model = new MultiLayerNetwork(listBuilder.build());
	}

	public NeuroBConvNet(Path modelFile, ConvolutionFeatures features, LabelGenerator labelling) throws IOException {
		super(modelFile, features, labelling);
		// Necessary to restrict the feature generator to ConvolutionFeatures
	}
	
	@Override
	protected void setUpNormalizer() {
		normalizer = new NormalizerMinMaxScaler();
	}
	
	@Override
	public DataSetIterator getDataSetIterator(Path datapath, int batchSize) throws IOException, InterruptedException {
		ConvolutionFeatures features = (ConvolutionFeatures) this.features;
		ImageRecordReader rr = new ImageRecordReader(features.getImageHeight(), features.getImageWidth(), 
				features.getFeatureChannels(), new ParentPathLabelGenerator());
		FileSplit fileSplit = new FileSplit(datapath.toFile(), NativeImageLoader.ALLOWED_FORMATS, new Random(123));
		rr.initialize(fileSplit);
		
		DataSetIterator iter = new RecordReaderDataSetIterator(rr, batchSize, 1, labelgen.getClassCount());
		
		return iter;
	}

}
