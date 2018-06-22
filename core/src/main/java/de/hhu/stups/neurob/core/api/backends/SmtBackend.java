package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;

import java.util.concurrent.TimeUnit;

/**
 * ProB's SMT supported interpreter backend, consisting of the
 * ProB+Z3 integration.
 */
public class SmtBackend extends Backend {

    /**
     * Sets default time out to {@link #defaultTimeOut}
     * with unit {@link #defaultTimeUnit}.
     */
    public SmtBackend() {
    }

    /**
     * @param timeOutValue Maximum runtime
     * @param timeOutUnit
     */
    public SmtBackend(long timeOutValue, TimeUnit timeOutUnit) {
        super(timeOutValue, timeOutUnit);
    }

    @Override
    public CbcSolveCommand.Solvers toCbcEnum() {
        return CbcSolveCommand.Solvers.SMT_SUPPORTED_INTERPRETER;
    }

    @Override
    public String toString() {
        return "SMT_SUPPORTED_INTERPRETER";
    }
}
