package de.hhu.stups.neurob.core.api.bmethod;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstraction of B machines as type.
 * <p>
 * By using {@link #spawnMachineAccess()} the machine access can directly be
 * accessed.
 */
public class BMachine implements BElement {

    private final Path location;
    private final MachineType machineType;

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

    public MachineType getMachineType() {
        return machineType;
    }

    /**
     * Loads access to the B machine.
     * The access needs to be closed after use.
     *
     * @return
     *
     * @throws MachineAccessException
     */
    public MachineAccess spawnMachineAccess() throws MachineAccessException {
        return new MachineAccess(location);
    }

    @Override
    public String toString() {
        return location + "[" + machineType + "]";
    }
}
