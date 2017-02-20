package neurob.training;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.util.Collections;
import neurob.training.analysis.ClassificationAnalysis;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;

public class TrainingSetAnalyser {
	private static final Logger log = LoggerFactory.getLogger(TrainingSetAnalyser.class);
	
	public void logTrainingAnalysis(ClassificationAnalysis analysis){
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
			return new ClassificationAnalysis(0);
		}
		
		TrainingAnalysisData data = new ClassificationAnalysis(numClasses);
		
		try(Stream<String> stream = Files.lines(csv)){
			stream
				.skip(1) // skip first line
				.forEachOrdered(line -> {
					String[] sample = line.split(":");
					double[] features = Arrays.stream(sample[0].split(","))
							.mapToDouble(Double::parseDouble).toArray();
					double[] labels = Arrays.stream(sample[1].split(","))
							.mapToDouble(Double::parseDouble).toArray();
					data.analyseSample(features, labels);
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
	 * @return A {@link ClassificationAnalysis} object or {@code null} in case of errors.
	 */
	public TrainingAnalysisData analyseNBTrainSet(Path sourceDirectory, LabelGenerator labelgen) throws IOException{		
		int numClasses = labelgen.getClassCount();
		if(numClasses < 1){
			log.error("Analysis of NBTrain files for regression problems not yet supported");
			return new ClassificationAnalysis(0);
		}
		
		TrainingAnalysisData data = new ClassificationAnalysis(numClasses);
		
		// iterate over directory recursively
		try (Stream<Path> stream = Files.walk(sourceDirectory)) {
			stream
				.parallel()
				.forEachOrdered(entry -> {
					if(Files.isRegularFile(entry)){
						data.countFileSeen(); // found a file, did it not?
			
						// check file extension
						String fileName = entry.getFileName().toString();
						String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
						
						if(ext.equals("nbtrain")){
						// save old data count to compare later on if we got an empty file
						int oldDataCount = data.getSamplesCount();
						
						// check if targets are not all equal
						try(Stream<String> filelines = Files.lines(entry)){
							filelines.forEach(line -> {
								
								String[] sample = line.split(":");
								double[] features = Arrays.stream(sample[0].split(","))
										.mapToDouble(Double::parseDouble).toArray();
								double[] labels = Arrays.stream(sample[1].split(","))
										.mapToDouble(Double::parseDouble).toArray();
								data.analyseSample(features, labels);
						
							});
						} catch (IOException e){
							log.warn("Could not access {}: {}", entry, e.getMessage());
						}
			
						// no new data found
						if (data.getSamplesCount() == oldDataCount){
							data.countEmptyFileSeen();
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
