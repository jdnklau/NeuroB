package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;

import java.util.concurrent.TimeUnit;

public class KodkodBackend extends Backend {

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit}.
     */
    public KodkodBackend() {
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     */
    public KodkodBackend(long timeOutValue, TimeUnit timeOutUnit) {
        super(timeOutValue, timeOutUnit);
    }

    @Override
    public CbcSolveCommand.Solvers toCbcEnum() {
        return CbcSolveCommand.Solvers.KODKOD;
    }

    @Override
    public String getDescriptionString() {
        String description =
                "Kodkod, timeout: " + timeOutValue + timeOutUnit;
        return description;
    }
}
