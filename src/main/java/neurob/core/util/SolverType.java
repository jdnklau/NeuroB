package neurob.core.util;

import de.prob.animator.command.CbcSolveCommand.Solvers;

public enum SolverType {
	PROB,
	KODKOD,
	Z3,
	SMT_SUPPORTED_INTERPRETER;
	
	/**
	 * Mainly used internally to translate between the different enums.
	 * @return
	 */
	public Solvers toCbcSolveCommandEnum(){
		switch(this){
		case PROB:
		default:
			return Solvers.PROB;
		case KODKOD:
			return Solvers.KODKOD;
		case Z3:
			return Solvers.Z3;
		case SMT_SUPPORTED_INTERPRETER:
			return Solvers.SMT_SUPPORTED_INTERPRETER;
		}
	}
}
