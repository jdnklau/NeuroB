package neurob.training.generators;

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
import neurob.training.generators.util.TrainingData;

public abstract class PredicateTrainingDataGenerator extends TrainingDataGenerator {
	// logger
	private static final Logger log = LoggerFactory.getLogger(PredicateTrainingDataGenerator.class);

	public PredicateTrainingDataGenerator(FeatureGenerator fg, LabelGenerator lg) {
		super(fg, lg);
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

	/**
	 * Generates a list of formulae for training from a given predicate collector.
	 * @param predicateCollector Holds base formulae (invariants, guards) from a machine file.
	 * @return List of formulae created.
	 */
	protected List<String> generateFormulae(PredicateCollector predicateCollector){
		// Get different formulas
		List<String> formulae = FormulaGenerator.extendedGuardFormulae(predicateCollector);
		formulae.addAll(FormulaGenerator.assertionsAndTheorems(predicateCollector));
		formulae.addAll(FormulaGenerator.multiGuardFormulae(predicateCollector));
		formulae.addAll(FormulaGenerator.enablingRelationships(predicateCollector));
		formulae.addAll(FormulaGenerator.invariantPreservations(predicateCollector));

		return formulae;
	}

	/**
	 * Takes a list of formulae and calculates the respective labelling and feature representation.
	 * Returns a list of {@link TrainingData}.
	 *
	 * @param formulae List of formulae to solve
	 * @param ss StateSpace that has the corresponding machine loaded
	 * @param sourceFile Corresponding machine
	 * @return List of Features and Labels as {@link TrainingData}, built from the formulae
	 */
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

	protected TrainingData setUpTrainingData(String predicate, Path source, StateSpace ss) throws NeuroBException{
		fg.setStateSpace(ss); // set state space for feature generator
		return new TrainingData(fg.generateFeatureArray(predicate), lg.generateLabelling(predicate, ss), source, predicate);
	}

//	/**
//	 * Generates a output string from a {@link TrainingData single sample}.
//	 * This string will be written as line to the training file.
//	 * @param td
//	 * @return
//	 */
//	protected String generateOutput(TrainingData td){
//		return td.getFeatureString()+":"+td.getLabelString()+":"+td.getComment();
//	}

}
