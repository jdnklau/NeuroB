import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import neurob.training.TrainingSetAnalyser;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.DefaultTrainingDataCollector;

public class NeuroBCli {
	private static final Path libraryIOpath = Paths.get("prob_examples/LibraryIO.def");

	public static void main(String[] args) {
		// check command
		if(args[0].equals("help")){
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
		TrainingSetGenerator tsg = new TrainingSetGenerator(new DefaultTrainingDataCollector());
		
		Path sourceDir = Paths.get("prob_examples/public_examples/B/");
		Path targetDir = Paths.get("training_data/manual_call/public_examples/B/");
		
		tsg.generateTrainingSet(sourceDir, targetDir);
		
//		try {
//			Path dirlist = Paths.get("prob_examples/directories.txt");
//			for( String line : Files.readAllLines(dirlist)){
//					Path src = Paths.get("prob_examples/reduced_examples/"+line);
//					Path tar = Paths.get("prob_examples/training_data/public_examples/"+line);
//					
//					tsg.generateTrainingSet(src, tar, false);
//					
//					// Mark directory as seen/delete line from file
//					String content = new String(Files.readAllBytes(dirlist));
//					content = content.substring(line.length()+1); // cut this line and the \n
//					Files.write(dirlist, content.getBytes());
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		Path sourceDir = Paths.get("prob_examples/public_examples/B/Tickets/Hansen23_WhilePerformance/WhileSlow_CartProduct.mch");
//		Path targetDir = Paths.get("prob_examples/training_data/public_examples/B/Tickets/Hansen23_WhilePerformance/WhileSlow_CartProduct.nbtrain");
//		
//		tsg.generateTrainingDataFile(sourceDir, targetDir);
		
		tsg.logStatistics();
		tsg.logTrainingSetAnalysis(targetDir);
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
