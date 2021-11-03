package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
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

import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class Backend {

    protected final long timeOutValue;
    protected final TimeUnit timeOutUnit;
    protected final BPreferences preferences;

    /** Default time out set for predicate evaluation. */
    public static final long defaultTimeOut = 2500L;
    /** Unit of defaultTimeout */
    public static final TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;

    private static final Logger log =
            LoggerFactory.getLogger(Backend.class);
    boolean isClpfdSetExplicity;
    boolean isSmtSetExplicity;

    /**
     * Sets the time out to the defaults if no TIME_OUT preference is present.
     *
     * @param preferences Preferences to be set
     *
     * @see #defaultTimeOut
     * @see #defaultTimeUnit
     */
    public Backend(BPreference... preferences) {
        this(defaultTimeOut, defaultTimeUnit, preferences);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     * @param preferences Further preferences to be set
     */
    public Backend(long timeOutValue, TimeUnit timeOutUnit, BPreference... preferences) {

        // Create timeout preference for backend
        Long timeoutInMs = timeOutUnit.toMillis(timeOutValue);
        BPreference timeout = BPreference.set("TIME_OUT", timeoutInMs.toString());

        SortedMap<String, BPreference> prefMap = new TreeMap<>();
        prefMap.put(timeout.getName(), timeout);

        /*
           Note: In ProB2 the CbcSolveCommand calls ProB's cbc_solve_with_opts (prob2_interface.pl)
           which itself always assumes SMT and CLPFD options.
           As we are interested in the backends being as pure as possible, we now assume the opposite.
           These preferences should be overridden by any explicit values set for them.
         */
        prefMap.put("SMT", BPreference.set("SMT", "FALSE"));
        prefMap.put("CLPFD", BPreference.set("CLPFD", "FALSE"));

        this.isSmtSetExplicity = false;
        this.isClpfdSetExplicity = false;

        Arrays.stream(preferences)
                .forEach(p -> {
                    if ("SMT".equals(p.getName())) {
                        this.isSmtSetExplicity = true;
                    }
                    if ("CLPFD".equals(p.getName())) {
                        this.isClpfdSetExplicity = true;
                    }
                    prefMap.put(p.getName(), p);
                });

        this.preferences = new BPreferences(prefMap);

        // When the preferences contained a TIME_OUT, it should override the timeOutValue and Unit
        BPreference newestTimeout = this.preferences.get("TIME_OUT");
        this.timeOutValue = Long.valueOf(newestTimeout.getValue());
        this.timeOutUnit = TimeUnit.MILLISECONDS;

    }

    /**
     * Sets the time out to the defaults if no TIME_OUT preference is present.
     *
     * @param preferences Preferences to be set
     */
    public Backend(BPreferences preferences) {
        this(defaultTimeOut, defaultTimeUnit, preferences);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     * @param preferences Further preferences to be set
     */
    public Backend(long timeOutValue, TimeUnit timeOutUnit, BPreferences preferences) {
        this(timeOutValue, timeOutUnit, preferences.stream().toArray(BPreference[]::new));
    }

    /**
     * @return Comprehensible and uniquely identifying name of the backend.
     */
    abstract public String getName();

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

    public BPreferences getPreferences() {
        String[] without;
        if (!isSmtSetExplicity && !isClpfdSetExplicity) {
            without = new String[]{"SMT", "CLPFD"};
        } else if (!isSmtSetExplicity) {
            without = new String[]{"SMT"};
        } else if (!isClpfdSetExplicity) {
            without = new String[]{"CLPFD"};
        } else {
            without = new String[0];
        }
        return preferences.without(without);
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
        return !solvePredicateUntimed(predicate, bMachine).getAnswer().equals(Answer.UNKNOWN);
    }

    public TimedAnswer solvePredicate(BPredicate predicate, MachineAccess access)
            throws FormulaException {
        return solvePredicate(predicate, access, getTimeOutValue(), getTimeOutUnit());
    }

    public TimedAnswer solvePredicate(BPredicate predicate, MachineAccess access,
            Long timeout, TimeUnit timeUnit) throws FormulaException {
        // Set up thread for timeout check
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<AnnotatedAnswer> futureRes = executor.submit(
                () -> solvePredicateUntimed(predicate, access));

        // Start thread and check for errors
        AnnotatedAnswer answer;
        Long start, duration;
        log.trace("{}: Deciding predicate {}", this.toString(), predicate);
        start = System.nanoTime(); // start measuring time
        try {
            answer = futureRes.get(timeout, timeUnit);
        } catch (IllegalStateException e) {
            access.sendInterrupt();
            // Forward the Illegal State, we really want to know when this happens.
            throw e;
        } catch (ProBError e) {
            access.sendInterrupt();
            answer = new AnnotatedAnswer(Answer.ERROR, "ProBError: " + e);
        } catch (TimeoutException e) {
            access.sendInterrupt();
            answer = new AnnotatedAnswer(Answer.TIMEOUT, "Timeout");
            log.warn("Timeout after {} {} for predicate {}",
                    getTimeOutValue(), getTimeOutUnit(), predicate);
        } catch (InterruptedException | ExecutionException e) {
            access.sendInterrupt();
            String message = "Execution interrupted: " + e.getMessage();
            answer = new AnnotatedAnswer(Answer.ERROR, message);
        } finally {
            executor.shutdown();
        }
        duration = System.nanoTime() - start; // stop measuring

        if (answer.getUsedCommand() != null) {
            CbcSolveCommand cmd = answer.getUsedCommand();
            // Note: Conversion to Long and nano seconds should not be a problem.
            // A long with 64 bits can store 584 years worth of nano seconds
            duration = cmd.getMilliSeconds().longValue() * 1_000_000; // Translate nano seconds.
        }
        return answer.getTimedAnswer(duration);
    }

    public AnnotatedAnswer solvePredicateUntimed(BPredicate predicate, MachineAccess access)
            throws FormulaException {
        CbcSolveCommand cmd = createCbcSolveCommand(predicate, access);

        // set preferences
        access.setPreferences(preferences);

        access.execute(cmd); // FIXME: is it possible that access is null at training set generation?

        // get value for result
        Answer res;
        String msg;
        AbstractEvalResult cmdres = cmd.getValue();
        if (cmdres instanceof EvalResult) {
            if (EvalResult.FALSE.equals(cmdres)) {
                res = Answer.INVALID;
            } else {
                res = Answer.VALID;
            }
            msg = ((EvalResult) cmdres).getValue();
        } else if (cmdres instanceof ComputationNotCompletedResult) {
            // Could neither solve nor disprove the predicate in question
            res = Answer.UNKNOWN;
            msg = ((ComputationNotCompletedResult) cmdres).getReason();
        } else {
            // Technically, this branch should be unreachable.
            throw new IllegalStateException(
                    "Unexpected output received from command execution: "
                    + cmdres.toString());
        }
        return new AnnotatedAnswer(res, msg);
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
               && this.timeOutUnit == other.getTimeOutUnit()
               && this.preferences.equals(other.preferences);
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
    public String getDescriptionString() {
        return getName() + preferences.toString();
    }

    @Override
    public int hashCode() {
        return getDescriptionString().hashCode();
    }

    @Override
    public String toString() {
        return getDescriptionString();
    }
}
