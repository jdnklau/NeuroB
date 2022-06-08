package de.hhu.stups.neurob.core.api.bmethod;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
    private static final Logger log =
            LoggerFactory.getLogger(BMachine.class);

    public static final BMachine EMPTY;
    static {
        BMachine emptyMch;
        try {
            InputStream resourceAsStream = BMachine.class.getClassLoader().getResourceAsStream("empty.mch");
            Path tempDirectory = Files.createTempDirectory("neurob-runtime");

            Path tmpMch = tempDirectory.resolve("emtpy.mch");
            Files.copy(resourceAsStream, tmpMch);
            emptyMch = new BMachine(tmpMch);
        } catch (IOException | NullPointerException e) {
            log.error("Unable to create temp dir for empty.mch - empty machine not available.", e);
            emptyMch = null;
        }
        EMPTY = emptyMch;
    }

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

    @Override
    public int hashCode() {
        if (location == null) {
            return 0;
        }
        return location.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BMachine) {
            BMachine other = (BMachine) o;
            if (this.location == null)
                return other.location == null;
            return this.location.equals(other.location);
        }
        return false;
    }
}
