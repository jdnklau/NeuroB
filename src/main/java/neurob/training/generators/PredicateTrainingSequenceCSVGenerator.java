package neurob.training.generators;

import neurob.core.features.interfaces.RNNFeatures;
import neurob.exceptions.NeuroBException;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.TrainingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author Jannik Dunkelau
 */
public class PredicateTrainingSequenceCSVGenerator extends PredicateTrainingDataGenerator{

	private int sequenceCounter;
	private final RNNFeatures fg;
	private static final Logger log =
			LoggerFactory.getLogger(PredicateTrainingSequenceCSVGenerator.class);

	public PredicateTrainingSequenceCSVGenerator(
			RNNFeatures featureGenerator, LabelGenerator labelGenerator){
		super(featureGenerator, labelGenerator);

		this.fg = featureGenerator;
		preferredFileExtension = "csv";
		sequenceCounter = 0;
	}

	@Override
	public void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir) throws NeuroBException {
		// Set up training data file
		Path sourceFile;
		Path featureFile;
		Path labelFile;

		for(TrainingData td : trainingData){
			sourceFile = td.getSource();
			Path dataPath = generateTrainingDataPath(sourceFile, targetDir);
			featureFile = dataPath.resolve("features").resolve(sequenceCounter+".csv");
			labelFile = dataPath.resolve("labels").resolve(sequenceCounter+".csv");

			if(generateTrainingCSV(td, featureFile, labelFile))
				sequenceCounter++; // increase the sequence counter by one, as one file was created
		}
	}

	/**
	 * @param td
	 * @param featureFile
	 * @param labelFile
	 * @return true iff training csvs could be set up correctly
	 */
	private boolean generateTrainingCSV(TrainingData td, Path featureFile, Path labelFile) {
		// set up header for CSVs
		Path sourceFile = td.getSource();
		String headerShared = "source,"+sourceFile+"\n";
		String headerFeatures = headerShared+"labels,"+labelFile+"\n";
		String headerLabels = headerShared+"features,"+featureFile+"\n";

		try{
			// write features CSV
			Files.createDirectories(featureFile.getParent());
			BufferedWriter featureCSV = Files.newBufferedWriter(featureFile);

			log.info("Writing data to CSV");
			// write header
			featureCSV.write(headerFeatures);
			// write sequence
			String sequence =
					String.join("\n",td.getFeatureString().split(",")); // comma separated to lines
			featureCSV.write(sequence);
			featureCSV.flush();
			featureCSV.close();

			// write label CSV
			BufferedWriter labelCSV = Files.newBufferedWriter(labelFile);
			// write header and label
			labelCSV.write(headerLabels);
			labelCSV.write(td.getLabelString());
			labelCSV.close();
		} catch (IOException e) {
			log.error("Could not create training CSVs", e);
			return false;
		}

		//created training files; return
		return true;
	}

	@Override
	public Path generateTrainingDataPath(Path sourceFile, Path targetDir) {
		// get source file name without file extension
		return targetDir.resolve("sequence_data");
	}

	@Override
	public void splitTrainingData(Path source, Path first, Path second, double ratio, Random rng) throws NeuroBException {
		/*
		 * Problem is that the features and labels are split up into different files,
		 * which has to be taken into account
		 *
		 * The idea here is to focus on the label data, get the index of each file, and copy
		 * the features accordingly to the label split
		 *
		 * Problem is, that the data need to be of a matching format: enumerated 1-n, with n>=1.
		 * So two new index numbers need to be kept, counting for first and second directory the
		 * files copied there respectively.
		 */

		// set up different counters
		AtomicInteger firstCounter = new AtomicInteger(0);
		AtomicInteger secondCounter = new AtomicInteger(0);

		// walk files
		try(Stream<Path> labels = Files.walk(source.resolve("labels"))){
			labels.forEach(
					l->splitTrainingSample(l,firstCounter,secondCounter,source,first,second,ratio,rng));
		} catch (IOException e) {
			log.error("Failed to load label directory", e);
			throw new NeuroBException("Could not split training data: " +
					"failed to load label directory", e);
		}
	}

	private void splitTrainingSample(Path labelFile, AtomicInteger firstCounter,
			AtomicInteger secondCounter, Path source, Path first, Path second,
			double ratio, Random rng) {
		// get RNG element
		double chance = rng.nextDouble();
		boolean copyToFirst = chance<ratio;

		// set pointers for target directory
		Path target = (copyToFirst) ? first : second;
		AtomicInteger index = (copyToFirst) ? firstCounter : secondCounter;

		// get index of current sample: path/to/file/index.csv
		int cIndex = Integer.parseInt(labelFile.getFileName().toString().split(".")[0]);

		// set new file name and increment the respective counter
		String newFileName = index.getAndIncrement()+"."+preferredFileExtension;

		try {
			// copy label file
			Files.copy(labelFile, target.resolve("labels").resolve(newFileName),
					StandardCopyOption.REPLACE_EXISTING);
			// copy feature file
			Path featureFile = source.resolve("features").resolve(cIndex+"."+preferredFileExtension);
			Files.copy(featureFile, target.resolve("features").resolve(newFileName),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			log.error("Could not split files with index {}", cIndex, e);
		}
	}

	@Override
	public void trimTrainingData(Path source, Path target) throws NeuroBException {
		// TODO
	}

	@Override
	protected TrainingAnalysisData analyseTrainingFile(Path file, TrainingAnalysisData analysisData) {
		return null;
		// TODO
	}

	@Override
	public double[] labellingFromSample(String sample) {
		// TODO
		return new double[0];
	}
}
