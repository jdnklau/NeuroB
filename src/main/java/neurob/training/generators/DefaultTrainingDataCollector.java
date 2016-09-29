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

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
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
import neurob.core.features.FeatureCollector;
import neurob.core.features.FeatureData;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.helpers.PredicateCollector;
import neurob.training.generators.interfaces.TrainingDataCollector;

public class DefaultTrainingDataCollector implements TrainingDataCollector {
	private FeatureCollector fc;
	private Api api;
	private Logger logger = Logger.getLogger(TrainingSetGenerator.class.getName());

	@Inject
	public DefaultTrainingDataCollector() {
		fc = new FeatureCollector();
		
		api = Main.getInjector().getInstance(Api.class);

	}
	
	@Override
	public int getNumberOfFeatures() {return FeatureData.featureCount;}
	@Override
	public int getNumberOfLabels() {return 3;}
	
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
		ArrayList<String> formulas = new ArrayList<String>();
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
		String properties = String.join(" & ", predc.getProperties());
		String invariants = String.join(" & ", predc.getInvariants());
		
		// check for empty formulas
		if(properties.isEmpty()){
			logger.info("\tNo properties found. Using TRUE=TRUE.");
			properties = "TRUE = TRUE";
		}
		if(invariants.isEmpty()){
			logger.info("\tNo invariants found. Using TRUE=TRUE.");
			invariants = "TRUE = TRUE";
		}
		
		String negInvariants = "not("+invariants+")";
		String propsAndInvs = String.join(" & ", properties, invariants);
		String propsAndNegInvs = String.join(" & ", properties, negInvariants); // properties and negated invariants
		
		// Add formulas
		formulas.add(invariants); // invariants
		formulas.add(propsAndInvs); // properties & invariants
		
		// guards
		for(ArrayList<String> guards : predc.getGuards()){
			String guard = String.join(" & ", guards);
			
			// only continue if the guards are nonempty
			if(guard.isEmpty()){
				continue;
			}
			
			String negGuard = "not("+guard+")";

			String propsAndGuard = String.join(" & ", properties, guard);
			String propsAndNegGuard = String.join(" & ", properties, negGuard);
			
			formulas.add(propsAndInvs + " & " + guard); // events active w/o violating invariants
			formulas.add(propsAndInvs + " => " + guard); // events usable with unviolated invariants

			formulas.add(propsAndNegInvs + " & " + guard); // events active despite invariant violation
			formulas.add(propsAndNegInvs + " => " + guard); // events usable despite invariant violation
			
			formulas.add(propsAndInvs + " & " + negGuard); // events not active w/o violating invariants
			formulas.add(propsAndInvs + " => " + negGuard); // events not usable with unviolated invariants

			formulas.add(propsAndNegInvs + " & " + negGuard); // events not active despite invariant violation
			formulas.add(propsAndNegInvs + " => " + negGuard); // events not usable despite invariant violation

			formulas.add(propsAndGuard + " => "+ invariants); // events only usable w/o invariant violation
			formulas.add(propsAndGuard + " => "+ negInvariants); // events only usable with invariant violation
			
			formulas.add(propsAndNegGuard + " => "+ invariants); // events never usable w/o invariant violation
			formulas.add(propsAndNegGuard + " => "+ negInvariants); // events never usable with invariant violation
		}
		logger.info("\tGenerated "+formulas.size()+" formulas to solve.");
		
		// generate data per formula
		ArrayList<String> results = new ArrayList<String>();
		int count = formulas.size();
		int curr = 1;
		for( String formula : formulas) {
			logger.info("\tSolving formula "+(curr++)+"/"+count+"...");
			
			String res = ""; // for target vector
			
			// generate ProB command: assume conjunct of invariants is a constrained problem
			try {
				f = new ClassicalB(formula);
			} catch (Exception e) {
				logger.severe("\tCould not create command from formula "+formula+": "+e.getMessage());
				continue;
			}
			
			// solve with different solvers:
			// ProB
			logger.info("\tSolving with ProB...");
			cmd = new CbcSolveCommand(f);
			res += evaluateCommandExecution(ss, cmd, formula);
			
			// KodKod
			res += ","; // Delimiter
			logger.info("\tSolving with KodKod...");
			cmd = new CbcSolveCommand(f);
			res += evaluateCommandWithSolver(ss, "KODKOD", cmd, formula);
			
			// SMT
			res += ","; // Delimiter
			logger.info("\tSolving with ProB/Z3...");
			cmd = new CbcSolveCommand(f);
			res += evaluateCommandWithSolver(ss, "SMT_SUPPORTED_INTERPRETER", cmd, formula);
			
			f.getAst().apply(fc); // Feature collector should reset itself at each apply call
			
			// add result
			results.add(fc.getFeatureData().toString()+":"+res); // features:labels vector
			
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
		} catch (InterruptedException e) {
			logger.warning("\tExecution interrupted: "+e.getMessage());
		} catch (ExecutionException e) {
			logger.warning("\tExecution interrupted: "+e.getMessage());
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
