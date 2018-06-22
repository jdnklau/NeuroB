package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.command.CbcSolveCommand.Solvers;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.StateSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Enum of all supported backends of ProBBackend used in NeuroB.
 */
public abstract class Backend {

    protected final long timeOutValue;
    protected final TimeUnit timeOutUnit;

    /** Default time out set for predicate evaluation. */
    public static final long defaultTimeOut = 20L;
    /** Unit of defaultTimeout */
    public static final TimeUnit defaultTimeUnit = TimeUnit.SECONDS;

    private static final Logger log =
            LoggerFactory.getLogger(Backend.class);

    /**
     * Sets the time out to the defaults.
     *
     * @see #defaultTimeOut
     * @see #defaultTimeUnit
     */
    public Backend() {
        this(defaultTimeOut, defaultTimeUnit);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     */
    public Backend(long timeOutValue, TimeUnit timeOutUnit) {
        this.timeOutValue = timeOutValue;
        this.timeOutUnit = timeOutUnit;
    }

    /**
     * Returns matching backend's {@link Solvers} enum from ProBBackend's
     * {@link CbcSolveCommand}.
     * <p>
     * Used internally for command creation.
     *
     * @return
     */
    public abstract Solvers toCbcEnum();

    public long getTimeOutValue() {
        return timeOutValue;
    }

    public TimeUnit getTimeOutUnit() {
        return timeOutUnit;
    }

    /**
     * Checks if the predicate given is decidable or not by the given solver
     * with respect to the time out specified in the constructor.
     *
     * @param predicate
     * @param stateSpace
     *
     * @return
     */
    public Boolean isDecidable(String predicate, StateSpace stateSpace)
            throws FormulaException {
        // Set up thread for timeout check
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> futureRes = executor.submit(
                () -> decidePredicate(predicate, stateSpace));

        // Start thread and check for errors
        try {
            log.debug("{}: Deciding predicate {}", this.toString(), predicate);
            return futureRes.get(getTimeOutValue(), getTimeOutUnit());
        } catch (IllegalStateException e) {
            stateSpace.sendInterrupt();
            throw e;
        } catch (ProBError e) {
            stateSpace.sendInterrupt();
            throw new FormulaException(
                    "ProBBackend encountered Problems with " + predicate, e);
        } catch (TimeoutException e) {
            stateSpace.sendInterrupt();
            log.warn("Timeout after {} {} for predicate {}",
                    getTimeOutValue(), getTimeOutUnit(), predicate);
            return false;
        } catch (InterruptedException | ExecutionException e) {
            stateSpace.sendInterrupt();
            throw new FormulaException(
                    "Execution interrupted: " + e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Measures time needed to decide whether the predicate is decidable or not.
     *
     * @param predicate The predicate to decide
     * @param stateSpace The StateSpace the predicate gets decided in
     *
     * @return Time needed in nano seconds or -1 if it could not be decided
     *
     * @throws FormulaException
     */
    public Long measureEvalTime(String predicate, StateSpace stateSpace)
            throws FormulaException {
        Long start = System.nanoTime();
        Boolean isDecidable = isDecidable(predicate, stateSpace);
        Long duration = System.nanoTime() - start;

        return (isDecidable) ? duration : -1;
    }

    /**
     * Tries to decide the given predicate in the given {@link StateSpace} over
     * this backend.
     *
     * @param predicate
     * @param stateSpace State space to decide the predicate in.
     *
     * @return Whether or not backend could decide the predicate.
     *
     * @throws FormulaException
     */
    public Boolean decidePredicate(String predicate, StateSpace stateSpace)
            throws FormulaException {
        Boolean res;
        CbcSolveCommand cmd = createCbcSolveCommand(predicate, stateSpace);

        stateSpace.execute(cmd);

        // get value for result
        AbstractEvalResult cmdres = cmd.getValue();
        if (cmdres instanceof EvalResult) {
            // could solve or disprove it
            res = true;
        } else if (cmdres instanceof ComputationNotCompletedResult) {
            // Could neither solve nor disprove the predicate in question
            res = false;
        } else {
            // Technically, this branch should be unreachable.
            throw new IllegalStateException(
                    "Unexpected output received from command execution: "
                    + cmdres.toString());
        }
        return res;
    }

    public CbcSolveCommand createCbcSolveCommand(String predicate,
            StateSpace stateSpace) throws FormulaException {
        IBEvalElement formula = generateBFormula(predicate, stateSpace);
        return new CbcSolveCommand(formula, toCbcEnum());
    }

    /**
     * Creates an {@link IBEvalElement} for command creation for ProB2 with
     * respect to the machine type.
     * <p>
     * If you are using a state space of a B machine, it is advised to use
     * {@link #generateBFormula(String, StateSpace)}
     * instead
     *
     * @param predicate Predicate to create an evaluation element from
     * @param mt Machine type the command should get parsed in
     *
     * @return
     *
     * @see #generateBFormula(String, StateSpace)
     */
     public static IBEvalElement generateBFormula(String predicate,
             MachineType mt) throws FormulaException {
        IBEvalElement cmd;
        try {
            switch (mt) {
                case EVENTB:
                    cmd = new EventB(predicate);
                    break;
                default:
                case CLASSICALB:
                    cmd = new ClassicalB(predicate);
            }
        } catch (Exception e) {
            throw new FormulaException("Could not translate to IBEvalElement "
                                       + "from formula " + predicate, e);
        }
        return cmd;
    }

    /**
     * Creates an {@link IBEvalElement} for command creation for ProB2 with
     * respect to a given StateSpace.
     * <p>
     * If no StateSpace exists, use
     * {@link #generateBFormula(String, MachineType)}.
     *
     * @param predicate Predicate to create an evaluation element from
     * @param ss StateSpace over which the eval element will be created
     *
     * @return
     *
     * @throws FormulaException
     * @see #generateBFormula(String, MachineType)
     */
     public static IBEvalElement generateBFormula(String predicate,
             StateSpace ss) throws FormulaException {
        try {
            return (IBEvalElement) ss.getModel().parseFormula(predicate);
        } catch (Exception e) {
            throw new FormulaException("Could not translate to IBEvalElement "
                                       + "from predicate " + predicate, e);
        }
    }


}
