package neurob.training;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import neurob.logging.NeuroBLogFormatter;

public class TrainingSetAnalyser {
	private int fileCount;
	private int emptyFilesCount;
	private int dataCount; // counts the lines in the found files, being the feature and target data
	private int uninteresstingDataCount; // data lines having all target values as the same 
	private static final Logger logger = Logger.getLogger(TrainingSetAnalyser.class.getName());
	
	static {
		//** setting up logger
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.FINE);
		// log to console
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(new NeuroBLogFormatter());
		logger.addHandler(ch);
		// log to logfile
//		try {
//			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
//			FileHandler fh = new FileHandler(
//					"neurob_logs/NeuroB-TrainingSetAnalyser-"
//					+dateFormat.format(new Date())
//					+"-%u.log");
//			fh.setFormatter(new NeuroBLogFormatter());
//			logger.addHandler(fh);
//		} catch (SecurityException | IOException e) {
//			System.err.println("Could not greate file logger");
//		}
	}
	
	public TrainingSetAnalyser(){
		fileCount = 0;
		emptyFilesCount = 0;
		dataCount = 0;
		uninteresstingDataCount = 0;
	}
	
	/**
	 * Analyses all .nbtrain files in the given directory and 
	 * gives corresponding output containing the gathered statistics.
	 * @param sourceDirectory
	 */
	public void analyseTrainingSet(Path sourceDirectory){
		analyseTrainingSet(sourceDirectory, false);
	}
	public void analyseTrainingSet(Path sourceDirectory, boolean logRelevantFiles){
		
		// iterate over directory recursively
		try (Stream<Path> stream = Files.list(sourceDirectory)) {
			
			stream
				.parallel()
				.forEachOrdered(entry -> {
		        	// check if directory or not; recursion if so
		            if (Files.isDirectory(entry)) {
		            	analyseTrainingSet(entry, logRelevantFiles);
		            } 
		            else if(Files.isRegularFile(entry)){
		            	
		            	// check file extension
		            	String fileName = entry.getFileName().toString();
		            	String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		            	
		            	if(ext.equals("nbtrain")){
		            		fileCount++; // found a file, did it not?
		            		
		            		// save old data count to compare later on
		            		int oldDataCount = dataCount;
		            		// check if targets are not all equal
		            		try(Stream<String> filelines = Files.lines(entry)){
		            			filelines.forEach(line -> {
		            				dataCount++; // found data line
		            				
		            				String[] targets = line.split(":")[1].split(",");
		            				
		            				String ot = targets[0]; // old target label (the one to compare with
		            				boolean differentTargets = false;
		            				for(String t : targets) {
		            					if(!t.equals(ot)){
		            						differentTargets = true;
		            						break; // exit loop
		            					}
		            					ot = t;
		            				}
		            				
		            				// did not find different labels
		            				if (!differentTargets) {
		            					uninteresstingDataCount++; // => no informational gain by this data
		            				} else if(logRelevantFiles) {
		            					// Found interesting data
		            					logger.info(entry.toString());
		            					logger.info("\t"+line);
		            				}
		            				
		            			});
		            		} catch (IOException e){
		            			logger.warning("Could not access "+entry+": "+e.getMessage());
		            		}
		            		
	        				// no new data found
	        				if (dataCount == oldDataCount){
	        					emptyFilesCount++; // => so we found an empty file
	        				}
		            		
		            	}
		            }

				});
		} catch (IOException e) {
			logger.severe("Could not access directory "+sourceDirectory+": "+e.getMessage());
		}
    }
	/**
	 * 
	 * @deprecated Use {@link #logStatistics()} instead to get the string properly logged
	 */
	@Deprecated
	public String getStatistics(){
		int relevantFiles = fileCount-emptyFilesCount;
		StringBuilder b = new StringBuilder(1000);
		b.append("Files found: "+fileCount).append('\n');
		b.append("Of these were "+emptyFilesCount+" seemingly empty").append('\n');
		b.append("=> "+relevantFiles+" relevant files").append('\n');
		b.append("In the relevent filese were "+dataCount+" data vectors").append('\n');
		b.append("and of these serve "+uninteresstingDataCount+" vectors no informational gain").append('\n');
		return b.toString();
	}
	
	public void logStatistics(){
		logStatistics(logger);
	}
	public void logStatistics(Logger l) {
		int relevantFiles = fileCount-emptyFilesCount;
		l.info("Files found: "+fileCount);
		l.info("Of these were "+emptyFilesCount+" seemingly empty");
		l.info("=> "+relevantFiles+" relevant files");
		l.info("In the relevent filese were "+dataCount+" data vectors");
		l.info("and of these serve "+uninteresstingDataCount+" vectors no informational gain");
	}

}
