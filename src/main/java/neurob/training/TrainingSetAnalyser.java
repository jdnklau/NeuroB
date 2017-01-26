package neurob.training;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;

public class TrainingSetAnalyser {
	private int fileCount;
	private int emptyFilesCount;
	private int dataCount; // counts the lines in the found files, being the feature and target data
	private int uninteresstingDataCount; // data lines having all target values as the same 
	private static final Logger log = LoggerFactory.getLogger(TrainingSetAnalyser.class);
	
	public TrainingSetAnalyser(){
		fileCount = 0;
		emptyFilesCount = 0;
		dataCount = 0;
		uninteresstingDataCount = 0;
	}
	
	public void logTrainingAnalysis(TrainingAnalysisData analysis){
		log.info("Analysing training data...");
		
		if(analysis == null){
			log.warn("No training analysis data to log");
			return;
		}

		int fileCount = analysis.getFilesSeen();
		int emptyFilesCount = analysis.getEmptyFilesSeen();
		
		int relevantFiles = fileCount-emptyFilesCount;
		log.info("Files found: "+fileCount);
		log.info("Of these were "+emptyFilesCount+" seemingly empty");
		log.info("=> "+relevantFiles+" relevant files");
		
		// list classification mappings
		int dataCount = 0;
		ArrayList<Integer> trueLabelCounts = analysis.getTrueLabelCounters();
		for(int i = 0; i < trueLabelCounts.size(); i++){
			int classSamples = trueLabelCounts.get(i);
			dataCount += classSamples;
			log.info("\tClass {} is represented by {} samples", i, classSamples);
		}
		log.info("The relevant files contain "+dataCount+" samples in total");
		
		log.info("*****************************");
	}
	
	/**
	 * Analyses all .nbtrain files in the given directory and 
	 * gives corresponding output containing the gathered statistics.
	 * @param sourceDirectory
	 */
	@Deprecated
	public void analyseTrainingSet(Path sourceDirectory){
		log.info("Analysing training data...");
		analyseTrainingSet(sourceDirectory, false);
		
		int relevantFiles = fileCount-emptyFilesCount;
		log.info("Files found: "+fileCount);
		log.info("Of these were "+emptyFilesCount+" seemingly empty");
		log.info("=> "+relevantFiles+" relevant files");
		log.info("In the relevent filese were "+dataCount+" data vectors");
		log.info("and of these serve "+uninteresstingDataCount+" vectors no informational gain");
		
		log.info("*****************************");
		
	}
	
	@Deprecated
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
		            					log.debug("Relevant file found: {}",entry.toString());
		            					log.debug("\t"+line);
		            				}
		            				
		            			});
		            		} catch (IOException e){
		            			log.warn("Could not access {}: {}", entry, e.getMessage());
		            		}
		            		
	        				// no new data found
	        				if (dataCount == oldDataCount){
	        					emptyFilesCount++; // => so we found an empty file
	        				}
		            		
		            	}
		            }

				});
		} catch (IOException e) {
			log.error("Could not access directory {}: {}", sourceDirectory, e.getMessage());
		}
    }
	
	public TrainingAnalysisData analyseNBTrainSet(Path sourceDirectory, LabelGenerator labelgen){		
		int numClasses = labelgen.getClassCount();
		if(numClasses < 1){
			log.error("Analysis of NBTrain files for regression problems not yet supported");
			return null;
		}
		
		TrainingAnalysisData data = new TrainingAnalysisData(numClasses);
		
		// iterate over directory recursively
		try (Stream<Path> stream = Files.walk(sourceDirectory)) {
			stream
				.parallel()
				.forEachOrdered(entry -> {
					if(Files.isRegularFile(entry)){
						data.addFileSeen(); // found a file, did it not?
			
						// check file extension
						String fileName = entry.getFileName().toString();
						String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
						
						if(ext.equals("nbtrain")){
						// save old data count to compare later on if we got an empty file
						int oldDataCount = dataCount;
						
						// check if targets are not all equal
						try(Stream<String> filelines = Files.lines(entry)){
							filelines.forEach(line -> {
								dataCount++; // found data line
						
								// NOTE: This may be a huge assumption, that we only got one dimension in
								// the label. The net later will predict a one-hot vector, but this is
								// pre training.
								int trueLabel = Integer.parseInt(line.split(":")[1]);
								
								data.countEntryForClass(trueLabel);
						
							});
						} catch (IOException e){
							log.warn("Could not access {}: {}", entry, e.getMessage());
						}
			
						// no new data found
						if (dataCount == oldDataCount){
							data.addEmptyFileSeen();
						}
					}
				}
			
			});
		} catch (IOException e) {
			log.error("Could not access directory {}: {}", sourceDirectory, e.getMessage());
		}
		
		return data;
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
	
	@Deprecated
	public void logStatistics(){
		logStatistics(log);
	}
	@Deprecated
	public void logStatistics(Logger log) {
		int relevantFiles = fileCount-emptyFilesCount;
		log.info("Files found: "+fileCount);
		log.info("Of these were "+emptyFilesCount+" seemingly empty");
		log.info("=> "+relevantFiles+" relevant files");
		log.info("In the relevent filese were "+dataCount+" data vectors");
		log.info("and of these serve "+uninteresstingDataCount+" vectors no informational gain");
	}

}
