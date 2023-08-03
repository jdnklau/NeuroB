package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.prob.animator.command.CbcSolveCommand;

import java.util.concurrent.TimeUnit;

public class Z3CnsBackend extends Backend {

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit} if no TIME_OUT preference is present.
     *
     * @param preferences Preferences to be set.
     */
    public Z3CnsBackend(BPreference... preferences) {
        super(preferences);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     * @param preferences Preferences to be set.
     */
    public Z3CnsBackend(long timeOutValue, TimeUnit timeOutUnit, BPreference... preferences) {
        super(timeOutValue, timeOutUnit, preferences);
    }

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit} if no TIME_OUT preference is present.
     *
     * @param preferences Preferences to be set
     */
    public Z3CnsBackend(BPreferences preferences) {
        super(preferences);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     * @param preferences Preferences to be set.
     */
    public Z3CnsBackend(long timeOutValue, TimeUnit timeOutUnit, BPreferences preferences) {
        super(timeOutValue, timeOutUnit, preferences);
    }

    @Override
    public CbcSolveCommand.Solvers toCbcEnum() {
        return CbcSolveCommand.Solvers.Z3CNS;
    }

    @Override
    public String getName() {
        return "Z3Cns";
    }
}
