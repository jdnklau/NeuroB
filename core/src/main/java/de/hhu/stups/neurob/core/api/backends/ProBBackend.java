package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;

import java.util.concurrent.TimeUnit;

/**
 * The ProB constriant-based checker, a disprover.
 */
public class ProBBackend extends Backend {

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit}.
     */
    public ProBBackend() {
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     */
    public ProBBackend(long timeOutValue, TimeUnit timeOutUnit) {
        super(timeOutValue, timeOutUnit);
    }

    @Override
    public CbcSolveCommand.Solvers toCbcEnum() {
        return CbcSolveCommand.Solvers.PROB;
    }

    @Override
    public String toString() {
        return "ProB";
    }
}
