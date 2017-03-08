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
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateCollector;

public class PredicateTrainingDataGenerator extends TrainingDataGenerator {
	// logger
	private static final Logger log = LoggerFactory.getLogger(PredicateTrainingDataGenerator.class);

	public PredicateTrainingDataGenerator(FeatureGenerator fg, LabelGenerator lg) {
		super(fg, lg);
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
		List<String> formulae;
		
		// Get different formulas
		PredicateCollector predc = new PredicateCollector(ss);
		formulae = FormulaGenerator.extendedGuardFormulae(predc);
		formulae.addAll(FormulaGenerator.assertionsAndTheorems(predc));
		formulae.addAll(FormulaGenerator.multiGuardFormulae(predc));
		formulae.addAll(FormulaGenerator.enablingRelationships(predc));
		formulae.addAll(FormulaGenerator.invariantPreservations(predc));
		// TODO: this should be implemented for convolution features, but for predicates only
		// This should be implemented after restructuring training set generation
		// into a more general format, that is not restricted to predicates only
//				// get shuffles for images
//				if(fg instanceof ConvolutionFeatures){
//					for(long i=0; i<3; i++){
//						predc.shuffleConjunctions(i);
//						formulae = FormulaGenerator.extendedGuardFormulae(predc);
//						formulae.addAll(FormulaGenerator.extendedGuardFomulaeWithInfiniteDomains(predc));
//					}
//				}
		
		log.info("\tGenerated {} formulae to solve.", formulae.size());
		
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
				results.add(fg.generateFeatureString(formula)+":"+lg.generateLabelling(formula, ss)+":\""+formula+"\"");
				corruptionCounter--; // could use new state space without corrupting it again
			} catch (NeuroBException e) {
				log.warn("\t{}", e.getMessage(), e);
			} catch (IllegalStateException e) {
				log.error("\tReached Illegal State: {}", e.getMessage(), e);
				isStateSpaceCorrupted = true;
			} catch (Exception e) {
				log.error("\tUnexpected Exception encountered: {}", e.getMessage(), e);
				isStateSpaceCorrupted = true;
			}
		}
		
		// close StateSpace
		ss.kill();
		
		// No training data to write? -> return from method
		// otherwise write to targetFile
		if(results.isEmpty()){
			log.info("\tNo training data created");
			return;
		}
		
		// open target file
		try(BufferedWriter out = Files.newBufferedWriter(targetFile)) {
			// write feature vector to stream
			log.info("\tWriting training data...");
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

}
