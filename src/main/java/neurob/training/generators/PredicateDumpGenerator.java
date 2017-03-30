package neurob.training.generators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.statespace.StateSpace;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.labelling.PredicateDumpLabelGenerator;
import neurob.training.generators.util.TrainingData;

public class PredicateDumpGenerator extends PredicateTrainingDataGenerator {
	private static final Logger log = LoggerFactory.getLogger(PredicateDumpGenerator.class);

	public PredicateDumpGenerator() {
		this(3);
	}
	
	public PredicateDumpGenerator(int samplingSize) {
		super(null, new PredicateDumpLabelGenerator(samplingSize));
		preferredFileExtension = "pdump";
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
			// write file name
			out.write("#source:"+sourceFile.toString());
			out.newLine();
			// write feature vector to stream
			for(TrainingData d : trainingData){
				out.write(generateOutput(d));
				out.newLine();
				out.flush();
			}
			log.info("\tDone: {}", targetFile);
		} catch (IOException e) {
			throw new NeuroBException("Could not correctly access target file: "+targetFile, e);
		}
		
		
	}
	
	protected String generateOutput(TrainingData td){
		return td.getLabelString()+":"+td.getComment();
	}
	
	@Override
	protected TrainingData setUpTrainingData(String predicate, Path source, StateSpace ss) throws NeuroBException {
		return new TrainingData(null, lg.generateLabelling(predicate, ss), source, predicate);
	}

}
