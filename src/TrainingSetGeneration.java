
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import neurob.training.TrainingSetGenerator;
import neurob.training.generators.DefaultTrainingDataCollector;

public class TrainingSetGeneration {

	public static void main(String[] args) {
		
		System.out.println("Beginning to generate training set...");
		
		TrainingSetGenerator tsg = new TrainingSetGenerator(new DefaultTrainingDataCollector());
		
//		Path sourceDir = Paths.get("prob_examples/reduced_examples/B/Tickets");
//		Path targetDir = Paths.get("prob_examples/training_data/public_examples/B/Tickets");
//		
//		tsg.generateTrainingSet(sourceDir, targetDir);
		
		try {
			Path dirlist = Paths.get("prob_examples/directories.txt");
			for( String line : Files.readAllLines(dirlist)){
					Path src = Paths.get("prob_examples/reduced_examples/"+line);
					Path tar = Paths.get("prob_examples/training_data/public_examples");
					
					tsg.generateTrainingSet(src, tar, false);
					
					// Mark directory as seen/delete line from file
					String content = new String(Files.readAllBytes(dirlist));
					content = content.substring(line.length()+1); // cut this line and the \n
					Files.write(dirlist, content.getBytes());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		Path sourceDir = Paths.get("prob_examples/public_examples/B/Tickets/Hansen23_WhilePerformance/WhileSlow_CartProduct.mch");
//		Path targetDir = Paths.get("prob_examples/training_data/public_examples/B/Tickets/Hansen23_WhilePerformance/WhileSlow_CartProduct.nbtrain");
//		
//		tsg.generateTrainingDataFile(sourceDir, targetDir);
		
		System.out.println("... finished generation of training set.");
		System.out.println("Visited "+tsg.getFileCounter()+" files.");
	}

}
