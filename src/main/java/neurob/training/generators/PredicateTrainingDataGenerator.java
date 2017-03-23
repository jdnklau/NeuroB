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
	public void collectTrainingDataFromFile(Path sourceFile, Path targetFile) throws NeuroBException {
		// determine machine type
		MachineType mt;
		String fileName = sourceFile.getFileName().toString();
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		if(ext.equals("bcm")){
			mt = MachineType.EVENTB;
			log.debug("EventB machine detected: {}", sourceFile);
		} else if(ext.equals("mch")) {
			mt = MachineType.CLASSICALB;
			log.debug("ClassicalB machine detected: {}", sourceFile);
		} else {
			log.debug("Not a machine file. Skipping: {}", sourceFile);
			return;
		}

		log.info("Generating: {} > {}", sourceFile, targetFile);
		
		// Access source file/loading state space
		StateSpace ss = null;
		log.info("\tLoading machine file {} ...", sourceFile);
		try{
			ss = loadStateSpace(sourceFile, mt);
		} catch(IOException | NeuroBException e){
			throw new NeuroBException("Could not load machine correctly: "+e.getMessage(), e);
		} catch(Exception e) {
			throw new NeuroBException("Unexpected exception encountered: "+e.getMessage(), e);
		}
		
		// For the formula and ProB command to use
		List<String> formulae = generateFormulae(new PredicateCollector(ss));
		
		log.info("\tGenerated {} formulae to solve.", formulae.size());
		
		List<String> results = generateResults(formulae, ss, sourceFile, mt);
		
		// close StateSpace
		ss.kill();
		
		// No training data to write? -> return from method
		// otherwise write to targetFile
		if(results.isEmpty()){
			log.info("\tNo training data created");
			return;
		}
		
		createTrainingDataFile(sourceFile, targetFile, results);

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
	
	protected List<String> generateResults(List<String> formulae, StateSpace ss, Path sourceFile, MachineType mt){
		// generate data per formula
		List<String> results = new ArrayList<String>();
		int count = formulae.size();
		int curr = 1;
		boolean isStateSpaceCorrupted = false; // if in the solvers at label generation something goes wrong, we might want to reload the state space
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
				String output = generateOutput(formula, ss); 
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
	
	protected String generateOutput(String predicate, StateSpace ss) throws NeuroBException{
		return fg.generateFeatureString(predicate)+":"+lg.generateLabellingString(predicate, ss)+":"+predicate;
	}

}
