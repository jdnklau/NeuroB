package neurob.core.nets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.interfaces.LabelGenerator;

public class NeuroBNet {
	// Network specifics
	protected MultiLayerNetwork model; // Model in use
	protected FeatureGenerator features; // Features in use
	protected LabelGenerator labelgen; // Label generator in use of training set generation
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
			if(labelling.getClassCount()==-1){ // Regression
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
	 * @throws IOException
	 */
	public NeuroBNet(Path modelFile, FeatureGenerator features, LabelGenerator labelling) throws IOException{
		this(features, labelling);
		this.model = ModelSerializer.restoreMultiLayerNetwork(modelFile.toFile());
	}
	
	protected void setUpNormalizer(){
		normalizer = new NormalizerStandardize();
	}
	
	/**
	 * Model your normaliser on the training set
	 * @param data
	 */
	public void fitNormalizer(DataSet data){
		if(useNormalizer)
			normalizer.fit(data);
	}
	
	public void applyNormalizer(DataSet data){
		if(useNormalizer)
			normalizer.transform(data);
	}
	
	public DataNormalization getNormalizer(){
		return normalizer;
	}
	
	/**
	 * Insert data to train your network on
	 * @param data
	 */
	public void fit(DataSet data){
		// normalize data
		if(useNormalizer)
			normalizer.transform(data);
		model.fit(data);
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
		return new TrainingSetGenerator(features, labelgen);
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
	 * Returns an iterator for the given data set.
	 * <p>
	 * It is assumed that the csv file's first row consists of column headers and thus it is ignored.
	 * @param csvFile The data set to use.
	 * @param batchSize
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public DataSetIterator getDataSetIterator(Path csvFile, int batchSize) throws IOException, InterruptedException{
		// set up record reader
		RecordReader recordReader = new CSVRecordReader(1,","); // skip first line (header line)
		recordReader.initialize(new FileSplit(csvFile.toFile()));
		
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
	 * For network training purposes: Start The DL4J UI to monitor the training of the model 
	 */
	public void enableDL4JUI() {
		UIServer uiServer = UIServer.getInstance();
		
		StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		
		model.setListeners(new StatsListener(statsStorage));
	}
	
	public void enableTrainingScoreIteration(int iterationCount){
		model.setListeners(new PerformanceListener(iterationCount, true));
	}

}
