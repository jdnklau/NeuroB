package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.command.CbcSolveCommand.Solvers;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.exception.ProBError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
     * @param bMachine Access to the B machine the predicate gets decided over
     *
     * @return
     */
    public Boolean isDecidable(BPredicate predicate, MachineAccess bMachine)
            throws FormulaException {
        return isDecidable(predicate, bMachine,
                getTimeOutValue(), getTimeOutUnit());
    }

    /**
     * Checks if the predicate given is decidable or not by the given solver
     * with respect to the time out specified in the constructor.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate gets decided over
     *
     * @return
     */
    public Boolean isDecidable(String predicate, MachineAccess bMachine)
            throws FormulaException {
        return isDecidable(BPredicate.of(predicate), bMachine,
                getTimeOutValue(), getTimeOutUnit());
    }

    /**
     * Checks if the predicate given is decidable or not by the given solver
     * with respect to the time out specified in the constructor.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate gets decided over
     * @param timeOutValue Time until the backend shall time out
     * @param timeOutUnit Unit of the time out
     *
     * @return
     */
    public Boolean isDecidable(BPredicate predicate, MachineAccess bMachine,
            Long timeOutValue, TimeUnit timeOutUnit) throws FormulaException {
        // True if it can be decided in a non-negative time
        return measureEvalTime(predicate, bMachine,
                timeOutValue, timeOutUnit) >= 0;
    }

    /**
     * Checks if the predicate given is decidable or not by the given solver
     * with respect to the time out specified in the constructor.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate gets decided over
     * @param timeOutValue Time until the backend shall time out
     * @param timeOutUnit Unit of the time out
     *
     * @return
     */
    public Boolean isDecidable(String predicate, MachineAccess bMachine,
            Long timeOutValue, TimeUnit timeOutUnit) throws FormulaException {
        return isDecidable(BPredicate.of(predicate), bMachine,
                timeOutValue, timeOutUnit);
    }

    /**
     * Measures time needed to decide whether the predicate is decidable or not.
     *
     * @param predicate The predicate to decide
     * @param bMachine Access to the B machine the predicate gets decided over
     *
     * @return Time needed in nano seconds or -1 if it could not be decided
     *
     * @throws FormulaException
     */
    public Long measureEvalTime(BPredicate predicate, MachineAccess bMachine)
            throws FormulaException {
        return measureEvalTime(predicate, bMachine,
                getTimeOutValue(), getTimeOutUnit());
    }

    /**
     * Measures time needed to decide whether the predicate is decidable or not.
     *
     * @param predicate The predicate to decide
     * @param bMachine Access to the B machine the predicate gets decided over
     *
     * @return Time needed in nano seconds or -1 if it could not be decided
     *
     * @throws FormulaException
     */
    public Long measureEvalTime(String predicate, MachineAccess bMachine)
            throws FormulaException {
        return measureEvalTime(BPredicate.of(predicate), bMachine);
    }

    /**
     * Measures time needed to decide whether the predicate is decidable or not.
     *
     * @param predicate The predicate to decide
     * @param bMachine Access to the B machine the predicate gets decided over
     * @param timeOutValue Time until the backend shall time out
     * @param timeOutUnit Unit of the time out
     *
     * @return Time needed in nano seconds or -1 if it could not be decided
     *
     * @throws FormulaException
     */
    public Long measureEvalTime(BPredicate predicate, MachineAccess bMachine,
            Long timeOutValue, TimeUnit timeOutUnit)
            throws FormulaException {
        // Set up thread for timeout check
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> futureRes = executor.submit(
                () -> decidePredicate(predicate, bMachine));

        // Start thread and check for errors
        Boolean isDecidable;
        Long start, duration;
        try {
            log.trace("{}: Deciding predicate {}", this.toString(), predicate);
            start = System.nanoTime(); // start measuring time
            isDecidable = futureRes.get(timeOutValue, timeOutUnit);
            duration = System.nanoTime() - start; // stop measuring
        } catch (IllegalStateException e) {
            bMachine.sendInterrupt();
            throw e;
        } catch (ProBError e) {
            bMachine.sendInterrupt();
            throw new FormulaException(
                    "ProBBackend encountered Problems with " + predicate, e);
        } catch (TimeoutException e) {
            bMachine.sendInterrupt();
            log.warn("Timeout after {} {} for predicate {}",
                    getTimeOutValue(), getTimeOutUnit(), predicate);
            return -1L;
        } catch (InterruptedException | ExecutionException e) {
            bMachine.sendInterrupt();
            throw new FormulaException(
                    "Execution interrupted: " + e.getMessage(), e);
        } finally {
            executor.shutdown();
        }

        return (isDecidable) ? duration : -1;
    }

    /**
     * Measures time needed to decide whether the predicate is decidable or not.
     *
     * @param predicate The predicate to decide
     * @param bMachine Access to the B machine the predicate gets decided over
     * @param timeOutValue Time until the backend shall time out
     * @param timeOutUnit Unit of the time out
     *
     * @return Time needed in nano seconds or -1 if it could not be decided
     *
     * @throws FormulaException
     */
    public Long measureEvalTime(String predicate, MachineAccess bMachine,
            Long timeOutValue, TimeUnit timeOutUnit)
            throws FormulaException {
        return measureEvalTime(BPredicate.of(predicate), bMachine,
                timeOutValue, timeOutUnit);
    }

    /**
     * Tries to decide the given predicate in the given B machine over
     * this backend.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate gets decided over
     *
     * @return Whether or not backend could decide the predicate.
     *
     * @throws FormulaException
     */
    public Boolean decidePredicate(String predicate, MachineAccess bMachine)
            throws FormulaException {
        return decidePredicate(BPredicate.of(predicate), bMachine);
    }

    /**
     * Tries to decide the given predicate in the given B machine over
     * this backend.
     *
     * @param predicate
     * @param bMachine Access to the B machine the predicate gets decided over
     *
     * @return Whether or not backend could decide the predicate.
     *
     * @throws FormulaException
     */
    public Boolean decidePredicate(BPredicate predicate, MachineAccess bMachine)
            throws FormulaException {
        Boolean res;
        CbcSolveCommand cmd = createCbcSolveCommand(predicate, bMachine);

        bMachine.execute(cmd);

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

    public CbcSolveCommand createCbcSolveCommand(BPredicate predicate,
            MachineAccess bMachine) throws FormulaException {
        IBEvalElement formula = generateBFormula(predicate, bMachine);
        return new CbcSolveCommand(formula, toCbcEnum());
    }

    /**
     * Creates an {@link IBEvalElement} for command creation for ProB2 with
     * respect to the machine type.
     * <p>
     * If you are using a state space of a B machine, it is advised to use
     * {@link #generateBFormula(BPredicate, MachineAccess)}
     * instead
     *
     * @param predicate Predicate to create an evaluation element from
     * @param mt Machine type the command should get parsed in
     *
     * @return
     *
     * @see #generateBFormula(BPredicate, MachineAccess)
     */
    public static IBEvalElement generateBFormula(BPredicate predicate,
            MachineType mt) throws FormulaException {
        IBEvalElement cmd;
        try {
            switch (mt) {
                case EVENTB:
                    cmd = new EventB(predicate.toString(), FormulaExpand.EXPAND);
                    break;
                default:
                case CLASSICALB:
                    cmd = new ClassicalB(predicate.toString(), FormulaExpand.EXPAND);
            }
        } catch (Exception e) {
            throw new FormulaException("Could not translate to IBEvalElement "
                                       + "from formula " + predicate, e);
        }
        return cmd;
    }

    /**
     * Creates an {@link IBEvalElement} for command creation for ProB2 with
     * respect to a given B machine.
     * {@link #generateBFormula(BPredicate, MachineType)}.
     *
     * @param predicate Predicate to create an evaluation element from
     * @param bMachine Access to the B machine over which the eval element will be created
     *
     * @return
     *
     * @throws FormulaException
     * @see #generateBFormula(BPredicate, MachineType)
     */
    public static IBEvalElement generateBFormula(BPredicate predicate,
            MachineAccess bMachine) throws FormulaException {
        try {
            return (IBEvalElement) bMachine.parseFormula(predicate);
        } catch (Exception e) {
            throw new FormulaException("Could not translate to IBEvalElement "
                                       + "from predicate " + predicate, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        // Needs to be a backend at least
        if (!(o instanceof Backend)) {
            return false;
        }

        // Needs to be same backend
        if (!this.getClass().equals(o.getClass())) {
            return false;
        }

        Backend other = (Backend) o;

        return this.timeOutValue == other.getTimeOutValue()
               && this.timeOutUnit == other.getTimeOutUnit();
    }

    /**
     * Returns a string that states the backend's name and any settings made
     * to it, so two instances of the same backend can be easily distinguished
     * as differently configured by comparing this string.
     * <p>
     * For two backends b1 and b2 should hold:
     * <pre>{@code b1.equals(b2) <=> b1.getDescriptionString().equals(b2.getDescriptionString())}</pre>
     *
     * @return String describing the backend
     */
    abstract public String getDescriptionString();

    @Override
    public int hashCode() {
        return getDescriptionString().hashCode();
    }

    @Override
    public String toString() {
        return getDescriptionString();
    }
}
