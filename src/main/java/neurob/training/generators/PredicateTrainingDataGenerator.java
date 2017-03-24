package neurob.training.generators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.PredicateLabelGenerator;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateCollector;
import neurob.training.generators.util.TrainingData;

public class PredicateTrainingDataGenerator extends TrainingDataGenerator {
	// logger
	private static final Logger log = LoggerFactory.getLogger(PredicateTrainingDataGenerator.class);
	// Label generator
	protected PredicateLabelGenerator lg;

	public PredicateTrainingDataGenerator(FeatureGenerator fg, PredicateLabelGenerator lg) {
		super(fg, lg);
		this.lg = lg;
	}
	
	@Override
	public List<TrainingData> collectTrainingDataFromFile(Path sourceFile, StateSpace ss)
			throws NeuroBException {
		// load formulae from predicate collector
		List<String> formulae = generateFormulae(new PredicateCollector(ss));
		log.info("\tGenerated {} formulae to solve.", formulae.size());

		// set up results
		List<TrainingData> data = generateResults(formulae, ss, sourceFile);
		
		return data;
	}

	@Override
	public void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir) throws NeuroBException {
		
		Path sourceFile = trainingData.get(0).getSource();
		Path targetFile = generateTargetFilePath(sourceFile, targetDir);
		
		// ensure existence of target directory
		try {
			Files.createDirectories(targetFile.getParent());
		} catch (IOException e) {
//			log.error("\tCould not create or access directory {}: {}", targetDir, e.getMessage(), e);
//			return;
			throw new NeuroBException("Could not create or access directory "+targetDir, e);
		}
		
		// Check if file creation is really necessary
		try {
			if(isTargetFileUpToDate(sourceFile, targetFile)){
				log.info("Target file {} already present and seems to be up to date. Skipping.", targetFile);
			}
		} catch (IOException e) {
			throw new NeuroBException("Could not correctly access source file "+sourceFile+" or target file "+targetFile, e);
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
	
	protected List<String> generateFormulae(PredicateCollector predicateCollector){
		// Get different formulas
		List<String> formulae = FormulaGenerator.extendedGuardFormulae(predicateCollector);
		formulae.addAll(FormulaGenerator.assertionsAndTheorems(predicateCollector));
		formulae.addAll(FormulaGenerator.multiGuardFormulae(predicateCollector));
		formulae.addAll(FormulaGenerator.enablingRelationships(predicateCollector));
		formulae.addAll(FormulaGenerator.invariantPreservations(predicateCollector));
		
		return formulae;
	}
	
	protected List<TrainingData> generateResults(List<String> formulae, StateSpace ss, Path sourceFile){
		// get machine type
		MachineType mt = MachineType.getTypeFromStateSpace(ss);
		
		// generate data per formula
		List<TrainingData> results = new ArrayList<>();
		int count = formulae.size();
		int curr = 1;
		boolean isStateSpaceCorrupted = false; // if something goes wrong inside the solvers at label generation, we might want to reload the state space
		int corruptionCounter = 0; // count sequential corruptions. To stop if state space corrupts to often 
		for( String formula : formulae) {
			// check state space corruption
			if(isStateSpaceCorrupted){
				corruptionCounter++;
				if(corruptionCounter <= 3){ // arbitrary choice
					log.info("\tStateSpace may be corrupted. Trying to reload...");
					//reload state space
					ss.kill();
					try {
						ss = loadStateSpace(sourceFile, mt);
						isStateSpaceCorrupted=false;
						log.info("\tRecovered StateSpace!");
					} catch (Exception e) {
						log.error("Unable to restore state space to uncorrupted state: {}", e.getMessage(), e);
						log.info("Skipping remaining formulae.");
						break;
					}
				} else {
					log.warn("StateSpace corrupted sequentially {} times. Aborting further formulae.", corruptionCounter);
					break; // get out of the loop to save at least the other formulae to file
				}
			}
			
			log.info("\tAt {}/{}...", curr++, count);
			try {
				// features:labeling vector:comment
				TrainingData output = setUpTrainingData(formula, sourceFile, ss); 
				results.add(output);
				log.debug("\tGenerated training data: {}", output);
				corruptionCounter = (corruptionCounter ==0 ? 0 : corruptionCounter-1); // could use new state space without corrupting it again
			} catch (NeuroBException e) {
				log.warn("\t{}", e.getMessage(), e);
				isStateSpaceCorrupted = true;
			} catch (IllegalStateException e) {
				log.error("\tReached Illegal State: {}", e.getMessage(), e);
				isStateSpaceCorrupted = true;
			} catch (Exception e) {
				log.error("\tUnexpected Exception encountered: {}", e.getMessage(), e);
				isStateSpaceCorrupted = true;
			}
		}
		
		return results;
	}
	
	protected void createTrainingDataFile(Path sourceFile, Path targetFile, List<String> results) throws NeuroBException{
		Path targetDirectory = targetFile.getParent();
		// ensure existence of target directory
		try {
			Files.createDirectories(targetDirectory);
		} catch (IOException e) {
			log.error("\tCould not create or access directory {}: {}", targetDirectory, e.getMessage(), e);
			return;
		}
		
		// open target file
		try(BufferedWriter out = Files.newBufferedWriter(targetFile)) {
			log.info("\tWriting training data...");
			// write file name
			out.write("#source:"+sourceFile.toString());
			out.newLine();
			// write feature vector to stream
			for(String res : results){
				out.write(res);
				out.newLine();
				out.flush();
			}
			log.info("\tDone: {}", targetFile);
		} catch (IOException e) {
			throw new NeuroBException("Could not correctly access target file: "+targetFile, e);
		}
	}
	
	protected TrainingData setUpTrainingData(String predicate, Path source, StateSpace ss) throws NeuroBException{
		return new TrainingData(fg.generateFeatureArray(predicate), lg.generateLabelling(predicate, ss), source, predicate);
	}
	
	protected String generateOutput(TrainingData td){
		return td.getFeatureString()+":"+td.getLabelString()+":"+td.getComment();
	}

}
