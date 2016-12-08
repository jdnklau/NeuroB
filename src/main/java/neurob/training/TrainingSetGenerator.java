package neurob.training;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.Main;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractElement;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.logging.NeuroBLogFormatter;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.interfaces.TrainingDataCollector;
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
	private TrainingDataCollector tdc; // used collector of training data
	// FInding the api
	protected Api api;
	// statistics
	private int fileCounter; // number of files seen
	private int fileProblemsCounter; // number of files which caused problems
	// logger
	private static final Logger logger = Logger.getLogger(TrainingSetGenerator.class.getName());
	
	static {
		//** setting up logger
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.FINE);
		// log to console
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(new NeuroBLogFormatter());
		logger.addHandler(ch);
		// log to logfile
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
			FileHandler fh = new FileHandler(
					"neurob_logs/NeuroB-TrainingSetGenerator-"
					+dateFormat.format(new Date())
					+"-%u.log");
			fh.setFormatter(new NeuroBLogFormatter());
			logger.addHandler(fh);
		} catch (SecurityException | IOException e) {
			System.err.println("Could not greate file logger");
		}
	}
	
	/**
	 * 
	 * @param trainingDataCollector Instance of the training collection to be used
	 */
	@Deprecated
	public TrainingSetGenerator(TrainingDataCollector trainingDataCollector) {
		tdc = trainingDataCollector;
		
		// set Logger of tdc
		tdc.setLogger(logger);
		
		fileCounter = 0;
		fileProblemsCounter = 0;
	}
	
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
		logger.info("******************************");
		logger.info("Summary:");
		logger.info("Seen:\t"+fileCounter+" .mch-files");
		logger.info("\t"+fileProblemsCounter+" caused problems and could not be properly processed");
	}
	
	public void logTrainingSetAnalysis(Path dir){
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.analyseTrainingSet(dir);
		logger.info("******************************");
		logger.info("Training Data Analysis:");
		tsa.logStatistics(logger);
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
				logger.severe("Could not access exclude file: "+e.getMessage());
			}
		}
		
		// iterate over directory
		int depth = (recursion) ? Integer.MAX_VALUE : 1;
		try (Stream<Path> stream = Files.walk(sourceDirectory, depth)) {
			Files.createDirectories(targetDirectory);
			
			
			stream
//				.parallel() // parallel computation
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
	    }
		catch (IOException e){
			logger.severe("Could not access directory "+sourceDirectory+": "+e.getMessage());
		}
		
	}
	
	protected void collectTrainingData(Path sourceFile, Path targetFile) throws NeuroBException{
		// StateSpace and main component
		StateSpace ss = null;
		AbstractElement mainComp;
		// For the formula and ProB command to use
		ArrayList<String> formulae;
		ClassicalB f; // formula as EventB formula
		CbcSolveCommand cmd;
		
		// Access source file
		try{
			logger.info("\tLoading machine...");
			ss = api.b_load(sourceFile.toString());
		}catch(Exception e) {
			throw new NeuroBException("Could not load machine correctly.", e);
		}
		
		// Get different formulas
		mainComp = ss.getMainComponent();	// extract main component
		PredicateCollector predc = new PredicateCollector(mainComp);
		formulae = FormulaGenerator.extendedGuardFormulas(predc);
		logger.info("\tGenerated "+formulae.size()+" formulas to solve.");
		
		// generate data per formula
		ArrayList<String> results = new ArrayList<String>();
		int count = formulae.size();
		int curr = 1;
		for( String formula : formulae) {
			logger.info("\tAt "+(curr++)+"/"+count+"...");
			
			try {
				// features:labeling vector:comment
				results.add(fg.generateFeatureString(formula)+":"+lg.generateLabelling(formula, ss)+":\""+formula+"\"");
			} catch (NeuroBException e) {
				logger.warning(e.getMessage());
			}
		}
		
		// close StateSpace
		ss.kill();
		
		// open target file
		try(BufferedWriter out = Files.newBufferedWriter(targetFile)) {
			// write feature vector to stream
			logger.info("\tWriting training data...");
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
		logger.info("Generating: "+source+" > "+target);
		Path targetDirectory = target.getParent();
		// ensure existence of target directory
		try {
			Files.createDirectories(targetDirectory);
		} catch (IOException e) {
			logger.severe("\tCould not create or access directory "+targetDirectory+": "+e.getMessage());
			return;
		}
		// create file
		try {
			collectTrainingData(source, target);
			logger.fine("\tDone: "+target);
			return;
		} catch (ProBError e) {
			logger.warning("\tProBError on "+source+": "+e.getMessage());
		} catch (IllegalStateException e) {
			logger.severe("\tReached illegal state while processing: "+e.getMessage());
		} catch (NeuroBException e) {
			logger.severe("\t"+e.getMessage());
		}
		fileProblemsCounter++;
	}

	public void generateCSVFromNBTrainData(Path sourceDirectory, Path target){
		generateCSVFromNBTrainData(sourceDirectory, target, false);
	}
	
	public void generateCSVFromNBTrainData(Path sourceDirectory, Path target, boolean ignoreWithSameLabeling){
		try (Stream<Path> stream = Files.walk(sourceDirectory)){
			// Create CSV file
			Files.createDirectories(target.getParent());
			BufferedWriter csv = Files.newBufferedWriter(target);
			
			// set header
			for(int i=0; i<fg.getfeatureDimension(); i++){
				csv.write("Feature"+i+",");
			}
			for(int i=0; i< lg.getLabelDimension(); i++){
				csv.write("Label"+i+",");
			}
			csv.newLine();
			csv.flush();
			
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
									if(features.length+labels.length < fg.getfeatureDimension()+lg.getLabelDimension()){
										throw new Exception("Size of training vector does not match!");
									}
									boolean write = true;
									if(ignoreWithSameLabeling){
										String pivot = labels[0];
										write = false;
										for(int i=1; !write & i<labels.length; i++){
											if(!labels[i].equals(pivot)){
												write = true;
											}
										}
									}
									if(write){
										csv.write(String.join(",", features)+","+String.join(",", labels));
										csv.newLine();
									}
//									csv.write(l.replace(':', ',')+"\n");// replace : with , to get csv format
								} catch (Exception e) {
									logger.warning("Could not add a data vector from "+f+": "+e.getMessage());
								} 
							});
							csv.flush();
						} catch(IOException e) {
							logger.severe("Could not add data from "+f+": "+e.getMessage());
						}
					}
					
				}
			});
			
		} catch (IOException e) {
			logger.severe("Failed to setup CSV correctly: " +e.getMessage());
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
