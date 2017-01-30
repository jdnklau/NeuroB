package neurob.training;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Optional;
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
							generateTrainingDataFromFile(entry, dataFilePath);
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
		formulae = FormulaGenerator.extendedGuardFormulae(predc);
		formulae.addAll(FormulaGenerator.extendedGuardFomulaeWithInfiniteDomains(predc));
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
	 * Deprecated because name was to confusing.
	 * @deprecated {@link #generateTrainingDataFromFile(Path, Path)}
	 */
	@Deprecated
	public void generateTrainingDataFile(Path source, Path target){
		generateTrainingDataFromFile(source, target);
	}
	
	/**
	 * Generates a file containing feature data found in the source file,
	 * and writes them to the target file.
	 * @param source
	 * @param target
	 */
	public void generateTrainingDataFromFile(Path source, Path target){
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
	 * @deprecation Use {@link #splitCSV(Path, Path, Path, double, boolean)} instead
	 */
	@Deprecated
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
	
	public void generateCSVFromNBTrainFiles(Path sourceDirectory, Path csv){
		try (Stream<Path> stream = Files.walk(sourceDirectory)){
			// Create CSV files
			Files.createDirectories(csv.getParent());
			BufferedWriter trainCsv = Files.newBufferedWriter(csv);
			
			// set headers
			for(int i=0; i<fg.getFeatureDimension(); i++){
				trainCsv.write("Feature"+i+",");
			}
			for(int i=0; i< lg.getLabelDimension(); i++){
				trainCsv.write("Label"+i+",");
			}
			trainCsv.newLine();
			trainCsv.flush();
			
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
									// write to chosen file
									trainCsv.write(String.join(",", features)+","+String.join(",", labels));
									trainCsv.newLine();
								} catch (NeuroBException e) {
									log.error("Could not add a data vector: {}", f, e.getMessage());
								} catch (IOException e) {
									log.error("Failed to write data vector to file: {}", e.getMessage());
								}
							});
							trainCsv.flush();
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
	 * Split a training CSV file into two, aiming for a given ratio.
	 * <p>
	 * The ratio is a number r in [0,1]. For n samples in the original file, the
	 * train file will have ~r*n, the test file ~(1-r)*n samples.
	 * <p>
	 * The splitting is random, so calling it multiple times on the same file yields different results.
	 * This is intended behaviour, to use it e.g. for cross-validation.
	 * <p>
	 * This method calls {@link #splitCSV(Path, Path, Path, double, boolean)}, so if determinism is required,
	 * use that method instead.
	 * @param csv
	 * @param train
	 * @param test
	 * @param ratio
	 * @throws NeuroBException 
	 * @see #splitCSV(Path, Path, Path, double, boolean)
	 */
	public static void splitCSV(Path csv, Path train, Path test, double ratio) throws NeuroBException{
		splitCSV(csv, train, test, ratio, false);
	}
	
	/**
	 * Split a training CSV file into two, aiming for a given ratio.
	 * <p>
	 * The ratio is a number r in [0,1]. For n samples in the original file, the
	 * train file will have ~r*n, the test file ~(1-r)*n samples.
	 * <p>
	 * The {@code deterministic} parameter influences the choice of RNG used internally. If set to true, the 
	 * RNG will always be initialised with the same seed, resulting in deterministic behaviour.
	 * @param csv Original CSV file, containing training set data generated
	 * @param train Targeted file to split to, having {@code ratio*samples} from the original file
	 * @param test Targeted file to split to, having {@code (1-ratio)*samples} from the original file
	 * @param ratio A number in [0,1]
	 * @param deterministic Whether to use a specific seed for RNG or not
	 * @throws NeuroBException 
	 */
	public static void splitCSV(Path csv, Path train, Path test, double ratio, boolean deterministic) throws NeuroBException{
		Random rng;
		if(deterministic)
			rng = new Random(123);
		else
			rng = new Random(); // no seed
		
		if(ratio < 0 || ratio > 1){
			throw new IllegalArgumentException("Parameter ratio has to be a value in the interval [0,1].");
		}
		
		// open CSV
		// step 1: extract header
		BufferedReader br;
		String header;
		try {
			br = new BufferedReader(new FileReader(csv.toFile()));
			header = br.readLine();
			br.close();		
		} catch (FileNotFoundException e) {
			throw new NeuroBException("Could not find the source CSV: " + e.getMessage());
		} catch (IOException e) {
			throw new NeuroBException("Could not access the source CSV: " + e.getMessage());
		}
		
		try(Stream<String> stream = Files.lines(csv)){
			// Set up CSV files
			Files.createDirectories(train.getParent());
			Files.createDirectories(test.getParent());
			BufferedWriter trainCsv = Files.newBufferedWriter(train);
			BufferedWriter testCsv = Files.newBufferedWriter(test);
			//headers
			trainCsv.write(header);
			trainCsv.newLine();
			trainCsv.flush();
			testCsv.write(header);
			testCsv.newLine();
			testCsv.flush();
			
			// write data
			stream
				.skip(1)
				.forEachOrdered(line -> {
					double coinflip = rng.nextDouble();
					BufferedWriter target; // will be switching between train or test
					
					if(coinflip <= ratio){
						target = trainCsv;
					}
					else {
						target = testCsv;
					}
					
					// simply write the line to the target
					try {
						target.write(line);
						target.newLine();
					} catch (Exception e) {
						log.error("Could not write data to target csv: {}", e.getMessage());
					}
				});
			
			trainCsv.flush();
			testCsv.flush();

			trainCsv.close();
			testCsv.close();
			
		} catch (IOException e) {
			log.error("Could not access target files correctly: {}", e.getMessage());
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
