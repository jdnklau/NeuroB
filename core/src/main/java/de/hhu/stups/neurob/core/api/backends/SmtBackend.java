package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.prob.animator.command.CbcSolveCommand;

import java.util.concurrent.TimeUnit;

/**
 * ProB's SMT supported interpreter backend, consisting of the
 * ProB+Z3 integration.
 */
public class SmtBackend extends Backend {

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit} if no TIME_OUT preference is present.
     *
     * @param preferences Preferences to be set.
     */
    public SmtBackend(BPreference... preferences) {
        super(preferences);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     * @param preferences Preferences to be set.
     */
    public SmtBackend(long timeOutValue, TimeUnit timeOutUnit, BPreference... preferences) {
        super(timeOutValue, timeOutUnit, preferences);
    }

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit} if no TIME_OUT preference is present.
     *
     * @param preferences Preferences to be set
     */
    public SmtBackend(BPreferences preferences) {
        super(preferences);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     * @param preferences Preferences to be set.
     */
    public SmtBackend(long timeOutValue, TimeUnit timeOutUnit, BPreferences preferences) {
        super(timeOutValue, timeOutUnit, preferences);
    }

    @Override
    public CbcSolveCommand.Solvers toCbcEnum() {
        return CbcSolveCommand.Solvers.SMT_SUPPORTED_INTERPRETER;
    }

    @Override
    public String getName() {
        return "SMT_SUPPORTED_INTERPRETER";
    }
}
