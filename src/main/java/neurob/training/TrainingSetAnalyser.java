package neurob.training;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;

public class TrainingSetAnalyser {
	private int dataCount; // counts the lines in the found files, being the feature and target data 
	private static final Logger log = LoggerFactory.getLogger(TrainingSetAnalyser.class);
	
	public TrainingSetAnalyser(){
		dataCount = 0;
	}
	
	public void logTrainingAnalysis(TrainingAnalysisData analysis){
		if(analysis == null){
			log.warn("No training analysis data to log");
			return;
		}
		analysis.log();
	}
	
	public TrainingAnalysisData analyseTrainingCSV(Path csv, LabelGenerator labelgen) throws IOException{
		int numClasses = labelgen.getClassCount();
		if(numClasses < 1){
			log.error("Analysis of samples for regression problems not yet supported");
			return new TrainingAnalysisData(0);
		}
		
		TrainingAnalysisData data = new TrainingAnalysisData(numClasses);
		
		try(Stream<String> stream = Files.lines(csv)){
			stream
				.skip(1) // skip first line
				.forEachOrdered(line -> {
					// NOTE: huge assumption, that
					// last entry is classification
					int trueLabel = Integer.parseInt(line.substring(line.lastIndexOf(',')+1));
					data.countEntryForClass(trueLabel);
				});
		} catch (IOException e) {
			// log.error("Could not open target csv {}: {}", csv, e.getMessage());
			throw e;
		}
		
		return data;
	}
	
	
	/**
	 * Analyses the .nbtrain files in the given directory with respect to the given {@link LabelGenerator}.
	 * <p>
	 * The LabelGenerator is used for deciding whether the data represent classification or regression, 
	 * and to know the number of different classes before hand.
	 * @param sourceDirectory A directory full of nbtrain files
	 * @param labelgen A LabelGenerator used to create the nbtrain files
	 * @return A {@link TrainingAnalysisData} object or {@code null} in case of errors.
	 */
	public TrainingAnalysisData analyseNBTrainSet(Path sourceDirectory, LabelGenerator labelgen) throws IOException{		
		int numClasses = labelgen.getClassCount();
		if(numClasses < 1){
			log.error("Analysis of NBTrain files for regression problems not yet supported");
			return new TrainingAnalysisData(0);
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
			//log.error("Could not access directory {}: {}", sourceDirectory, e.getMessage());
			throw e;
		}
		
		return data;
	}

}
