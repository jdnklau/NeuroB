import java.nio.file.Path;
import java.nio.file.Paths;

import neurob.training.TrainingSetGenerator;
import neurob.training.generators.DefaultTrainingDataCollector;

public class TrainingCSVGeneration {

	public static void main(String[] args) {
		TrainingSetGenerator tsg = new TrainingSetGenerator(new DefaultTrainingDataCollector());
		
		// Path sourceDir = Paths.get("prob_examples/public_examples/B/");
		Path targetDir = Paths.get("prob_examples/training_data/public_examples/B/");
		
		tsg.generateCSVFromNBTrainData(targetDir, Paths.get("prob_examples/training_data/data.csv"));

	}

}
