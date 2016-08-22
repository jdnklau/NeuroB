
import java.nio.file.Path;
import java.nio.file.Paths;

import neurob.training.TrainingSetGenerator;

public class TrainingSetGeneration {

	public static void main(String[] args) {
		
		System.out.println("Beginning to generate training set...");
		
		TrainingSetGenerator tsg = new TrainingSetGenerator();
		
		Path sourceDir = Paths.get("prob_examples/public_examples/B/");
		Path targetDir = Paths.get("prob_examples/training_data/public_examples/B/");
		
		tsg.generateTrainingSet(sourceDir, targetDir);
		
		System.out.println("... finished generation of training set.");
	}

}
