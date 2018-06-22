package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;

import java.util.concurrent.TimeUnit;

public class Z3Backend extends Backend {

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit}.
     */
    public Z3Backend() {
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     */
    public Z3Backend(long timeOutValue, TimeUnit timeOutUnit) {
        super(timeOutValue, timeOutUnit);
    }

    @Override
    public CbcSolveCommand.Solvers toCbcEnum() {
        return CbcSolveCommand.Solvers.Z3;
    }

    @Override
    public String toString() {
        return "Z3";
    }
}
