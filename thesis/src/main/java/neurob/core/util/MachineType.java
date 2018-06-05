package neurob.core.util;

import de.prob.model.eventb.EventBModel;
import de.prob.statespace.StateSpace;

public enum MachineType {
	CLASSICALB,
	EVENTB;
	
	public static MachineType getTypeFromStateSpace(StateSpace ss){
		if(ss.getModel() instanceof EventBModel){
			return EVENTB;
		}
		return CLASSICALB;
	}
}
