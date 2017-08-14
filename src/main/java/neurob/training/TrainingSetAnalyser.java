package neurob.training;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.util.ProblemType;
import neurob.training.analysis.ClassificationAnalysis;
import neurob.training.analysis.PredicateDumpAnalysis;
import neurob.training.analysis.RegressionAnalysis;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;

public class TrainingSetAnalyser {
	private static final Logger log = LoggerFactory.getLogger(TrainingSetAnalyser.class);

	/**
	 * Logs the results gathered by the analysis object on INFO level.
	 * @param analysis
	 */
	public static void logTrainingAnalysis(TrainingAnalysisData analysis){
		if(analysis == null){
			log.warn("No training analysis data to log");
			return;
		}
		log.info("Training set analysis");
		log.info(analysis.getStatistics());
		log.info("******************************");
	}

	/**
	 * Write the given analysis data to a <i>analysis.txt</i> file in the given directory.
	 * @param analysis Analysis data that already evaluated a data set
	 * @param directory Target directory to create the .txt file in
	 * @throws IOException
	 */
	public static void writeTrainingAnalysis(TrainingAnalysisData analysis, Path directory) throws IOException{
		Path analysisFile = directory.resolve("analysis.txt");
		log.info("Writing analysis results to {}", analysisFile);
		BufferedWriter out = Files.newBufferedWriter(analysisFile);
		out.write(analysis.getStatistics());
		out.close();
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
	public static TrainingAnalysisData analyseTrainingCSV(Path csv, LabelGenerator labelgen) throws IOException{
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
	public static TrainingAnalysisData analyseTrainingCSV(Path csv, TrainingAnalysisData data, int labelSize) throws IOException{
		// to compare later on if file was empty
		data.countFileSeen();
		int samplesBefore = data.getSamplesCount();

		// iterate over file
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

		// found an empty file
		if(data.getSamplesCount() == samplesBefore){
			data.countEmptyFileSeen();
		}

		return data;
	}

	/**
	 * Analyses training data files from the given directory with the given analyser,
	 * which have the given file extension.
	 * @param sourceDirectory
	 * @param data
	 * @param fileExtension
	 * @return
	 * @throws IOException
	 */
	public static TrainingAnalysisData analyseTrainingDataFiles(Path sourceDirectory,
			TrainingAnalysisData data, String fileExtension) throws IOException{
		// iterate over directory recursively
		try (Stream<Path> stream = Files.walk(sourceDirectory)) {
			stream
			.filter(Files::isRegularFile)
			.filter(p->p.toString().endsWith(fileExtension))
			.forEach(p->analyseTrainingDataFile(p, data));
		}

		return data;
	}

	public static void analyseTrainingDataFile(Path file, TrainingAnalysisData data){
		// to compare later on if file was empty
		data.countFileSeen();
		int samplesBefore = data.getSamplesCount();

		// iterate over file
		try(Stream<String> lines = Files.lines(file)){
			lines
			.filter(l->!l.startsWith("#"))
			.forEach(data::analyseTrainingDataSample);
		} catch (IOException e){
			log.error("Could not analyse {}", file, e);
		}

		// found an empty file
		if(data.getSamplesCount() == samplesBefore){
			data.countEmptyFileSeen();
		}
	}

	public static TrainingAnalysisData analysePredicateDumps(Path sourceDirectory) throws IOException{
		return analyseTrainingDataFiles(sourceDirectory, new PredicateDumpAnalysis(), ".pdump").evaluateAllSamples();
	}

	public static TrainingAnalysisData analysePredicateDumps(Path sourceDirectory, LabelGenerator lg)
			throws IOException{
		return analyseTrainingDataFiles(sourceDirectory, new PredicateDumpAnalysis(lg), ".pdump").evaluateAllSamples();
	}

	/**
	 * For a given label generator, return a fitting {@link TrainingAnalysisData} object.
	 * @param labelgen
	 * @return
	 */
	public static TrainingAnalysisData getAnalysisTypeByProblem(LabelGenerator labelgen){
		if(labelgen.getProblemType() == ProblemType.REGRESSION){
			return new RegressionAnalysis(labelgen.getLabelDimension());
		}
		else {
			return new ClassificationAnalysis(labelgen.getClassCount());
		}
	}

}
