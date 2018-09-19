package de.hhu.stups.neurob.core.api;

import de.prob.model.eventb.EventBModel;
import de.prob.statespace.StateSpace;

import java.nio.file.Path;

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

    /**
     * Predicts the machine type of the specified source by its file extension.
     *
     * @param location Path to a B machine file
     *
     * @return
     */
    public static MachineType predictTypeFromLocation(Path location) {
        if (location.toString().endsWith(".bcm")) {
            return MachineType.EVENTB;
        }
        return MachineType.CLASSICALB;
    }
}
