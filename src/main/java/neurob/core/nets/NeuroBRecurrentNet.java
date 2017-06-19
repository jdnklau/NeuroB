package neurob.core.nets;

import neurob.core.features.interfaces.RNNFeatures;
import neurob.core.util.ProblemType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

/**
 * @author Jannik Dunkelau
 */
public class NeuroBRecurrentNet extends NeuroBNet {

	public NeuroBRecurrentNet(MultiLayerNetwork model, RNNFeatures features, LabelGenerator labelling) {
		super(model, features, labelling);
	}

	public NeuroBRecurrentNet(int[] hiddenLayers, double learningRate, RNNFeatures features, LabelGenerator labelling) {
		this(hiddenLayers, learningRate, features, labelling, new Random().nextInt());
	}

	/**
	 * Creates a recurrent neural network (RNN) with given structure.
	 * <p>
	 *     {@code hiddenLayers} parameter gives structure of the RNN. For each entry in the array
	 *     a GravesLSTM layer will be added, with size corresponding to the entry's value.
	 * </p>
	 * @param hiddenLayers Structure to build
	 * @param learningRate learning rate to be used
	 * @param features FeatureGenerator to use
	 * @param labelling LabelGenerator to use
	 * @param seed seed fed to the RNG
	 */
	public NeuroBRecurrentNet(int[] hiddenLayers, double learningRate, RNNFeatures features, LabelGenerator labelling, int seed) {
		super(features, labelling);

		this.seed = seed;
		int tbpttLength = 50;

		NeuralNetConfiguration.ListBuilder listBuilder = new NeuralNetConfiguration.Builder()
				.seed(seed)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.iterations(1)
				.learningRate(learningRate)
//				.updater(Updater.NESTEROVS).momentum(0.9)
				.updater(Updater.RMSPROP).rmsDecay(0.95)
				.weightInit(WeightInit.XAVIER)
				.regularization(true).l2(1e-4)
				.list();

		// set up layers
		if(hiddenLayers.length < 1) {
			// no hidden layers
			throw new IllegalArgumentException("NeuroBRecurrentNet needs to have at least one " +
					"entry for the hidden layer sizes");
		}

		int lastOut = features.getFeatureDimension();
		for (int i = 0; i < hiddenLayers.length; i++) {
			listBuilder = listBuilder.layer(i, new GravesLSTM.Builder()
					.nIn(lastOut)
					.nOut(hiddenLayers[i])
					.activation(Activation.TANH)
					.build());
			lastOut = hiddenLayers[i];
		}

		// Output layer - depending on whether we do regression or not
		LossFunctions.LossFunction lossFunction;
		Activation activationFunction;
		if(labelling.getProblemType() == ProblemType.REGRESSION){ // Regression
			lossFunction = LossFunctions.LossFunction.MSE;
			activationFunction = Activation.IDENTITY;
		}
		else { // No regression
			lossFunction = LossFunctions.LossFunction.MCXENT;
			activationFunction = Activation.SOFTMAX;
		}
		// the layer itself
		listBuilder = listBuilder.layer(hiddenLayers.length, new RnnOutputLayer.Builder(lossFunction)
				.nIn(lastOut)
				.nOut(labelling.getLabelDimension())
				.activation(activationFunction)
				.build());

		// normaliser
		useNormalizer = true;
		setUpNormalizer();

		// Model
		MultiLayerConfiguration conf = listBuilder
				.backpropType(BackpropType.TruncatedBPTT)
					.tBPTTForwardLength(tbpttLength)
					.tBPTTBackwardLength(tbpttLength)
				.pretrain(false).backprop(true)
				.build();
		model = new MultiLayerNetwork(conf);
		model.init();
	}

	public NeuroBRecurrentNet(Path modelDirectory, RNNFeatures features, LabelGenerator labelling) throws NeuroBException {
		super(modelDirectory, features, labelling);
	}

	@Override
	public DataSetIterator getDataSetIterator(Path dataSet, int batchSize) throws IOException, InterruptedException {
		SequenceRecordReader featureReader = (SequenceRecordReader) features.getRecordReader(dataSet.resolve("features"), batchSize);
		SequenceRecordReader labelReader = (SequenceRecordReader) features.getRecordReader(dataSet.resolve("labels"), batchSize);

		return new SequenceRecordReaderDataSetIterator(featureReader, labelReader, batchSize,
				labelgen.getClassCount(), labelgen.getProblemType() == ProblemType.REGRESSION,
				SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
	}
}
