package de.hhu.stups.neurob.core.api;

import de.prob.model.eventb.EventBModel;
import de.prob.statespace.StateSpace;

public enum MachineType {
    CLASSICALB,
    EVENTB;

    /**
     * Loads the {@link MachineType} associated with the given StateSpace.
     *
     * @param ss State space to load formalism from
     *
     * @return
     */
    public static MachineType getTypeFromStateSpace(StateSpace ss) {
        if (ss.getModel() instanceof EventBModel) {
            return EVENTB;
        }
        return CLASSICALB;
    }
}
