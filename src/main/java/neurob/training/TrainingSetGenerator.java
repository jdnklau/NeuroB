package neurob.training;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.exception.ProBError;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.interfaces.LabelGenerator;

public class TrainingSetGenerator {
	// Training data handling
	private FeatureGenerator fg; // Feature generator in use
	private LabelGenerator lg; // Label generator in use
	private TrainingDataGenerator tdg; // Training data generator in use
	// statistics
	private int fileCounter; // number of files seen
	private int fileProblemsCounter; // number of files which caused problems
	// logger
	private static final Logger log = LoggerFactory.getLogger(TrainingSetGenerator.class);
	
	public TrainingSetGenerator(TrainingDataGenerator dataGenerator){
		fg = dataGenerator.getFeatureGenerator();
		lg = dataGenerator.getLabelGenerator();
		
		tdg = dataGenerator;
		
		fileCounter = 0;
		fileProblemsCounter = 0;
	}
	
	/**
	 * Adds an overview of the seen files to the log
	 */
	public void logStatistics(){
		log.info("Summary of training set generation:");
		log.info("Seen:\t{} files", fileCounter);
		log.info("\t{} caused problems and could not be properly processed", fileProblemsCounter);
		log.info("*****************************");
	}
	
	public void logTrainingSetAnalysis(Path dir) throws IOException{
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.analyseNBTrainSet(dir, lg).log();
	}
	
	public void logTrainingCSVAnalysis(Path csv) throws IOException{
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.analyseTrainingCSV(csv, lg).log();
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
		generateTrainingSet(sourceDirectory, targetDirectory, null, true); // call with default recursion=true
	}
	
	/**
	 * Same as {@link #generateTrainingSet(Path, Path)}, but with the option to turn of the search in sub-directories.
	 * @param sourceDirectory
	 * @param targetDirectory
	 * @param recursion
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory, boolean recursion){
		generateTrainingSet(sourceDirectory, targetDirectory, null, recursion);
	}
	
	/**
	 * Same as {@link #generateTrainingSet(Path, Path)}, but with the option to specify an exclude file.
	 * @param sourceDirectory
	 * @param targetDirectory
	 * @param excludeFile
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory, Path excludeFile){
		generateTrainingSet(sourceDirectory, targetDirectory, excludeFile, true);
	}
	
	/**
	 * Same as {@link TrainingSetGenerator#generateTrainingDataFromFile(Path, Path)}, but with the option to specify an exclude file and
	 * to turn off the recursion step.
	 * @param sourceDirectory
	 * @param targetDirectory
	 * @param excludeFile
	 * @param recursion If false, the sub-directories are not searched
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory, Path excludeFile, boolean recursion){
		log.info("Generating training set from {} in {}", sourceDirectory, targetDirectory);
		// prepare exclude data
		List<Path> excludes = new ArrayList<Path>();
		if(excludeFile != null){
//			Path excludeFileDirectory = excludeFile.getParent();
			try(Stream<String> exc = Files.lines(excludeFile)){
				excludes.addAll(
						(List<Path>) exc
							.filter(s -> !s.isEmpty())
							.map(s -> Paths.get(s)).collect(Collectors.toList()));
			} catch (IOException e) {
				log.error("Could not access exclude file: {}", e.getMessage(), e);
			}
		}
		
		// iterate over directory
		int depth = (recursion) ? Integer.MAX_VALUE : 1;
		try (Stream<Path> stream = Files.walk(sourceDirectory, depth)) {
			Files.createDirectories(targetDirectory);
			
			stream
				.parallel() // parallel computation
				.filter(p -> !excludes.stream().anyMatch(ex -> p.startsWith(ex))) // no excluded files or directories
				.forEach(entry -> {
	            	if(Files.isRegularFile(entry)){
						generateTrainingDataFromFile(entry, targetDirectory);
		            }
				});
			log.info("Finished training set generation");
			log.info("******************************");
	    }
		catch (IOException e){
			log.error("Could not access directory {}: {}", sourceDirectory, e.getMessage(), e);
		}
		
	}
	
	/**
	 * Generates a file containing feature data found in the source file,
	 * and writes them to the target file.
	 * @param source
	 * @param target
	 */
	public void generateTrainingDataFromFile(Path source, Path target){
		
		// create file
		try {
			tdg.generateTrainingDataFromFile(source, target);
			return;
		} catch (ProBError e) {
			log.error("\tProBError on {}: {}", source, e.getMessage(), e);
		} catch (IllegalStateException e) {
			log.error("\tReached illegal state while processing: {}", e.getMessage(), e);
		} catch (NeuroBException e) {
			log.error("\t{}", e.getMessage(), e);
		} catch (IOException e) {
			log.error("\tCould not access source file {} correctly: {}", source, e.getMessage(), e);
		}
		log.info("\tStopped with errors: {}", source);
		++fileProblemsCounter;
	}
	
	public void translateDataDumpFiles(Path sourceDirectory, Path targetDirectory) {
		DataDumpTranslator ddt = new DataDumpTranslator(tdg);
		ddt.translateDumpDirectory(sourceDirectory, targetDirectory);
	}
	
	
	public void splitTrainingData(Path source, Path first, Path second, double ratio) throws NeuroBException{
		splitTrainingData(source, first, second, ratio, false);
	}
	
	public void splitTrainingData(Path source, Path first, Path second, double ratio, boolean deterministic) 
			throws NeuroBException{
		Random rng;
		if(deterministic)
			rng = new Random(123);
		else
			rng = new Random(); // no seed
		
		if(ratio < 0 || ratio > 1){
			throw new IllegalArgumentException("Parameter ratio has to be a value in the interval [0,1].");
		}
		
		tdg.splitTrainingData(source, first, second, ratio, rng);
	}
	
	/**
	 * <p>Excludes a given path in the source directory.
	 * </p>
	 * <p>This puts the <code>exclude</code> into the <code>excludeFile</code>. 
	 * <code>exclude</code> can hereby point to either an directory or a specific file.
	 * </p>
	 * 
	 * @param excludeFile The exclude file to write to
	 * @param exclude Path to the file or directory to exclude
	 */
	public void exclude(Path excludeFile, Path exclude) {		
		// check if already excluded
		boolean newExclude = true;
		if(Files.exists(excludeFile)){
			try(Stream<String> stream = Files.lines(excludeFile)){
				newExclude = stream.noneMatch(s -> s.equals(exclude.toString()));
			} catch (IOException e) {
				log.error("Could not access exclude file: "+e.getMessage(), e);
			}
		}
		
		// add it to the excludes.list file
		if(newExclude){
			try {
				Files.write(excludeFile, (exclude.toString()+"\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			} catch (IOException e) {
				log.error("Could not append the exclude properly: "+e.getMessage(), e);
			}
		}
	}
	
	
	
}
