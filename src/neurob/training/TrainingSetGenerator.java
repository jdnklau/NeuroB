package neurob.training;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.exception.ProBError;
import neurob.training.generators.interfaces.TrainingDataCollector;

/**
 * Class to generate the training data for the neural net.
 * 
 * <p>
 * The constructor takes to parameters:
 * 		<ul>
 * 			<li>An object implementing {@link TrainingDataCollector}, to get desired features, and</li>
 * 			<li>An object implementing {@link TrainingOutputCollector}, to get corresponding output data, the neural net should be trained on.</li>
 * 		</ul>
 * </p>
 * <p>
 * Use {@link TrainingSetGenerator#generateTrainingSet(Path, Path) generateTrainingSet} to generate the test data from a given source directory,
 * that contains .mch files. They will be translated into .nbtrain files, containing the collected data.
 * </p>
 * 
 * @author Jannik Dunkelau
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
	}
	
	/**
	 * 
	 * @return Number of .mch files seen.
	 */
	public int getFileCounter(){ return fileCounter; }
	
	/**
	 * Generates the training data by iterating over all *.mch files in the given source directory 
	 * and generates corresponding *.train.nbdat files in the target directory.
	 * 
	 * The given source directory will be searched recursively with respect to sub-directories.
	 * 
	 * The target directory will mirror the original file hierarchy to simplify the mapping from *.mch to *.nbtrain.
	 * 
	 * @param sourceDirectory Directory from which the machine files are read
	 * @param targetDirectory Directory in which the *.nbtrain files will be put 
	 * 
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory){
		generateTrainingSet(sourceDirectory, targetDirectory, true); // call with default recursion=true
	}
	
	/**
	 * Same as {@link TrainingSetGenerator#generateTrainingDataFile(Path, Path)}, but with the option to turn off the 
	 * recursion step.
	 * @param sourceDirectory
	 * @param targetDirectory
	 * @param recursion If false, the subdirectories are not searched
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory, boolean recursion){
		
		// iterate over directory recursively
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDirectory)) {
			Files.createDirectories(targetDirectory);
			
	        for (Path entry : stream) {

	        	// check if directory or not; recursion if so, else get features from file if .mch
	            if (Files.isDirectory(entry) && recursion) {
	            	Path subdir = entry.getFileName(); // get directory name
	            	
	            	/*
	            	 * TODO:
	            	 * Find better training data. The ProB examples contain a subdirectory called Tickets/ParserPushBackOverflow/, 
	            	 * in which code samples can be found the parser fails to parse, causing everything to just blow up, 
	            	 * as I can not catch the thrown exception properly (although I should..)
	            	 * 
	            	 * For now I simply skip ParserPushBackOverflow/ 
	            	 * 
	            	 * Same with PerformanceTests/
	            	 * and RefinementChecking/
	            	 */
	            	if(subdir.toString().equals("ParserPushBackOverflow")
	            			|| subdir.toString().equals("PerformanceTests")
	            			|| subdir.toString().equals("RefinementChecking")) continue;
	            	
	            	generateTrainingSet(sourceDirectory.resolve(subdir), targetDirectory.resolve(subdir), recursion);
	            }
	            else if(Files.isRegularFile(entry)){
	            	
	            	// check file extension
	            	String fileName = entry.getFileName().toString();
	            	String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
	            	
	            	if(ext.equals("mch")){
	            		Path dataFilePath = targetDirectory.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+".nbtrain");
	            		
	            		generateTrainingDataFile(entry, dataFilePath);
	            		
	            	}
	            	
	            }
	            
	        }
	    }
		catch (IOException e){
			log("Could not access directory "+sourceDirectory+": "+e.getMessage());
		}
		
	}
	
	/**
	 * Generates a file containing feature data found in the source file,
	 * and writes them to the target file.
	 * @param source
	 * @param target
	 */
	public void generateTrainingDataFile(Path source, Path target){
		log("Generating: "+source+" > "+target);
		try {
			tdc.collectTrainingData(source, target);
			log("\tDone: "+target);
		} catch (BException e) {
			log("\tCould not parse "+source+": "+e.getMessage());
		} catch (ProBError e) {
			log("\tProBError on "+source+": "+e.getMessage());
		} catch (IOException e) {
			log("\tCould not access file: "+e.getMessage());
		}
	}
	
	/**
	 * used to add strings to the log file (NFI).
	 * 
	 * For now only uses System.out.println
	 * @param s
	 */
	private void log(String s){
		System.out.println(s);
	}
	
	
}
