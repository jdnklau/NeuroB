package neurob.core.nets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Random;

import org.datavec.api.records.reader.RecordReader;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.ROC;
import org.deeplearning4j.eval.ROCMultiClass;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.ProblemType;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.interfaces.LabelGenerator;

public class NeuroBNet {
	// Network specifics
	protected MultiLayerNetwork model; // Model in use
	protected FeatureGenerator features; // Features in use
	protected LabelGenerator labelgen; // Label generator in use of training set generation
	protected int seed;
	// Preprocessing
	protected DataNormalization normalizer; // Normalizer used
	protected boolean useNormalizer;

	/**
	 * Creates a NeuroBNet without any model attached to it.
	 * This is useful for e.g. the training set generation, when no model is needed
	 * but the feature generator and label generator alone.
	 * @param features
	 * @param labelling
	 */
	public NeuroBNet(FeatureGenerator features, LabelGenerator labelling){
		this.features = features;
		this.labelgen = labelling;
		useNormalizer = false;
	}

	/**
	 * Set up your deeplearning4j {@link MultiLayerNetwork} and use it as NeuroB class
	 * @param model The model to use
	 * @param features The feature class to use
	 * @param labelling The classification approach used --- necessary for training set generation
	 */
	public NeuroBNet(MultiLayerNetwork model, FeatureGenerator features, LabelGenerator labelling) {
		this(features, labelling);
		this.model = model;
		this.model.init();
		useNormalizer = true;
		setUpNormalizer();
	}

	/**
	 * Creates a neural network with given structure and a random seed.
	 * <p>
	 * See {@link #NeuroBNet(int[], double, FeatureGenerator, LabelGenerator, int)} for more details, as this only
	 * generates a random seed an call that constructor instead.
	 * @param hiddenLayers
	 * @param learningRate
	 * @param features
	 * @param labelling
	 */
	public NeuroBNet(int[] hiddenLayers, double learningRate, FeatureGenerator features, LabelGenerator labelling) {
		this(hiddenLayers, learningRate, features, labelling, new Random().nextInt());

	}

	/**
	 * Creates a neural network with given structure.
	 * <p>
	 * {@code hiddenLayers} parameter gives structure of the neural net. For each entry in the array, a hidden layer with the entry's value of neurons will be created.
	 * They are stacked onto each other according to the index in the array.
	 * <br>
	 * For example:
	 * {1000, 500, 200} would create a neural net with three hidden layers. The first one having 1000 neurons, the second one having 500 neurons,
	 * and the third one having 200 neurons.
	 * <br>
	 * The network has to have at least one hidden layer.
	 * @param hiddenLayers Structure to be build
	 * @param learningRate Learning rate to be used
	 * @param features
	 * @param labelling
	 * @param seed
	 */
	public NeuroBNet(int[] hiddenLayers, double learningRate, FeatureGenerator features, LabelGenerator labelling, int seed) {
		this(features, labelling);

		// save seed
		this.seed = seed;

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

		useNormalizer = true;
		setUpNormalizer();

		this.model = new MultiLayerNetwork(listBuilder.build());
		model.init();
	}

	/**
	 * Load an already existing {@link MultiLayerNetwork} from file and use it
	 * @param modelFile Path to the model file
	 * @param features
	 * @param labelling
	 * @throws NeuroBException
	 */
	public NeuroBNet(Path modelDirectory, FeatureGenerator features, LabelGenerator labelling) throws NeuroBException{
		this(features, labelling);
		try {
			this.model = ModelSerializer
					.restoreMultiLayerNetwork(modelDirectory.resolve("model.zip").toFile());
			this.normalizer = NormalizerSerializer.getDefault()
					.restore(modelDirectory.resolve("normalizer").toFile());
			// read seed
			BufferedReader seedReader =
					Files.newBufferedReader(modelDirectory.resolve("seed.txt"));
			this.seed = (int) model.getDefaultConfiguration().getSeed();
		} catch (Exception e) {
			throw new NeuroBException("Could not correctly load model located in "
				+ modelDirectory, e);
		}
	}

	public MultiLayerNetwork getModel(){
		return model;
	}

	/**
	 * Saves the NeuroBNet to disc.
	 * <p>
	 * In the given target directory, up to three files will be created.
	 * <ul>
	 * <li><b>model.zip</b> hold the model
	 * <li><b>normalizer</b> hold the normalizer; only created if {@link #isNormalizerUsed()}
	 * <li><b>seed.txt</b> holds the seed the model was initialised with
	 * </ul>
	 * <p>
	 * After reloading, the model can further be trained
	 * @param targetDirectory Directory to save the files to
	 * @throws IOException
	 */
	public void saveModel(Path targetDirectory) throws IOException{
		saveModel(targetDirectory, true);
	}

	/**
	 * Saves the NeuroBNet to disc.
	 * <p>
	 * In the given target directory, up to three files will be created.
	 * <ul>
	 * <li><b>model.zip</b> hold the model
	 * <li><b>normalizer</b> hold the normalizer; only created if {@link #isNormalizerUsed()}
	 * <li><b>seed.txt</b> holds the seed the model was initialised with
	 * </ul>
	 * @param targetDirectory Directory to save the files to
	 * @param saveUpdater set to true if you may want to continue training the model
	 * @throws IOException
	 */
	public void saveModel(Path targetDirectory, boolean saveUpdater) throws IOException{
		// make sure the directory exists
		Files.createDirectories(targetDirectory);
		// save normalizer
		if(useNormalizer){
			NormalizerSerializer normserializer = NormalizerSerializer.getDefault();
			normserializer.write(normalizer,
					targetDirectory.resolve("normalizer").toFile());
		}
		// save model
		ModelSerializer.writeModel(model,
				targetDirectory.resolve("model.zip").toFile(), saveUpdater);
		// save seed
		BufferedWriter seedWr = Files.newBufferedWriter(targetDirectory.resolve("seed.txt"));
		seedWr.write(seed);
		seedWr.newLine();
		seedWr.close();
	}

	protected void setUpNormalizer(){
		normalizer = features.getNewNormalizer();
	}

	/**
	 * Model your normaliser on the training set
	 * @param data
	 * @deprecated use {@link #fitNormalizer(DataSetIterator)} instead
	 */
	@Deprecated
	public void fitNormalizer(DataSet data){
		if(useNormalizer)
			normalizer.fit(data);
	}

	/**
	 * Fits the normalizer over the data set iterator and sets it as preprocessor.
	 * Does nothing if no normalizer is used.
	 * @param iterator
	 */
	public void fitNormalizer(DataSetIterator iterator){
		if(useNormalizer){
			normalizer.fit(iterator);
			iterator.setPreProcessor(normalizer);
		}

	}

	/**
	 * Normalises the data with the trained normalizer, or does nothing if no normalizer is used.
	 */
	public void applyNormalizer(DataSet data){
		if(useNormalizer)
			normalizer.transform(data);
	}

	/**
	 * Sets the normalizer trained for this model as preprocessor to the given data set iterator.
	 * If no normalizer is used, does nothing.
	 * @param iterator
	 */
	public void applyNormalizer(DataSetIterator iterator){
		if(useNormalizer)
			iterator.setPreProcessor(normalizer);
	}

	public DataNormalization getNormalizer(){
		return normalizer;
	}

	/**
	 * Insert data to train your network on
	 * @param data
	 * @see #fit(DataSetIterator)
	 */
	public void fit(DataSet data){
		// normalize data
		if(useNormalizer)
			normalizer.transform(data);
		model.fit(data);
	}

	/**
	 * Trains for one epoch over the data set.
	 * @param iterator
	 * @see #fit(DataSet)
	 */
	public void fit(DataSetIterator iterator){
		model.fit(iterator);
	}

	/**
	 * Input predicate to the model and get prediction.
	 * @param predicate
	 * @return
	 * @throws NeuroBException
	 */
	public INDArray output(String predicate) throws NeuroBException{
		return output(features.generateFeatureNDArray(predicate));
	}

	public INDArray output(INDArray dataArray) {
		return model.output(dataArray, false);
	}

	/**
	 * Returns a {@link TrainingSetGenerator} instance, that can be used to generate a training set for this network.
	 * @return
	 */
	public TrainingSetGenerator getTrainingSetGenerator(){
		return new TrainingSetGenerator(features.getTrainingDataGenerator(labelgen));
	}

	public int getInputSize(){
		return features.getFeatureDimension();
	}

	public int getOutputSize(){
		return labelgen.getLabelDimension();
	}

	/**
	 *
	 * @return Number of classes the net differentiates
	 */
	public int getClassificationSize(){
		return labelgen.getClassCount();
	}

	/**
	 * Returns an iterator for the given data set to use for training a model.
	 * @param dataSet The data set to use.
	 * @param batchSize Size of samples read at once per batch
	 * @return An iterator to go over thr data set
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public DataSetIterator getDataSetIterator(Path dataSet, int batchSize) throws IOException, InterruptedException{
		// set up record reader
		RecordReader recordReader = features.getRecordReader(dataSet, batchSize);

		return labelgen.getDataSetIterator(recordReader, batchSize, features.getFeatureDimension());
	}

	/**
	 * Returns a string, representing an identifying path with respect to the feature and label generation used.
	 * <p>
	 * This is intended to be used e.g. in the training data creation, to store different data sets more easily at
	 * semantically fitting locations.
	 * @return String containing Neural Net class name, label generator class name, feature generator class name, separated and followed by '/', in this order
	 */
	public String getDataPathName(){
		return this.getClass().getSimpleName()
				+"/" + labelgen.getDataPathIdentifier()
				+"/" +features.getDataPathIdentifier()
				+"/";
	}

	/**
	 * Sets and overrides the listeners attached to the dl4j model.
	 * @param listeners
	 */
	public void setListeners(IterationListener... listeners){
		model.setListeners(listeners);
	}

	/**
	 * Sets and overrides the listeners attached to the dl4j model.
	 * @param listeners
	 */
	public void setListeners(Collection<IterationListener> listeners){
		model.setListeners(listeners);
	}

	public boolean isNormalizerUsed(){return useNormalizer;}

	public ProblemType getProblemType(){ return labelgen.getProblemType();}

	/**
	 * Returns evaluation for classification problems.
	 * @param dataSet Iterator over data set to evaluate on
	 * @return Evaluation object containing relevant data
	 * @see Evaluation
	 */
	public Evaluation evaluate(DataSetIterator dataSet){
		return model.evaluate(dataSet);
	}

	/**
	 * Returns evaluation for regression problems.
	 * @param dataSet Iterator over data set to evaluate on
	 * @return RegressionEvaluation object containing relevant data
	 * @see RegressionEvaluation
	 */
	public RegressionEvaluation evaluateRegression(DataSetIterator dataSet){
		return model.evaluateRegression(dataSet);
	}

	/**
	 * Returns ROC for binary classification problems.
	 * @param dataSet Iterator over data set to evaluate on
	 * @param thresholdSteps
	 * @return ROC object containing evaluated data
	 * @see ROC
	 */
	public ROC evaluateROC(DataSetIterator dataSet, int thresholdSteps){
		return model.evaluateROC(dataSet, thresholdSteps);
	}

	/**
	 * Returns ROC for multi-classification problems.
	 * @param dataSet Iterator over data set to evaluate on
	 * @param thresholdSteps
	 * @return ROCMultiClass object containing evaluated data
	 */
	public ROCMultiClass evaluateRegression(DataSetIterator dataSet, int thresholdSteps){
		return model.evaluateROCMultiClass(dataSet, thresholdSteps);
	}

}