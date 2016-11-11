package neurob.training.generators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.Main;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.model.representation.AbstractElement;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import neurob.core.features.CodePortfolio;
import neurob.core.features.PredicateFeatureCollector;
import neurob.core.features.PredicateFeatureData;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.interfaces.TrainingDataCollector;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateCollector;

public class SolverClassificationDataCollectorWithCodePortfolio implements TrainingDataCollector {
	private PredicateFeatureCollector fc;
	private Api api;
	private Logger logger = Logger.getLogger(TrainingSetGenerator.class.getName());
	private boolean solveProB;
	private boolean solveKodKod;
	private boolean solveProBZ3;
	private int labels;
	private int dim;
	
	@Inject
	public SolverClassificationDataCollectorWithCodePortfolio(int dimension) {
		fc = new PredicateFeatureCollector();
		api = Main.getInjector().getInstance(Api.class);
		solveProB = true;
		solveKodKod = true;
		solveProBZ3 = true;
		labels = 3;
		dim = dimension;
	}
	
	@Inject
	public SolverClassificationDataCollectorWithCodePortfolio(int dimension, boolean useProB, boolean useKodKod, boolean useProBZ3){
		fc = new PredicateFeatureCollector();
		api = Main.getInjector().getInstance(Api.class);
		solveProB = useProB;
		solveKodKod = useKodKod;
		solveProBZ3 = useProBZ3;
		labels = 0;
		if(solveProB) ++labels;
		if(solveKodKod) ++labels;
		if(solveProBZ3) ++labels;
		dim = dimension;
	}
	
	@Override
	public int getNumberOfFeatures() {return PredicateFeatureData.featureCount;}
	@Override
	public int getNumberOfLabels() {
		return labels;
	}
	
	/**
	 * Set the logger to a different one
	 * @param l
	 */
	@Override
	public void setLogger(Logger l){
		logger = l;
	}

	@Override
	public void collectTrainingData(Path source, Path target) throws IOException, BException, IllegalStateException {
		// StateSpace and main component
		StateSpace ss = null;
		AbstractElement mainComp;
		// For the formula and ProB command to use
		ArrayList<String> formulae;
		ClassicalB f; // formula as EventB formula
		CbcSolveCommand cmd;
		
		// Access source file
		try{
			logger.info("\tLoading machine...");
			ss = api.b_load(source.toString());
		}catch(Exception e) {
			logger.severe("\tCould not load machine:" + e.getMessage());
			ensureStateSpaceKill(ss);
			return;
		}
		
		// Get different formulas
		mainComp = ss.getMainComponent();	// extract main component
		PredicateCollector predc = new PredicateCollector(mainComp);
		formulae = FormulaGenerator.extendedGuardFormulas(predc);
		logger.info("\tGenerated "+formulae.size()+" formulas to solve.");
		
		// generate data per formula
		ArrayList<String> results = new ArrayList<String>();
		int count = formulae.size();
		int curr = 1;
		for( String formula : formulae) {
			logger.info("\tAt "+(curr++)+"/"+count+"...");
			
			ArrayList<String>labels = new ArrayList<String>();
			
			// generate ProB command: assume conjunct of invariants is a constrained problem
			try {
				f = new ClassicalB(formula);
			} catch (Exception e) {
				logger.severe("\tCould not create command from formula "+formula+": "+e.getMessage());
				continue;
			}
			
			// solve with different solvers:
			// ProB
			if(solveProB){
				logger.info("\tSolving with ProB...");
				cmd = new CbcSolveCommand(f);
				labels.add(evaluateCommandExecution(ss, cmd, formula));
			}
				
			// KodKod
			if(solveKodKod){
				logger.info("\tSolving with KodKod...");
				cmd = new CbcSolveCommand(f);
				labels.add(evaluateCommandWithSolver(ss, "KODKOD", cmd, formula));
			}
			// SMT
			if(solveProBZ3){
				logger.info("\tSolving with ProB/Z3...");
				cmd = new CbcSolveCommand(f);
				labels.add(evaluateCommandWithSolver(ss, "SMT_SUPPORTED_INTERPRETER", cmd, formula));
			}
			
			CodePortfolio cp = new CodePortfolio(dim, formula);
			
			// add result
			String res = String.join(",", labels);
			results.add(cp+":"+res+":\""+formula+"\""); // features:labeling vector:comment
			
		}
		// close StateSpace
		ss.kill();
		
		// open target file
		BufferedWriter out = Files.newBufferedWriter(target);
		// write feature vector to stream
		logger.info("\tWriting training data...");
		for(String res : results){
			out.write(res);
			out.newLine();
			out.flush();
		}
		out.close();		
		
	}
	
	private String evaluateCommandWithSolver(StateSpace ss, String solverpref, CbcSolveCommand cmd, String formula) 
			throws IllegalStateException{
		String res ;
		
		try {
			ss.execute(new SetPreferenceCommand(solverpref, "true")); // turn SMT on
			res = evaluateCommandExecution(ss, cmd, formula);
			ss.execute(new SetPreferenceCommand(solverpref, "false")); // and turn it off again
		} catch(Exception e){
			ensureStateSpaceKill(ss);
			throw new IllegalStateException("Could not correctly set solver to use.", e);
		}
		
		return res;
	}
	
	private String evaluateCommandExecution(StateSpace ss, CbcSolveCommand cmd, String formula){
	
		// Set up thread for timeout check
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> futureRes = executor.submit(() -> {
			String res = "0";
			try {
				ss.execute(cmd);
				
				// get value for result
				AbstractEvalResult cmdres = cmd.getValue();
				if(cmdres instanceof EvalResult){
					// could solve or disprove it
					res = "1";
				} else if(cmdres instanceof ComputationNotCompletedResult){
					// Could not solve nor disprove the predicate in question
					res = "0";
				} else {
					// durr?
					throw new Exception("Unexpected output recieved from command execution.");
				}
			} catch(Exception e) {
				// catch block is intended to catch invariants where ProB encounters problems with
				logger.warning("\tAt "+formula+":\t"+e.getMessage());
				ss.sendInterrupt();
				res = "0";
			}
			return res;
		});
		
		// actually check for timeout
		String res = "0";
		try {
			res = futureRes.get(20L, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			// Timeout
			logger.warning("\tTimeouted after 20 seconds.");
			ss.sendInterrupt();
		} catch (InterruptedException e) {
			logger.warning("\tExecution interrupted: "+e.getMessage());
			ss.sendInterrupt();
		} catch (ExecutionException e) {
			logger.warning("\tExecution interrupted: "+e.getMessage());
			ss.sendInterrupt();
		}
		
		executor.shutdown();
		return res;
		
	}
	
	private void ensureStateSpaceKill(StateSpace s){
		if(s == null){
			return;
		}
		s.kill();
	}
}
