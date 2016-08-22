package neurob.training;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import neurob.training.generators.interfaces.TrainingDataCollector;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class TrainingSetGenerator {
	private TrainingDataCollector tdc; // used collector of training data
	private int limit; // only this much files are generated (or looked into in th first place)
	private int fileCounter; // number of files generated
	
	/**
	 * 
	 * @param trainingDataCollector Instance of the training collection to be used
	 */
	public TrainingSetGenerator(TrainingDataCollector trainingDataCollector) {
		tdc = trainingDataCollector;
		limit = 5000;
		fileCounter = 0;
	}
	
	/**
	 * Generates the training data by iterating over all *.mch files in the given source directory 
	 * and generates corresponding *.train.nbdat files in the target directory.
	 * 
	 * The given source directory will be searched recursively with respect to sub-directories.
	 * 
	 * The target directory will mirror the original file hierarchy to simplify the mapping from *.mch to *.train.nbdat.
	 * 
	 * @param sourceDirectory Directory from which the machine files are read
	 * @param targetDirectory Directory in which the *.train.nbdat files will be put 
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory){
		
		// iterate over directory recursively
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDirectory)) {
			Files.createDirectories(targetDirectory);
			
	        for (Path entry : stream) {
	        	if(fileCounter >= limit){
	        		System.out.println("Reached limit of "+limit+" files.");	
	        		return;
	        	}

	        	// check if directory or not; recursion if so, else get features from file if .mch
	            if (Files.isDirectory(entry)) {
	            	Path subdir = entry.getFileName(); // get /subdir from /sourceDirctory/subdir
	            	
	            	/*
	            	 * TODO:
	            	 * Find better training data. The ProB examples contain a subdirectory called Tickets/ParserPushBackOverflow/, 
	            	 * in which code samples can be found the parser fails to parse, causing everything to just blow up, 
	            	 * as I can not catch the thrown exception properly (although I should..)
	            	 * 
	            	 * For now I simply skip ParserPushBackOverflow/ 
	            	 * 
	            	 * Same with PerformanceTests/
	            	 */
	            	if(subdir.toString().equals("ParserPushBackOverflow")
	            			|| subdir.toString().equals("PerformanceTests")) continue;
	            	
	            	generateTrainingSet(sourceDirectory.resolve(subdir), targetDirectory.resolve(subdir));
	            }
	            else if(Files.isRegularFile(entry)){
	            	fileCounter++;
	            	
	            	// check file extension
	            	String fileName = entry.getFileName().toString();
	            	String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
	            	
	            	if(ext.equals("mch")){
	            		Path dataFilePath = targetDirectory.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+".train.nbdat");
	            		
	            		try{
	            			tdc.collectTrainingData(entry, dataFilePath);
	            			System.out.println("Generated: "+dataFilePath); // TODO: delete
	            		}
	            		catch (IOException e) {
	            			System.out.println("Could not create "+dataFilePath+": "+e.getMessage());
	            		}
	            		
	            	}
	            	
	            }
	            
	        }
	    }
		catch (IOException e){
			e.printStackTrace();
		}
	}
		
	public int getFileCounter(){ return fileCounter; }

}
