package neurob.training.generators.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.exception.ProBError;
import de.prob.statespace.StateSpace;
import neurob.exceptions.NeuroBException;

public class PredicateEvaluator {
	
	public static boolean isDecidableWithSolver(StateSpace stateSpace, String solverPreference, ClassicalB formula) throws NeuroBException{
		boolean decidable = false;
		
		try {
			// Set solver preference
			stateSpace.execute(new SetPreferenceCommand(solverPreference, "true")); // turn SMT on
		} catch(Exception e){
			throw new IllegalStateException("Could not correctly set solver to use.", e);
		}
		
		// Check decidability
		decidable = PredicateEvaluator.evaluateCommandExecution(stateSpace, formula);
			
		try {
			// turn of solver preference
			stateSpace.execute(new SetPreferenceCommand(solverPreference, "false")); // and turn it off again
		} catch (Exception e){
			throw new IllegalStateException("Could not correctly turn off the selected solver.", e);
		}
		
		return decidable;
	}

	public static boolean evaluateCommandExecution(StateSpace stateSpace, ClassicalB formula) throws NeuroBException {
		// Set up thread for timeout check
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> futureRes = executor.submit(() -> {
			Boolean res = false;
			CbcSolveCommand cmd = new CbcSolveCommand(formula);
			
			stateSpace.execute(cmd);
			
			// get value for result
			AbstractEvalResult cmdres = cmd.getValue();
			if(cmdres instanceof EvalResult){
				// could solve or disprove it
				res = true;
			} else if(cmdres instanceof ComputationNotCompletedResult){
				// Could neither solve nor disprove the predicate in question
				res = false;
			} else {
				// durr?
				throw new IllegalStateException("Unexpected output recieved from command execution.");
			}
			return res;
		});
		
		// actually check for timeout
		Boolean res = false;
		try {
			res = futureRes.get(20L, TimeUnit.SECONDS);
		} catch (IllegalStateException e) {
			throw e;
		} catch (ProBError e) {
			stateSpace.sendInterrupt();
			throw new NeuroBException("ProB encountered Problems with "+formula, e);
		} catch (TimeoutException e) {
			// Timeout
			stateSpace.sendInterrupt();
			throw new NeuroBException("Timeouted after 20 seconds.", e);
		} catch (InterruptedException e) {
			stateSpace.sendInterrupt();
			throw new NeuroBException("Execution interrupted: "+e.getMessage(), e);
		} catch (ExecutionException e) {
			stateSpace.sendInterrupt();
			throw new NeuroBException("Execution interrupted: "+e.getMessage(), e);
		} finally {
			executor.shutdown();
		}
		
		return res.booleanValue();
	}

}
