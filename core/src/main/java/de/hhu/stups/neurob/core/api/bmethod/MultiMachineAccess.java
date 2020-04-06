package de.hhu.stups.neurob.core.api.bmethod;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.prob.animator.command.AbstractCommand;
import de.prob.statespace.StateSpace;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Machine Access that serves as a single access point to the same machine
 * which is actually loaded for multiple backends (read: multiple preferences).
 */
public class MultiMachineAccess extends MachineAccess {
    protected final Set<Backend> backends;
    /** Map containing all the different machine accesses */
    protected Map<Backend, MachineAccess> accessMap;

    public MultiMachineAccess(Path source, MachineType machineType, Backend[] backends,
            boolean initialise) throws MachineAccessException {
        super(source, machineType, false);
        this.backends = new HashSet<>();
        this.backends.addAll(Arrays.asList(backends));

        // Establish the multi-accesses
        this.accessMap = new HashMap<>();
        for (Backend b : backends) {
            MachineAccess access = new MachineAccess(source, machineType, initialise);
            access.setPreferences(b.getPreferences());
            accessMap.put(b, access);
        }

    }

    public MultiMachineAccess(Path source, MachineType machineType,
            Map<Backend, MachineAccess> accessMap, boolean initialise)
            throws MachineAccessException {
        super(source, machineType, false);
        this.backends = accessMap.keySet();
        this.accessMap = accessMap;
    }

    @Override
    public MachineAccess load() throws MachineAccessException {
        for (Backend b : backends) {
            accessMap.get(b).load();
        }
        isLoaded = true;
        return this;
    }

    @Override
    public void close() {
        backends.stream()
                .map(accessMap::get)
                .forEach(MachineAccess::close);
        isLoaded = false;
        closeHandlers.stream().forEach(c -> c.accept(this));
    }

    @Override
    public void execute(AbstractCommand... commands) {
        backends.stream()
                .map(accessMap::get)
                .forEach(
                        access -> access.execute(commands)
                );
    }

    @Override
    public StateSpace getStateSpace() {
        return super.getStateSpace();
    }

    public MachineAccess getAccess(Backend b) {
        return accessMap.get(b);
    }

    @Override
    public void setPreferences(BPreferences preferences) {
        // Set preferences for each access
        backends.stream()
                .map(accessMap::get)
                .forEach(a -> a.setPreferences(preferences));
    }

    @Override
    public void setPreferences(BPreference... preferences) {
        this.setPreferences(new BPreferences(preferences));
    }

    @Override
    public void sendInterrupt() {
        backends.stream()
                .map(accessMap::get)
                .forEach(MachineAccess::sendInterrupt);
    }

    @Override
    public String toString() {
        String containedAccesses = backends.stream()
                .map(accessMap::get)
                .map(MachineAccess::toString)
                .collect(Collectors.joining(", "));
        return getSource() + "{"
               + "loaded=" + isLoaded() + ", "
               + "contains=[" + containedAccesses + "]"
               + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MultiMachineAccess) {
            MultiMachineAccess other = (MultiMachineAccess) o;

            return accessMap.equals(other.accessMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return accessMap.hashCode();
    }
}
