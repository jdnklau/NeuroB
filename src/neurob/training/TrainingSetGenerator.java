package neurob.training;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import neurob.training.generators.interfaces.IFeatureCollector;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class TrainingSetGenerator {
	
	/**
	 * 
	 */
	public TrainingSetGenerator() {
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
	        	// check if directory or not; recursion if so, else get features from file if .mch
	            if (Files.isDirectory(entry)) {
	            	Path subdir = sourceDirectory.relativize(entry); // get /subdir from /sourceDirctory/subdir
	            	generateTrainingSet(sourceDirectory.resolve(subdir), targetDirectory.resolve(subdir));
	            }
	            
	        }
	    }
		catch (IOException e){
			e.printStackTrace();
		}
		
		
	}

}
