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
import de.prob.statespace.StateSpace;

public class PredicateEvaluator {
	
	public static boolean isDecidableWithSolver(StateSpace stateSpace, String solverPreference, ClassicalB formula){
		boolean decidable = false;
		
		try {
			// Set solver preference
			stateSpace.execute(new SetPreferenceCommand(solverPreference, "true")); // turn SMT on
			
			// Check decidability
			decidable = PredicateEvaluator.evaluateCommandExecution(stateSpace, formula);
			
			// turn of solver preference
			stateSpace.execute(new SetPreferenceCommand(solverPreference, "false")); // and turn it off again
		} catch(Exception e){
			throw new IllegalStateException("Could not correctly set solver to use.", e);
		}
		
		return decidable;
	}

	public static boolean evaluateCommandExecution(StateSpace stateSpace, ClassicalB formula) {
		// Set up thread for timeout check
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> futureRes = executor.submit(() -> {
			Boolean res = false;
			try {
				// Set up command to solve
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
			} catch(Exception e) {
				// catch block is intended to catch invariants where ProB encounters problems with
				// logger.warning("\tAt "+formula+":\t"+e.getMessage());
				stateSpace.sendInterrupt();
			}
			return res;
		});
		
		// actually check for timeout
		Boolean res = false;
		try {
			res = futureRes.get(20L, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			// Timeout
			// logger.warning("\tTimeouted after 20 seconds.");
			stateSpace.sendInterrupt();
		} catch (InterruptedException e) {
			// logger.warning("\tExecution interrupted: "+e.getMessage());
			stateSpace.sendInterrupt();
		} catch (ExecutionException e) {
			// logger.warning("\tExecution interrupted: "+e.getMessage());
			stateSpace.sendInterrupt();
		}
		
		executor.shutdown();
		return res.booleanValue();
	}

}
