package neurob.training;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateCollector;
import neurob.training.generators.util.PredicateEvaluator;

/**
 * This class collects all different predicates found in
 * given machine files, including those generated by it, and creates necessary information
 * regarding possible labelling in the training generation process.
 * <p>
 * This is meant to serve a mean to reduce training set generation time.
 * <br> Each predicate found or built has already different values attached to it,
 * so in training set generation, only the features need to be computed and the desired 
 * labellings need to be picked.
 * <p>
 * The format is: {@code labels:predicate}
 * with {@code labels} being a comma separated list of labels. For each solver, the time needed to
 * decide the predicate is listed in nanoseconds, or a negative value, if it could not be decided.
 * <br> Times are the mean of three runs by default; this amount can be changed by {@link #setSamplingSize(int)}
 * 
 * @author jannik
 *
 */
public class TrainingPredicateDumper {
	private Api api;
	private final String pdumpExt = ".pdump"; // extension for predicate dump files
	private int samplingSize;
	
	private static final Logger log = LoggerFactory.getLogger(TrainingSetGenerator.class);
	
	@Inject
	public TrainingPredicateDumper(){
		api = Main.getInjector().getInstance(Api.class);
		samplingSize = 3;
	}
	
	/**
	 * Sets the sampling size for time estimation.
	 * <p>
	 * Each predicate will be solved {@code size} times, and the mean is taken.
	 * @param size
	 */
	public void setSamplingSize(int size){
		samplingSize = size;
	}
	
	/**
	 * Creates a predicate dump.
	 * <p>
	 * For each machine file found in {@code sourceDir}, a corresponding .pdump file is created
	 * in {@code targetDir}.
	 * 
	 * @param sourceDir
	 * @param targetDump
	 */
	public void createPredicateDump(Path sourceDir, Path targetDir){
		createPredicateDump(sourceDir, targetDir, null);
	}
	
	/**
	 * Creates a predicate dump.
	 * <p>
	 * For each machine file found in {@code sourceDir}, a corresponding .pdump file is created
	 * in {@code targetDir}.
	 * <p>
	 * Files that match entries in the specified {@code excludeFile} are ignored.
	 * 
	 * @param sourceDir
	 * @param targetDir
	 * @param excludeFile
	 */
	public void createPredicateDump(Path sourceDir, Path targetDir, Path excludeFile){
		log.info("Generating predicate dump for training from {} in {}", sourceDir, targetDir);
		// prepare exclude data
		ArrayList<Path> excludes = new ArrayList<Path>();
		if(excludeFile != null){
			try(Stream<String> exc = Files.lines(excludeFile)){
				excludes.addAll(
						(ArrayList<Path>) exc
							.filter(s -> !s.isEmpty())
							.map(s -> Paths.get(s)).collect(Collectors.toList()));
			} catch (IOException e) {
				log.error("Could not access exclude file: {}", e.getMessage());
			}
		}
		
		// iterate over directory
		try (Stream<Path> stream = Files.walk(sourceDir)) {
			Files.createDirectories(targetDir);
			
			stream
				.parallel() // parallel computation
				.filter(p -> !excludes.stream().anyMatch(ex -> p.startsWith(ex))) // no excluded files or directories
				.forEach(entry -> {
	            	if(Files.isRegularFile(entry)){
						try {
							createPredicateDumpFromFile(entry, targetDir);
						} catch (IOException e) {
							log.error("Could not access source file {}.", entry, e);
						} catch (ModelTranslationError e) {
							log.error("Error at model translation for file {}.", entry, e);
						}						
		            }
				});
			log.info("Finished predicate dump creation");
	    }
		catch (IOException e){
			log.error("Could not access directory {}: {}", sourceDir, e.getMessage());
		}
		log.info("******************************");
	}
	
	/**
	 * 
	 * @param sourceFile
	 * @param targetDir
	 * @throws IOException
	 * @throws ModelTranslationError
	 */
	public void createPredicateDumpFromFile(Path sourceFile, Path targetDir) throws IOException, ModelTranslationError{
		log.info("Dumping predicates from {}", sourceFile);
		// check file extension
		String fileName = sourceFile.getFileName().toString();
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		Path fullTargetDirectory;
		Path dataFilePath;
		StateSpace ss;
		if(ext.equals("mch")){
    		// get full target directory
    		fullTargetDirectory = targetDir.resolve("ClassicalB").resolve(sourceFile.getParent());
			dataFilePath = fullTargetDirectory.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+pdumpExt);
			ss = api.b_load(sourceFile.toString());
		} else if(ext.equals("eventb")){
    		// get full target directory
    		fullTargetDirectory = targetDir.resolve("EventB").resolve(sourceFile.getParent());
			dataFilePath = fullTargetDirectory.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+pdumpExt);
			ss = api.eventb_load(sourceFile.toString());
		} else {
			return;
		}
		
		try {
			createDump(ss, dataFilePath);
		} catch (NeuroBException e) {
			log.error("\t{}", e.getMessage());
		}
		
		ss.kill();
	}
	
	private void createDump(StateSpace ss, Path targetFile) throws NeuroBException{
		// For the formulae created
		ArrayList<String> formulae;
		
		// Get different formulas
		PredicateCollector predc = new PredicateCollector(ss);
		formulae = FormulaGenerator.extendedGuardFormulae(predc);
//		formulae.addAll(FormulaGenerator.extendedGuardFomulaeWithInfiniteDomains(predc));
		// NOTE: the line above, in almost all cases, simply doubles the amount of formulae without providing interesting
		// new data
		formulae.addAll(FormulaGenerator.assertionsAndTheorems(predc));
		formulae.addAll(FormulaGenerator.multiGuardFormulae(predc));
		
		log.info("\tGenerated {} predicates to dump into {}.", formulae.size(), targetFile);
		
		// generate data per formula
		ArrayList<String> results = new ArrayList<String>();
		int count = formulae.size();
		int curr = 1;
		for( String formula : formulae) {
			log.info("\tAt {}/{}...", curr++, count);
			try {
				// labelling:predicate
				results.add(createDumpResult(ss, formula));
			} catch (NeuroBException e) {
				log.error("\t{}", e.getMessage());
			} catch (IllegalStateException e) {
				log.error("\tReached Illegal State: {}", e.getMessage());
			}
		}
		
		if(results.isEmpty()){
			log.info("\tNo predicates to dump");
			return;
		}
		
		// write predicates to target file
		try(BufferedWriter out = Files.newBufferedWriter(targetFile)) {
			// write feature vector to stream
			log.info("\tDumping {} entries to {}", results.size(), targetFile);
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

	private String createDumpResult(StateSpace stateSpace, String formula) throws NeuroBException {
		StringBuilder res = new StringBuilder();
		ClassicalB pred = new ClassicalB(formula);
		
		// Check for solvers if they can decide the predicate + get the time they need
		long ProBTime = 0;
		long KodKodTime = 0;
		long ProBZ3Time = 0;
		
		// get times of generated formula
		for(int sample=0; sample<samplingSize; ++sample){
			ProBTime += PredicateEvaluator.getCommandExecutionTimeInNanoSeconds(stateSpace, pred);
			KodKodTime += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, "KODKOD", pred);
			ProBZ3Time += PredicateEvaluator.getCommandExecutionTimeBySolverInNanoSeconds(stateSpace, "SMT_SUPPORTED_INTERPRETER", pred);
		}
		
		// normalise times
		// if a solver can not decide the predicate, it should be samplingSize*(-1)/samplingSize = -1
		ProBTime /= samplingSize;
		KodKodTime /= samplingSize;
		ProBZ3Time /= samplingSize;

		res.append(ProBTime).append(",");
		res.append(KodKodTime).append(",");
		res.append(ProBZ3Time);
		
		// append formula
		res.append(":").append(formula);
		
		return res.toString();
	}
}
