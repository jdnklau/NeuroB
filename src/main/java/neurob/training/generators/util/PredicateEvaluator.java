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
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;

public class PredicateEvaluator {
	private static long timeOutValue = 20L;
	private static TimeUnit timeUnit = TimeUnit.SECONDS;

	public static void setTimeOut(long value, TimeUnit unit){
		timeOutValue = value;
		timeUnit = unit;
	}

	public static long getTimeOutValue(){ return timeOutValue; }
	public static TimeUnit getTimeOutUnit(){ return timeUnit; }

	/**
	 * Checks if the formula given is decidable or not by the given solver.
	 * @param stateSpace
	 * @param solver The solver to use
	 * @param formula
	 * @return
	 * @throws NeuroBException
	 */
	public static boolean isDecidableWithSolver(StateSpace stateSpace, SolverType solver, IBEvalElement formula) throws NeuroBException{
		boolean decidable = false;

		// Check decidability
		decidable = PredicateEvaluator.evaluateCommandExecution(stateSpace, solver, formula);

		return decidable;
	}

	/**
	 * Measures time needed to decide whether the formula is decidable or not.
	 * @param stateSpace The StateSpace the predicate gets decided in
	 * @param solver The solver to use
	 * @param formula The Formula to decide
	 * @return Time needed in nano seconds or -1 if it could not be decided
	 * @throws NeuroBException
	 */
	public static long getCommandExecutionTimeBySolverInNanoSeconds(StateSpace stateSpace, SolverType solver, IBEvalElement formula) throws NeuroBException{
		long start = System.nanoTime();
		boolean isDecidable = evaluateCommandExecution(stateSpace, solver, formula);
		long duration = System.nanoTime()-start;

		return (isDecidable) ? duration : -1;
	}

	/**
	 * Checks whether the given formula is solvable with the given state space object, or not.
	 * @param stateSpace
	 * @param solver The solver to use
	 * @param formula
	 * @return
	 * @throws NeuroBException
	 */
	private static boolean evaluateCommandExecution(StateSpace stateSpace, SolverType solver, IBEvalElement formula) throws NeuroBException {
		// Set up thread for timeout check
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> futureRes = executor.submit(() -> {
			Boolean res = false;
//			EvaluateFormulaCommand cmd = new EvaluateFormulaCommand(formula, "0");
			CbcSolveCommand cmd = new CbcSolveCommand(formula, solver.toCbcSolveCommandEnum());

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
				throw new IllegalStateException("Unexpected output recieved from command execution: "+cmdres.toString());
			}
			return res;
		});

		// actually check for timeout
		Boolean res = false;
		try {
			res = futureRes.get(timeOutValue, timeUnit);
		} catch (IllegalStateException e) {
			throw e;
		} catch (ProBError e) {
			stateSpace.sendInterrupt();
			throw new NeuroBException("ProB encountered Problems with "+formula, e);
		} catch (TimeoutException e) {
			// Timeout
			stateSpace.sendInterrupt();
			throw new NeuroBException("Timeouted after "+ timeOutValue +" "+timeUnit+".", e);
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
