package neurob.training.generators;

import neurob.core.features.interfaces.RNNFeatures;
import neurob.exceptions.NeuroBException;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.TrainingData;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * @author Jannik Dunkelau
 */
public class PredicateTrainingSequenceCSVGenerator extends PredicateTrainingDataGenerator{

	public PredicateTrainingSequenceCSVGenerator(
			RNNFeatures featureGenerator, LabelGenerator labelGenerator){
		super(featureGenerator, labelGenerator);
	}

	@Override
	public void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir) throws NeuroBException {
		// Set up training data file
		Path sourceFile;
		Path trainingFile;
		int counter = 0; // count how many samples are already created

		for(TrainingData td : trainingData){
			sourceFile = td.getSource();
			trainingFile = generateTrainingDataPath(sourceFile, targetDir).resolve(counter+".csv");
			generateTrainingCSV(td, trainingFile);
		}
	}

	private void generateTrainingCSV(TrainingData td, Path trainingFile) {
		// TODO
	}

	@Override
	public Path generateTrainingDataPath(Path sourceFile, Path targetDir) {
		// get source file name without file extension
		return targetDir.resolve(sourceFile.toString()+".image_dir");
	}

	@Override
	public void splitTrainingData(Path source, Path first, Path second, double ratio, Random rng) throws NeuroBException {

	}

	@Override
	public void trimTrainingData(Path source, Path target) throws NeuroBException {

	}

	@Override
	protected TrainingAnalysisData analyseTrainingFile(Path file, TrainingAnalysisData analysisData) {
		return null;
	}

	@Override
	public double[] labellingFromSample(String sample) {
		return new double[0];
	}
}
