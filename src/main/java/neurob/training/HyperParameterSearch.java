package neurob.training;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.core.features.interfaces.RNNFeatures;
import neurob.core.nets.NeuroBConvNet;
import neurob.core.nets.NeuroBRecurrentNet;
import org.deeplearning4j.arbiter.DL4JConfiguration;
import org.deeplearning4j.arbiter.optimize.api.CandidateGenerator;
import org.deeplearning4j.eval.IEvaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import neurob.core.NeuroB;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.nets.NeuroBNet;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.statistics.interfaces.ModelEvaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HyperParameterSearch <T extends CandidateGenerator<DL4JConfiguration>> {
	private T candidateGenerator;
	private Path savePath;
	private int batchSize;
	private int bestPerformingIndex;
	@SuppressWarnings("rawtypes")
	private ModelEvaluation bestEvaluation = null;
	private FeatureGenerator featureGenerator;
	private LabelGenerator labelGenerator;
	private static final Logger log = LoggerFactory.getLogger(HyperParameterSearch.class);

	public HyperParameterSearch(T candidateGenerator,
			FeatureGenerator featureGen, LabelGenerator labelGen) {
		this(candidateGenerator,
				featureGen, labelGen,
				Paths.get("trained_models/hyper_parameter_search/")
				.resolve(ZonedDateTime.now()
						.format(DateTimeFormatter.ISO_INSTANT))
				);
	}

	public HyperParameterSearch(T candidateGenerator,
			FeatureGenerator featureGen, LabelGenerator labelGen,
			Path modelDirectory){
		this(candidateGenerator,
				featureGen, labelGen, 250,
				modelDirectory);
	}

	public HyperParameterSearch(T candidateGenerator,
			FeatureGenerator featureGen, LabelGenerator labelGen, int batchSize,
			Path modelDirectory){
		this.candidateGenerator = candidateGenerator;
		savePath = modelDirectory;
		bestPerformingIndex = -1;

		featureGenerator = featureGen;
		labelGenerator = labelGen;

		this.batchSize = batchSize;
	}

	/**
	 * Trains multiple models, saves them to disk and reports
	 * the index of the best performing model.
	 * <p>
	 * In the target directory, a subdirectory will be created for each model.
	 * Those subdirectories are enumerated from 0 onwards, being the model
	 * index.
	 * <p>
	 * It is advised to use a relatively small epoch number, as hyperparameter
	 * search is only meant to find hyperparameter settings to use for
	 * following, longer training.
	 *
	 * @param numModels Maximum number of models to train
	 * @param trainSource Location of training set
	 * @param testSource Location of test set
	 * @param numEpochs Number of epochs to train each model
	 * @return index of best performing model
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public int trainModels(int numModels,
			Path trainSource, Path testSource, int numEpochs)
			throws IOException, InterruptedException{
		// Train and evaluate up to numModels candidates
		for(int i=0;
				i<numModels && candidateGenerator.hasMoreCandidates();
				i++){
			// get candidate
			MultiLayerNetwork model = new MultiLayerNetwork(
					candidateGenerator.getCandidate()
					.getValue().getMultiLayerConfiguration());
			// make neuroB net instance
			NeuroBNet nbn = setUpModel(model);
			Path modelSavePath =
					savePath.resolve(nbn.getDataPathName())
					.resolve(Integer.toString(i));

			// set up NeuroB
			NeuroB nb = new NeuroB(nbn, modelSavePath);

			// train model and get evaluation
			ModelEvaluation eval = nb.train(trainSource,testSource, batchSize,
					numEpochs, true, 6);

			// compare evaluations
			if(bestEvaluation == null || eval.performsBetterThan(bestEvaluation)){
				bestEvaluation = eval;
				log.info("New model #{} performed best; previous best was #{}", i, bestPerformingIndex);
				bestPerformingIndex = i;
			}
		}

		log.info("Best model generated has index {}", bestPerformingIndex);
		return bestPerformingIndex;
	}


	public int getBestPerformingIndex() {
		return bestPerformingIndex;
	}

	@SuppressWarnings("rawtypes")
	public ModelEvaluation getBestEvaluation() {
		return bestEvaluation;
	}

	/**
	 * Decides which NeuroBNet to set up depending on feature generator in use
	 * @param model
	 * @return
	 */
	private NeuroBNet setUpModel(MultiLayerNetwork model) {
		if(featureGenerator instanceof RNNFeatures){
			return new NeuroBRecurrentNet(model, (RNNFeatures) featureGenerator, labelGenerator);
		} else if(featureGenerator instanceof ConvolutionFeatures){
			return new NeuroBConvNet(model, (ConvolutionFeatures) featureGenerator, labelGenerator);
		} else {
			return new NeuroBNet(model, featureGenerator, labelGenerator);
		}
	}
}
