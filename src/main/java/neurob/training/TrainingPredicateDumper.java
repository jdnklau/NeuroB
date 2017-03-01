package neurob.training;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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
import de.prob.exception.ProBError;
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
 * <p>
 * The solvers are, in this order
 * <ol>
 * <li>ProB</li>
 * <li>KodKod</li>
 * <li>ProB+Z3 (SMT_SUPPORTED_INTERPRETERS)</li>
 * </ol>
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
		
		// check file extension
		String fileName = sourceFile.getFileName().toString();
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		Path fullTargetDirectory;
		Path dataFilePath;
		MachineType mt;
		if(ext.equals("mch")){
			log.info("Dumping predicates from {}", sourceFile);
    		// get full target directory
    		fullTargetDirectory = targetDir.resolve("ClassicalB").resolve(sourceFile.getParent());
			dataFilePath = fullTargetDirectory.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+pdumpExt);
			// set machine type
			mt = MachineType.CLASSICALB;
		} else if(ext.equals("eventb")){
			log.info("Dumping predicates from {}", sourceFile);
    		// get full target directory
    		fullTargetDirectory = targetDir.resolve("EventB").resolve(sourceFile.getParent());
			dataFilePath = fullTargetDirectory.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+pdumpExt);
			// set machine type
			mt = MachineType.EVENTB;
		} else {
			return;
		}
		
		// load state space
		if(isDumpAlreadyPresent(sourceFile, dataFilePath)){
			log.info("\tPredicate dump for {} is already present at {} and seems to be up to date. Doing nothing.", sourceFile, dataFilePath);
			return;
		}
		StateSpace ss;
		try {
			ss = loadStateSpace(sourceFile, mt);
		} catch (ProBError e){
			log.error("\tError at predicate dump creation: {}", e.getMessage(), e);
			return;
		} catch(IOException | ModelTranslationError e){
			throw e; // forward this
		} catch(Exception e){
			// anything else is the real problem
			log.error("\tUnexpected, critical error): {}", e.getMessage(), e);
			log.debug("\tSkipping this entry, but system may not be stable anymore.");
			return;
		}
		
		try {
			createDump(ss, dataFilePath);
		} catch (NeuroBException e) {
			log.error("\t{}", e.getMessage());
		}
		
		ss.kill();
	}
	
	private StateSpace loadStateSpace(Path file, MachineType mt) throws IOException, ModelTranslationError{
			switch(mt){
			case EVENTB:
				log.info("\tLoading EventB machine {}", file);
				return api.eventb_load(file.toString());
			default:
				log.warn("\tUnknown value for MachineType at state space loading. Defaulting to Classical B.");
			case CLASSICALB:
				log.info("\tLoading ClassicalB machine {}", file);
				return api.b_load(file.toString());
			}
		
	}
	
	/**
	 *
	 * @param source
	 * @param target
	 * @return true if the target files already exists and is newer than the source file; false otherwise
	 */
	private boolean isDumpAlreadyPresent(Path source, Path target) throws IOException{
		// check necessity of file creation:
		// if a pdump file already exists and is newer than the machine file, 
		// then the data should be up to date
		if(Files.exists(target, LinkOption.NOFOLLOW_LINKS)){
			if(Files.getLastModifiedTime(source, LinkOption.NOFOLLOW_LINKS)
					.compareTo(Files.getLastModifiedTime(target, LinkOption.NOFOLLOW_LINKS))
				<= 0){ // last edit source file <= last edit target file -> nothing to do here
				return true;
			}
		}
		return false;
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
		
		log.info("\tGenerated {} predicates to dump into {}", formulae.size(), targetFile);
		
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
		try {
			Files.createDirectories(targetFile.getParent());
		} catch (IOException e) {
			throw new NeuroBException("Could not create target file: "+targetFile, e);
		}
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
		
		log.debug("\tdumping {}", res.toString());
		
		return res.toString();
	}
}
