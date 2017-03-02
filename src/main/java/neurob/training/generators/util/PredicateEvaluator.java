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
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.StateSpace;
import neurob.exceptions.NeuroBException;

public class PredicateEvaluator {
	
	/**
	 * Checks if the formula given is decidable or not.
	 * <p>
	 * For this, the state space will be manipulated
	 * by querying the given solverPreference as {@link SetPreferenceCommand} with the value  "true", then turning it off again 
	 * after the computation is done. 
	 * @param stateSpace
	 * @param solverPreference
	 * @param formula
	 * @return
	 * @throws NeuroBException
	 * @see {@link #evaluateCommandExecution(StateSpace, IBEvalElement)}
	 */
	public static boolean isDecidableWithSolver(StateSpace stateSpace, String solverPreference, IBEvalElement formula) throws NeuroBException{
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
	
	/**
	 * Measures time needed to decide whether the formula is decidable or not. 
	 * <p>
	 * The state space will be manipulated
	 * by querying the given solverPreference as {@link SetPreferenceCommand} with the value  "true", then turning it off again 
	 * after the computation is done. 
	 * @param stateSpace The StateSpace the predicate gets decided in
	 * @param formula The Formula to decide
	 * @return Time needed in nano seconds or -1 if it could not be decided
	 * @throws NeuroBException
	 * @see {@link #getCommandExecutionTimeInNanoSeconds(StateSpace, IBEvalElement)}
	 */
	public static long getCommandExecutionTimeBySolverInNanoSeconds(StateSpace stateSpace, String solverPreference, IBEvalElement formula) throws NeuroBException{
		long time = -1;
		
		try {
			// Set solver preference
			stateSpace.execute(new SetPreferenceCommand(solverPreference, "true")); // turn SMT on
		} catch(Exception e){
			throw new IllegalStateException("Could not correctly set solver to use.", e);
		}
		
		// Check decidability
		time = PredicateEvaluator.getCommandExecutionTimeInNanoSeconds(stateSpace, formula);
			
		try {
			// turn of solver preference
			stateSpace.execute(new SetPreferenceCommand(solverPreference, "false")); // and turn it off again
		} catch (Exception e){
			throw new IllegalStateException("Could not correctly turn off the selected solver.", e);
		}
		
		return time;
	}
	
	
	/**
	 * Measures time needed to decide whether the formula is decidable or not. 
	 * @param stateSpace The StateSpace the predicate gets decided in
	 * @param formula The Formula to decide
	 * @return Time needed in nano seconds or -1 if it could not be decided
	 * @throws NeuroBException
	 * @see {@link #getCommandExecutionTimeBySolverInNanoSeconds(StateSpace, String, IBEvalElement)}
	 */
	public static long getCommandExecutionTimeInNanoSeconds(StateSpace stateSpace, IBEvalElement formula) throws NeuroBException{
		long start = System.nanoTime();
		boolean isDecidable = evaluateCommandExecution(stateSpace, formula);
		long duration = System.nanoTime()-start;
		
		return (isDecidable) ? duration : -1;
	}

	/**
	 * Checks whether the given formula is solvable with the given state space object, or not.
	 * @param stateSpace
	 * @param formula
	 * @return
	 * @throws NeuroBException
	 */
	public static boolean evaluateCommandExecution(StateSpace stateSpace, IBEvalElement formula) throws NeuroBException {
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
