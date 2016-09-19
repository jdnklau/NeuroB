import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import neurob.NeuroB;
import neurob.core.nets.DefaultPredicateSolverPredictionNet;
import neurob.core.nets.interfaces.NeuroBNet;
import neurob.training.TrainingSetAnalyser;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.DefaultTrainingDataCollector;

public class NeuroBCli {
	private static final Path libraryIOpath = Paths.get("prob_examples/LibraryIO.def");
	private static NeuroB nb;

	public static void main(String[] args) {
		// set up net to use
		nb = new NeuroB(
				new DefaultPredicateSolverPredictionNet()
				.setSeed(0L)
				.build()
			);
		
		// default start
		if(args.length < 1){
			NeuroBCli.trainingSetGeneration();
		}
		
		// check command
		else if(args[0].equals("help")){
			// TODO: write a help text
			System.out.println("TODO: write a help text");
		}
		
		// Distribute the LibraryIO.def
		else if(args[0].equals("libraryIODef")) {
			if(args.length < 2) {
				System.out.println("libraryIODef needs a second argument, the directory path into which the file shall be distributed");
			}
			else {
				NeuroBCli.distribute(Paths.get(args[1]));
			}
			
		}
		
		// generate or analyse training set
		else if(args[0].equals("trainingset")) {
			if (args.length > 2) {
				if(args[1].equals("-analyse")){
					NeuroBCli.analyseTrainingSet(Paths.get(args[2]));
				}
				// generate csv from nbtrain files
				else if(args[1].equals("-csv")){
					NeuroBCli.trainingCSVGeneration(Paths.get(args[2]));
				}
			}
			else {
				NeuroBCli.trainingSetGeneration();
			}
		}
		
		// generate a single nbtrain file
		else if(args[0].equals("trainingfile")) {
			if(args.length < 2) {
				System.out.println("trainingfile needs a second argument, the path to the file in question");
			}
			else {
				NeuroBCli.singleTrainingDataGeneration(Paths.get(args[1]));
			}
		}
		
		

	}
	
	private static void distribute(Path directory){
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
			
	        for (Path entry : stream) {

	        	// check if directory or not; recursion if so
	            if (Files.isDirectory(entry)) {
	            	
	            	Path newLibraryIOPath = entry.resolve("LibraryIO.def");
	            	try{
	            		Files.copy(libraryIOpath, newLibraryIOPath);
	            		System.out.println("Created: "+newLibraryIOPath);
	            	} catch (IOException e){
	            		System.out.println("NOT created (maybe already existing) "+newLibraryIOPath+": "+e.getMessage());
	            	}
	            	
	            	distribute(entry); //  distribute the file recusively
	            	
	            }
	            
	        }
	    }
		catch (IOException e){
			System.out.println("Could not access directory "+directory+": "+e.getMessage());
		}
	}
	
	private static void singleTrainingDataGeneration(Path source){
		TrainingSetGenerator tsg = new TrainingSetGenerator(new DefaultTrainingDataCollector());
		
		Path targetDir = Paths.get("training_data/manual_call/public_examples/B/");
		
		String fileName = source.getFileName().toString();
		Path target = source.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+".nbtrain");
		tsg.generateTrainingDataFile(source, target);
	}
	
	private static void trainingSetGeneration(){
		Path sourceDir = Paths.get("prob_examples/public_examples/B/");
		Path targetDir = Paths.get("training_data/");
		
		nb.generateTrainingSet(sourceDir, targetDir);
	}
	
	private static void analyseTrainingSet(Path dir){
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.analyseTrainingSet(dir);
		System.out.println(tsa.getStatistics());
	}
	
	private static void trainingCSVGeneration(Path dir){
		TrainingSetGenerator tsg = new TrainingSetGenerator(new DefaultTrainingDataCollector());
		
		Path target = Paths.get("training_data/manual_call/data.csv");
		
		tsg.generateCSVFromNBTrainData(dir, target);
	}

}
