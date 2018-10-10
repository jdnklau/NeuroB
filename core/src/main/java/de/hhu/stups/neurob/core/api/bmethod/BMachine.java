package de.hhu.stups.neurob.core.api.bmethod;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstraction of B machines as type.
 * <p>
 * By using {@link #getMachineAccess()} the machine access can directly be
 * accessed.
 */
public class BMachine implements BElement {

    private final Path location;
    private final MachineType machineType;
    private MachineAccess machineAccess = null;

    public BMachine(Path location) {
        this.location = location;

        this.machineType = MachineType.predictTypeFromLocation(location);
    }

    public BMachine(String location) {
        this(Paths.get(location));
    }

    public Path getLocation() {
        return location;
    }

    /**
     * Accesses the B machine, loading it's state space internally.
     * <p>
     * This is a lazy operation: If the access was already established and
     * is not yet closed, the same instance will be returned every time.
     *
     * @return
     *
     * @throws MachineAccessException
     */
    public MachineAccess getMachineAccess() throws MachineAccessException {
        if (machineAccess == null) {
            // load machine
            machineAccess = new MachineAccess(location);
        }

        return machineAccess;
    }

    /**
     * Closes the access to the machine.
     */
    public void closeMachineAccess() {
        if (machineAccess != null) {
            machineAccess.close();
            machineAccess = null;
        }
    }

    @Override
    public String toString() {
        return location + "[" + machineType + "]";
    }
}
