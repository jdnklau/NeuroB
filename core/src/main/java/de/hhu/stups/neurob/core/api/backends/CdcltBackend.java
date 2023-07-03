package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.prob.animator.command.CbcSolveCommand;

import java.util.concurrent.TimeUnit;

/**
 * The CDCLT based backend of ProB itself.
 */
public class CdcltBackend extends Backend {

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit} if no TIME_OUT preference is present.
     *
     * @param preferences Preferences to be set.
     */
    public CdcltBackend(BPreference... preferences) {
        super(preferences);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     * @param preferences Preferences to be set.
     */
    public CdcltBackend(long timeOutValue, TimeUnit timeOutUnit, BPreference... preferences) {
        super(timeOutValue, timeOutUnit, preferences);
    }

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit} if no TIME_OUT preference is present.
     *
     * @param preferences Preferences to be set
     */
    public CdcltBackend(BPreferences preferences) {
        super(preferences);
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     * @param preferences Preferences to be set.
     */
    public CdcltBackend(long timeOutValue, TimeUnit timeOutUnit, BPreferences preferences) {
        super(timeOutValue, timeOutUnit, preferences);
    }

    @Override
    public CbcSolveCommand.Solvers toCbcEnum() {
        return CbcSolveCommand.Solvers.CDCLT;
    }

    @Override
    public String getName() {
        return "CDCLT";
    }
}
