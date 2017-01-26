package neurob.training;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractElement;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateCollector;

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
	// Training data handling
	private FeatureGenerator fg; // Feature generator in use
	private LabelGenerator lg; // Label generator in use
	// FInding the api
	protected Api api;
	// statistics
	private int fileCounter; // number of files seen
	private int fileProblemsCounter; // number of files which caused problems
	// logger
	private static final Logger log = LoggerFactory.getLogger(TrainingSetGenerator.class);
	
	/**
	 * Set up a training set generator, using the given feature generator and label generator
	 * @param featureGenerator
	 * @param labelGenerator
	 */
	@Inject
	public TrainingSetGenerator(FeatureGenerator featureGenerator, LabelGenerator labelGenerator){
		fg = featureGenerator;
		lg = labelGenerator;
		
		fileCounter = 0;
		fileProblemsCounter = 0;
		
		api = Main.getInjector().getInstance(Api.class);
	}
	
	/**
	 * Adds an overview of the seen files to the log
	 */
	public void logStatistics(){
		log.info("Summary of training set generation:");
		log.info("Seen:\t{} .mch-files", fileCounter);
		log.info("\t{} caused problems and could not be properly processed", fileProblemsCounter);
		log.info("*****************************");
	}
	
	public void logTrainingSetAnalysis(Path dir){
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.logTrainingAnalysis(tsa.analyseNBTrainSet(dir, lg));
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
	 * Same as {@link TrainingSetGenerator#generateTrainingDataFile(Path, Path)}, but with the option to specify an exclude file and
	 * to turn off the recursion step.
	 * @param sourceDirectory
	 * @param targetDirectory
	 * @param excludeFile
	 * @param recursion If false, the subdirectories are not searched
	 */
	public void generateTrainingSet(Path sourceDirectory, Path targetDirectory, Path excludeFile, boolean recursion){
		log.info("Generating training set from {} in {}", sourceDirectory, targetDirectory);
		// prepare exclude data
		ArrayList<Path> excludes = new ArrayList<Path>();
		if(excludeFile != null){
//			Path excludeFileDirectory = excludeFile.getParent();
			try(Stream<String> exc = Files.lines(excludeFile)){
				excludes.addAll(
						(ArrayList<Path>) exc
							.filter(s -> !s.isEmpty())
							.map(s -> Paths.get(s)).collect(Collectors.toList()));
			} catch (IOException e) {
				log.error("Could not access exclude file: {}", e.getMessage());
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
	            		// get ful target directory
	            		Path fullTargetDirectory = targetDirectory.resolve(entry.getParent());
						// check file extension
						String fileName = entry.getFileName().toString();
						String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
						if(ext.equals("mch")){
							fileCounter++;
							Path dataFilePath = fullTargetDirectory.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+".nbtrain");
							generateTrainingDataFile(entry, dataFilePath);
						}
		            }
				});
			log.info("Finished training set generation");
			log.info("******************************");
	    }
		catch (IOException e){
			log.error("Could not access directory {}: {}", sourceDirectory, e.getMessage());
		}
		
	}
	
	protected void collectTrainingData(Path sourceFile, Path targetFile) throws NeuroBException{
		// StateSpace and main component
		StateSpace ss = null;
		AbstractElement mainComp;
		// For the formula and ProB command to use
		ArrayList<String> formulae;
		
		// Access source file
		try{
			log.info("\tLoading machine file {} ...", sourceFile);
			ss = api.b_load(sourceFile.toString());
		}catch(Exception e) {
			throw new NeuroBException("Could not load machine correctly.", e);
		}
		
		// Get different formulas
		mainComp = ss.getMainComponent();	// extract main component
		PredicateCollector predc = new PredicateCollector(mainComp);
		formulae = FormulaGenerator.extendedGuardFormulas(predc);
		log.info("\tGenerated {} formulas to solve.", formulae.size());
		
		// generate data per formula
		ArrayList<String> results = new ArrayList<String>();
		int count = formulae.size();
		int curr = 1;
		for( String formula : formulae) {
			log.info("\tAt {}/{}...", curr++, count);
			try {
				// features:labeling vector:comment
				results.add(fg.generateFeatureString(formula)+":"+lg.generateLabelling(formula, ss)+":\""+formula+"\"");
			} catch (NeuroBException e) {
				log.warn("\t{}", e.getMessage());
			} catch (IllegalStateException e) {
				log.error("\tReached Illegal State: {}", e.getMessage());
			}
		}
		
		// close StateSpace
		ss.kill();
		
		// No training data to write? -> return from method
		// otherwise write to targetFile
		if(results.isEmpty()){
			log.info("\tNo training data created");
			return;
		}
		
		// open target file
		try(BufferedWriter out = Files.newBufferedWriter(targetFile)) {
			// write feature vector to stream
			log.info("\tWriting training data...");
			for(String res : results){
				out.write(res);
				out.newLine();
				out.flush();
			}
		} catch (IOException e) {
			throw new NeuroBException("Could not correctly access target file: "+targetFile, e);
		}
				
	}
	
	/**
	 * Generates a file containing feature data found in the source file,
	 * and writes them to the target file.
	 * @param source
	 * @param target
	 */
	public void generateTrainingDataFile(Path source, Path target){
		log.info("Generating: {} > {}", source, target);
		
		Path targetDirectory = target.getParent();
		// ensure existence of target directory
		try {
			Files.createDirectories(targetDirectory);
		} catch (IOException e) {
			log.error("\tCould not create or access directory {}: {}", targetDirectory, e.getMessage());
			return;
		}
		// create file
		try {
			collectTrainingData(source, target);
			log.info("\tDone: {}", target);
			return;
		} catch (ProBError e) {
			log.warn("\tProBError on {}: {}", source, e.getMessage());
		} catch (IllegalStateException e) {
			log.error("\tReached illegal state while processing: {}", e.getMessage());
		} catch (NeuroBException e) {
			log.error("\t{}", e.getMessage());
		}
		log.info("\tStopped with errors: {}", target);
		++fileProblemsCounter;
	}
	
	/**
	 * Creates two CSV files from the .nbtrain files in the given {@code sourceDirectory}.
	 * <p>
	 * With a probability of {@code triningRation}, the data will be put into the training file,
	 * or in the test file otherwise.
	 * @param sourceDirectory
	 * @param train
	 * @param test
	 * @param trainingRatio Probability that data will be written into the training set
	 */
	public void generateTrainAndTestCSVfromNBTrainData(Path sourceDirectory, Path train, Path test, double trainingRatio){
		Random rng = new Random(123);
		
		try (Stream<Path> stream = Files.walk(sourceDirectory)){
			// Create CSV files
			Files.createDirectories(train.getParent());
			Files.createDirectories(test.getParent());
			BufferedWriter trainCsv = Files.newBufferedWriter(train);
			BufferedWriter testCsv = Files.newBufferedWriter(test);
			
			// set headers
			for(int i=0; i<fg.getFeatureDimension(); i++){
				trainCsv.write("Feature"+i+",");
				testCsv.write("Feature"+i+",");
			}
			for(int i=0; i< lg.getLabelDimension(); i++){
				trainCsv.write("Label"+i+",");
				testCsv.write("Label"+i+",");
			}
			trainCsv.newLine();
			trainCsv.flush();
			testCsv.newLine();
			testCsv.flush();
			
			stream.forEach(f -> {
				// check if .nbtrain file
				if(Files.isRegularFile(f)){
					String fileName = f.getFileName().toString();
					String ext = fileName.substring(fileName.lastIndexOf('.'));
					if(ext.equals(".nbtrain")){
//						logger.info("Found "+f);
						// nbtrain file found!
						// read line wise
						try (Stream<String> lines = Files.lines(f)){
							lines.forEach(l -> {
								try {
									String[] data = l.split(":");
									String[] features = data[0].split(",");
									String[] labels = data[1].split(",");
									if(features.length+labels.length < fg.getFeatureDimension()+lg.getLabelDimension()){
										throw new NeuroBException("Size of training vector does not match, "
												+ "expecting "+ fg.getFeatureDimension()+" features and " + lg.getLabelDimension()+" labels, "
												+ "but got " +features.length + " and " + labels.length + " respectively");
									}
									
									// decide file to write to
									BufferedWriter targetCsv;
									if(rng.nextDouble() <= trainingRatio){
										targetCsv = trainCsv;
									}
									else {
										targetCsv = testCsv;
									}
									// write to chosen file
									targetCsv.write(String.join(",", features)+","+String.join(",", labels));
									targetCsv.newLine();
								} catch (NeuroBException e) {
									log.error("Could not add a data vector: {}", f, e.getMessage());
								} catch (IOException e) {
									log.error("Failed to write data vector to file: {}", e.getMessage());
								}
							});
							trainCsv.flush();
							testCsv.flush();
						} catch(IOException e) {
							log.error("Could not add data from {}: {}", f, e.getMessage());
						}
					}
					
				}
			});
			
		} catch (IOException e) {
			log.error("Failed to setup CSV correctly: {}", e.getMessage());
		}
		
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
			} catch (IOException e1) {
				System.err.println("Could not access exclude file: "+e1.getMessage());
			}
		}
		
		// add it to the excludes.list file
		if(newExclude){
			try {
				Files.write(excludeFile, (exclude.toString()+"\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			} catch (IOException e) {
				System.err.println("Could not append the exclude properly: "+e.getMessage());
			}
		}
	}
	
	
	
}
