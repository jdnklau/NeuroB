package neurob.training;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.util.ProblemType;
import neurob.training.analysis.ClassificationAnalysis;
import neurob.training.analysis.RegressionAnalysis;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;

public class TrainingSetAnalyser {
	private static final Logger log = LoggerFactory.getLogger(TrainingSetAnalyser.class);
	
	public void logTrainingAnalysis(TrainingAnalysisData analysis){
		if(analysis == null){
			log.warn("No training analysis data to log");
			return;
		}
		analysis.log();
	}
	
	/**
	 * Analyses all samples in the given csv file with respect to the given {@link LabelGenerator}.
	 * <p>
	 * The LabelGenerator is used for deciding whether the data represents 
	 * {@link ClassificationAnalysis classification} or {@link RegressionAnalysis regression}, 
	 * and to know the number of different classes before hand. 
	 * Alternatively use {@link #analyseTrainingCSV(Path, TrainingAnalysisData)} to use a custom analysis data class.
	 * @param csv
	 * @param labelgen
	 * @return
	 * @throws IOException
	 * @see {@link #analyseTrainingCSV(Path, TrainingAnalysisData)}
	 */
	public TrainingAnalysisData analyseTrainingCSV(Path csv, LabelGenerator labelgen) throws IOException{
		TrainingAnalysisData data;
		
		if(labelgen.getProblemType() == ProblemType.REGRESSION){
			data = new RegressionAnalysis(labelgen.getLabelDimension());
			return analyseTrainingCSV(csv, data, labelgen.getLabelDimension());
		}
		else {
			data = new ClassificationAnalysis(labelgen.getClassCount());
			return analyseTrainingCSV(csv, data, 1); // classification in csv is only one dimensional
		}
	}
	
	/**
	 * Analyses all samples in the given csv file with respect to the given {@link TrainingAnalysisData} object.
	 * <p>
	 * {@code labelSize} argument denotes the size of the labelling vector in the given sample. 
	 * For classification tasks, this is typically 1, as the training data only contains the label name.
	 * If a one-hot vector representation is used, it still will be 1, as the respresentation is build in training
	 * from this single number.
	 * <br>
	 * For regression, this is the number of different values predicted. E.g. if one net regresses sin and cos functions
	 * applied on the input, this would be 2, as we got two outputs.
	 * 
	 * @param csv
	 * @param data
	 * @param labelSize Number of entries the samples reserves for the label. For classification problems, this is typically 1.
	 * @return
	 * @throws IOException
	 * @see {@link #analyseTrainingCSV(Path, LabelGenerator)}
	 */
	public TrainingAnalysisData analyseTrainingCSV(Path csv, TrainingAnalysisData data, int labelSize) throws IOException{		
		try(Stream<String> stream = Files.lines(csv)){
			stream
				.skip(1) // skip first line
				.forEachOrdered(line -> {
					double[] values = Arrays.stream(line.split(","))
										.mapToDouble(Double::parseDouble)
										.toArray();
					int firstLabelIndex = values.length-labelSize;
					
					double[] features = new double[firstLabelIndex];
					for(int i=0; i<firstLabelIndex; i++)
						features[i] = values[i];
					double[] labels = new double[labelSize];
					for(int i=0; i<labelSize; i++)
						labels[i] = values[i + firstLabelIndex];
					data.analyseSample(features, labels);
				});
		}
		
		return data;
	}
	
	/**
	 * Analyses the .nbtrain files in the given directory with respect to the given {@link LabelGenerator}.
	 * <p>
	 * The LabelGenerator is used for deciding whether the data represent classification or regression, 
	 * and to know the number of different classes before hand. 
	 * Alternatively use {@link #analyseNBTrainSet(Path, TrainingAnalysisData)} to use a custom analysis data class.
	 * @param sourceDirectory A directory full of nbtrain files
	 * @param labelgen A LabelGenerator used to create the nbtrain files
	 * @return A {@link ClassificationAnalysis} object or {@code null} in case of errors.
	 * @see #analyseNBTrainSet(Path, TrainingAnalysisData)
	 */
	public TrainingAnalysisData analyseNBTrainSet(Path sourceDirectory, LabelGenerator labelgen) throws IOException{
		TrainingAnalysisData data;
		
		if(labelgen.getProblemType() == ProblemType.REGRESSION){
			data = new RegressionAnalysis(labelgen.getLabelDimension());
		}
		else {
			data = new ClassificationAnalysis(labelgen.getClassCount());
		}
		
		return analyseNBTrainSet(sourceDirectory, data);
	}
	
	/**
	 * Analyses the .nbtrain files in the given directory with respect to the given {@link TrainingAnalysisData} object.
	 * <p>
	 * The data object gets modified and is returned in the end.
	 * @param sourceDirectory
	 * @param data Object implementing {@link TrainingAnalysisData} interface used for the analysis
	 * @return
	 * @throws IOException
	 */
	public TrainingAnalysisData analyseNBTrainSet(Path sourceDirectory, TrainingAnalysisData data) throws IOException{
		
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
		}
		
		return data;
	}

}
