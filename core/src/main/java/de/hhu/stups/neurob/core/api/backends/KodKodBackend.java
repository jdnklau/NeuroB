package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;

import java.util.concurrent.TimeUnit;

public class KodKodBackend extends Backend {

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit}.
     */
    public KodKodBackend() {
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     */
    public KodKodBackend(long timeOutValue, TimeUnit timeOutUnit) {
        super(timeOutValue, timeOutUnit);
    }

    @Override
    public CbcSolveCommand.Solvers toCbcEnum() {
        return CbcSolveCommand.Solvers.KODKOD;
    }

    @Override
    public String toString() {
        return "KodKod";
    }
}
