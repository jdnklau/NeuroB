package neurob.training.generators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.TrainingData;
import neurob.training.splitting.TrainingSetSplitter;

public class PredicateTrainingCSVGenerator extends PredicateTrainingDataGenerator {
	private static final Logger log = LoggerFactory.getLogger(PredicateTrainingCSVGenerator.class);
	private final String csvHeader;

	public PredicateTrainingCSVGenerator(FeatureGenerator fg, LabelGenerator lg) {
		super(fg, lg);
		preferredFileExtension = "csv";
		
		
		// set up CSV header
		List<String> header = new ArrayList<>();
		// set features
		for(int i=0; i<fg.getFeatureDimension(); i++){
			header.add("Feature"+i);
		}
		// set labels
		for(int j=0; j<lg.getTrainingLabelDimension(); j++){
			header.add("Label"+j);
		}
		csvHeader = String.join(",", header);
	}

	@Override
	public void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir) throws NeuroBException {
		
		Path sourceFile = trainingData.get(0).getSource();
		Path targetFile = generateTrainingDataPath(sourceFile, targetDir);
		
		// ensure existence of target directory
		try {
			Files.createDirectories(targetFile.getParent());
		} catch (IOException e) {
			throw new NeuroBException("Could not create or access directory "+targetDir, e);
		}
		
		// open target file
		try(BufferedWriter out = Files.newBufferedWriter(targetFile)) {
			log.info("\tWriting training data...");
			// write csv header
			out.write(getCSVHeader());
			out.newLine();
			// write feature vector to stream
			for(TrainingData d : trainingData){
				out.write(d.getTrainingVectorString(lg.getProblemType()));
				out.newLine();
				out.flush();
			}
			log.info("\tDone: {}", targetFile);
		} catch (IOException e) {
			throw new NeuroBException("Could not correctly access target file: "+targetFile, e);
		}

	}
	
	/**
	 * Returns the header of the csv file that will be generated.
	 * <p>
	 * The header helps distinguishing the columns, whether they are features or labels.
	 * @return
	 */
	public String getCSVHeader(){
		return csvHeader;
	}
	
	@Override
	public void splitTrainingData(Path source, Path first, Path second, double ratio, Random rng)
			throws NeuroBException {
		TrainingSetSplitter.splitCSV(source, first, second, ratio, rng);
	}
}
