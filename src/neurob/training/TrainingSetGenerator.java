package neurob.training;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.exception.ProBError;
import neurob.logging.NeuroBLogFormatter;
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
	private int fileCounter; // number of files seen
	private int fileProblemsCounter; // number of files which caused problems
	private static final Logger logger = Logger.getLogger(TrainingSetGenerator.class.getName());
	
	/**
	 * 
	 * @param trainingDataCollector Instance of the training collection to be used
	 */
	public TrainingSetGenerator(TrainingDataCollector trainingDataCollector) {
		tdc = trainingDataCollector;
		
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
					"NeuroB-TrainingSetGenerator-"
					+dateFormat.format(new Date())
					+"-%u.log");
			fh.setFormatter(new NeuroBLogFormatter());
			logger.addHandler(fh);
		} catch (SecurityException | IOException e) {
			System.err.println("Could not greate file logger");
		}
		
		// set Logger of tdc
		tdc.setLogger(logger);
		
		fileCounter = 0;
		fileProblemsCounter = 0;
	}
	
	/**
	 * Adds an overview of the seen files to the log
	 */
	public void logStatistics(){
		logger.info("**********");
		logger.info("Summary:");
		logger.info("Seen:\t"+fileCounter+" .mch-files");
		logger.info("\t"+fileProblemsCounter+" caused problems and could not be properly processed");
	}
	
	public void logTrainingSetAnalysis(Path dir){
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.analyseTrainingSet(dir);
		logger.info("**********");
		logger.info("Training Data Analysis:");
		logger.info(tsa.getStatistics());
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
		
		// iterate over directory
		int depth = (recursion) ? Integer.MAX_VALUE : 1;
		try (Stream<Path> stream = Files.walk(sourceDirectory, depth)) {
			Files.createDirectories(targetDirectory);
			
			stream
//				.parallel() // parallel computation
				.forEach(entry -> {
					System.out.println(entry);
//					// check if directory or not; recursion if so, else get features from file if .mch
//		            if (Files.isDirectory(entry) && recursion) {
//		            	Path subdir = entry.getFileName(); // get directory name
//		            	
//		            	/*
//		            	 * TODO:
//		            	 * Find better training data. The ProB examples contain a subdirectory called Tickets/ParserPushBackOverflow/, 
//		            	 * in which code samples can be found the parser fails to parse, causing everything to just blow up, 
//		            	 * as I can not catch the thrown exception properly (although I should..)
//		            	 * 
//		            	 * For now I simply skip ParserPushBackOverflow/ 
//		            	 * 
//		            	 * Same with PerformanceTests/
//		            	 * and RefinementChecking/
//		            	 */
//		            	if(subdir.toString().equals("ParserPushBackOverflow")
//		            			|| subdir.toString().equals("PerformanceTests")
//		            			|| subdir.toString().equals("RefinementChecking")) return;
//		            	
//		            	generateTrainingSet(sourceDirectory.resolve(subdir), targetDirectory.resolve(subdir), recursion);
//		            }
//		            else if(Files.isRegularFile(entry)){
	            	if(Files.isRegularFile(entry)){
		            	
		            	// check file extension
		            	String fileName = entry.getFileName().toString();
		            	String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		            	
		            	if(ext.equals("mch")){
		            		fileCounter++;
		            		Path dataFilePath = targetDirectory.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+".nbtrain");
		            		
		            		generateTrainingDataFile(entry, dataFilePath);
		            		
		            	}
		            	
		            }
					
				});
	    }
		catch (IOException e){
			logger.severe("Could not access directory "+sourceDirectory+": "+e.getMessage());
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
		try {
			tdc.collectTrainingData(source, target);
			logger.fine("\tDone: "+target);
			return;
		} catch (BException e) {
			logger.warning("\tCould not parse "+source+": "+e.getMessage());
		} catch (ProBError e) {
			logger.warning("\tProBError on "+source+": "+e.getMessage());
		} catch (IOException e) {
			logger.warning("\tCould not access file: "+e.getMessage());
		}
		fileProblemsCounter++;
	}
	
	public void generateCSVFromNBTrainData(Path sourceDirectory, Path target){
		try (Stream<Path> stream = Files.walk(sourceDirectory)){
			// Create CSV file
			BufferedWriter csv = Files.newBufferedWriter(target);
			
			// set header
			for(int i=0; i<tdc.getNumberOfFeatures(); i++){
				csv.write("Feature"+i+",");
			}
			for(int i=0; i< tdc.getNumberOfLabels(); i++){
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
						logger.info("Found "+f);
						// nbtrain file found!
						// read line wise
						try (Stream<String> lines = Files.lines(f)){
							lines.forEach(l -> {
								try {
									csv.write(l.replace(':', ',')+"\n");// replace : with , to get csv format
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
			logger.severe("Failed to setup CSV correctly:" +e.getMessage());
		}
	}
	
	/**
	 * <p>Excludes a given path in the source directoy.
	 * </p>
	 * <p>This puts the <code>exclude</code> into the 
	 * <i>excludes.list</i> file in the source directory. <code>exclude</code> can hereby point to either an directory or a specific file.
	 * </p>
	 * <p>Note: <code>exclude</code> must be a subpath of <code>sourceDirectory</code>
	 * <br>
	 * Say the file <i>source/subdirectory/exclude_me.mch</i> is to be excluded, but <i>source/</i> is the data directory to 
	 * generate train files from. Then <code>sourceDirectory</code> should point to <i>source/</i> and 
	 * <code>exclude</code> to <i>subdirectory/exclude_me.mch</i>.
	 * </p>
	 * 
	 * @param sourceDirectory
	 * @param exclude Path to the file or subdirectory to exclude, relative to <code>sourceDirectory</code>
	 */
	public void exclude(Path sourceDirectory, Path exclude) {
		Path resolvedExcludePath = sourceDirectory.resolve(exclude);
		
		// check if already excluded
		Path exlist = sourceDirectory.resolve("excludes.list");
		boolean newExclude = true;
		if(Files.exists(exlist)){
			try(Stream<String> stream = Files.lines(exlist)){
				newExclude = stream.noneMatch(s -> s.equals(exclude.toString()));
			} catch (IOException e1) {
				System.err.println("Could not access excludes.list: "+e1.getMessage());
			}
		}
		
		// add it to the excludes.list file
		if(newExclude){
			try {
				Files.write(exlist, (exclude.toString()+"\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			} catch (IOException e) {
				System.err.println("Could not append the exclude properly: "+e.getMessage());
			}
		}
	}
	
	
	
}
