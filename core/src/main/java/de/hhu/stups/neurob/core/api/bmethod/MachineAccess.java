package de.hhu.stups.neurob.core.api.bmethod;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.prob.Main;
import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MachineAccess {
    private final Path source;
    private final MachineType machineType;

    private final Api api;
    private boolean isLoaded = false;
    private StateSpace stateSpace = null;

    private BPreferences preferences;

    private static final Logger log =
            LoggerFactory.getLogger(MachineAccess.class);


    public MachineAccess(Path source, MachineType machineType) throws MachineAccessException {
        this(source, machineType, true);
    }

    public MachineAccess(Path source, MachineType machineType, boolean initialise)
            throws MachineAccessException {
        this.source = source;
        this.machineType = machineType;

        this.preferences = new BPreferences(); // empty preferences

        api = Main.getInjector().getInstance(Api.class);
        if (initialise) {
            load();
        }

    }

    public MachineAccess(Path source) throws MachineAccessException {
        this(source, MachineType.predictTypeFromLocation(source));
    }

    public void load() throws MachineAccessException {
        if (stateSpace != null) {
            log.info("Reloading state space for {}, closing old connection", source);
            stateSpace.kill();
            isLoaded = false;
        }

        stateSpace = loadStateSpace(source);
        isLoaded = true;
    }


    public Path getSource() {
        return source;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public StateSpace getStateSpace() {
        return stateSpace;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setPreferences(BPreferences preferences) {
        // Gather new preferences which were not previously present
        List<BPreference> newPrefs = preferences.stream()
                .filter(p -> !this.preferences.contains(p))
                .collect(Collectors.toList());

        if (newPrefs.isEmpty()) {
            return;
        }

        // Execute preference commands
        newPrefs.stream()
                .map(p -> new SetPreferenceCommand(p.getName(), p.getValue()))
                .forEach(stateSpace::execute);

        // store full preferences
        BPreference[] fullPrefs = Stream.of(this.preferences.stream(), newPrefs.stream())
                .flatMap(s -> s)
                .toArray(BPreference[]::new);

        this.preferences = new BPreferences(fullPrefs);
    }

    public void setPreferences(BPreference... preferences) {
        setPreferences(new BPreferences(preferences));
    }

    /**
     * Loads a {@link StateSpace} from the given file.
     * Only supports *.mch (Classical B) and *.bcm (EventB) files.
     *
     * @param file Path to the machine file to load.
     *
     * @return
     *
     * @throws MachineAccessException
     */
    protected StateSpace loadStateSpace(Path file) throws MachineAccessException {
        String machineFile = file.toString(); // only str version needed
        try {
            // TODO: Make use of machine type
            if (machineFile.endsWith(".mch")) {
                log.info("Load State Space for Classical B machine {}", file);
                return api.b_load(machineFile);
            } else if (machineFile.endsWith(".bcm")) {
                log.info("Load State Space for EventB machine {}", file);
                return api.eventb_load(machineFile);
            } else {
                throw new MachineAccessException(
                        "Loading state space for " + machineFile + " failed "
                        + "due to not being able to detect correct formalism");
            }
        } catch (ModelTranslationError modelTranslationError) {
            throw new MachineAccessException(
                    "Unable to load state space for" + machineFile,
                    modelTranslationError);
        } catch (Exception e) {
            throw new MachineAccessException(
                    "Unexpected exception encountered during loading of "
                    + "state space for " + machineFile, e);
        }

    }

    public IEvalElement parseFormula(BPredicate formula) {
        return stateSpace.getModel().parseFormula(formula.toString(), FormulaExpand.EXPAND);
    }

    /**
     * Close connection to B machine.
     */
    public void close() {
        log.debug("Closed access to {}", source);
        stateSpace.kill();
        isLoaded = false;
    }

    public void execute(AbstractCommand... commands) {
        stateSpace.execute(commands);
    }

    /**
     * Interrupt the machine access.
     */
    public void sendInterrupt() {
        stateSpace.sendInterrupt();
    }

}
