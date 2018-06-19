package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.core.features.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.util.FormulaGenerator;
import de.hhu.stups.neurob.training.generation.util.PredicateCollection;
import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class PredicateTrainingGenerator
        <F extends PredicateFeatures, L extends PredicateLabelling>
        extends TrainingDataGenerator<F, L> {

    private Api api;

    private static Logger log =
            LoggerFactory.getLogger(PredicateFeatureGenerating.class);

    public PredicateTrainingGenerator(
            PredicateFeatureGenerating<F> featureGenerator,
            PredicateLabelGenerating<L> labelGenerator,
            TrainingDataFormat format) {
        super(featureGenerator, labelGenerator, format);

        api = Main.getInjector().getInstance(Api.class);
    }

    @Override
    public Stream<TrainingSample<F, L>> streamSamplesFromFile(Path file) {
        log.info("Loading training samples from {}", file);

        // Try to access machine or return empty stream
        Stream<String> predicates;
        StateSpace ss;
        try {
            ss = loadStateSpace(file);
            predicates = streamPredicatesFromFile(file, ss);
        } catch (MachineAccessException e) {
            log.warn("Unable to access {}: {}", file, e.getMessage(), e);
            return Stream.empty();
        }

        // Stream training samples
        Stream<TrainingSample<F, L>> samples = predicates.map(predicate -> {
            try {
                return generateSample(predicate, ss);
            } catch (FeatureCreationException e) {
                log.warn("Could not create features from {}", predicate, e);
            } catch (LabelCreationException e) {
                log.warn("Could not create labelling for {}", predicate, e);
            }
            // If any exceptions occur, return nothing
            return null;
        });

        // Close StateSpace
        ss.kill();

        return samples.filter(sample -> sample != null);
    }

    /**
     * Takes a predicate and generates a pair of Features and Labelling out of
     * it,
     * returning a TrainingSample containing both.
     *
     * @param predicate Predicate to be translated into a training
     *         sample.
     *
     * @return
     *
     * @throws FeatureCreationException
     * @throws LabelCreationException
     */
    public TrainingSample<F, L> generateSample(String predicate)
            throws FeatureCreationException, LabelCreationException {
        return generateSample(predicate, null);
    }

    /**
     * Takes a predicate and generates a pair of Features and Labelling out of
     * it,
     * returning a TrainingSample containing both.
     *
     * @param predicate Predicate to be translated into a training
     *         sample.
     * @param ss StateSpace the predicate belongs to.
     *
     * @return
     *
     * @throws FeatureCreationException
     * @throws LabelCreationException
     */
    public TrainingSample<F, L> generateSample(String predicate, StateSpace ss)
            throws FeatureCreationException, LabelCreationException {
        log.debug("Generating features for {}", predicate);
        F features = ((PredicateFeatureGenerating<F>) featureGenerator)
                .generate(predicate, ss);

        log.debug("Generating labelling for {}", predicate);
        L labelling = ((PredicateLabelGenerating<L>) labelGenerator)
                .generate(predicate, ss);


        return new TrainingSample<>(features, labelling);
    }

    @Override
    public boolean dataAlreadyExists(Path sourceFile, Path targetLocation) {
        // TODO
        return false;
    }

    /**
     * Generates predicates from given machine file.
     * The given {@link StateSpace} represents the already loaded machine.
     * If {@code ss} is {@code null}, a state space will be loaded at first.
     * <p>
     * The predicates are generated with the following functions:
     * <ul>
     * <li>{@link FormulaGenerator#assertions(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#enablingRelationships(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#invariantPreservations(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#multiPreconditionFormulae(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#extendedPreconditionFormulae(PredicateCollection)}</li>
     * </ul>
     *
     * @param file B machine to access.
     * @param ss Already loaded StateSpace from file
     *
     * @return Stream of generated predicates.
     */
    public Stream<String> streamPredicatesFromFile(Path file, StateSpace ss) {
        // open StateSpace, collect predicates
        PredicateCollection pc;
        try {
            boolean killStateSpace = false;
            if (ss == null) {
                ss = loadStateSpace(file);
                killStateSpace = true; // we opened it, we kill it again.
            }
            pc = new PredicateCollection(ss);
            ss.kill();
        } catch (MachineAccessException e) {
            log.warn("Could not load {}; no predicates generated", file, e);
            return Stream.empty();
        }

        return streamPredicatesFromCollection(pc);
    }

    /**
     * Loads given B machine, generates predicates from it and stream them.
     * <p>
     * The predicates are generated with the following functions:
     * <ul>
     * <li>{@link FormulaGenerator#assertions(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#enablingRelationships(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#invariantPreservations(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#multiPreconditionFormulae(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#extendedPreconditionFormulae(PredicateCollection)}</li>
     * </ul>
     *
     * @param file B machine to access.
     *
     * @return Stream of generated predicates.
     */
    public Stream<String> streamPredicatesFromFile(Path file) {
        return streamPredicatesFromFile(file, null);
    }

    /**
     * Streams generated predicates from a given {@link PredicateCollection}.
     * <p>
     * The predicates are generated with the following functions:
     * <ul>
     * <li>{@link FormulaGenerator#assertions(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#enablingRelationships(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#invariantPreservations(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#multiPreconditionFormulae(PredicateCollection)}</li>
     * <li>{@link FormulaGenerator#extendedPreconditionFormulae(PredicateCollection)}</li>
     * </ul>
     *
     * @param collection
     *
     * @return
     */
    public Stream<String> streamPredicatesFromCollection(
            PredicateCollection collection) {
        // stream different predicates created with FeatureGenerator
        List<Function<PredicateCollection, List<String>>> generations =
                new ArrayList<>();
        generations.add(FormulaGenerator::assertions);
        generations.add(FormulaGenerator::enablingRelationships);
        generations.add(FormulaGenerator::invariantPreservations);
        generations.add(FormulaGenerator::multiPreconditionFormulae);
        generations.add(FormulaGenerator::extendedPreconditionFormulae);

        return generations.stream().
                flatMap(gen -> gen.apply(collection).stream());
    }

    /**
     * Loads a {@link StateSpace} from the given file.
     * Only supports *.mch (Classical B) and *.bpm (EventB) files.
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
            if (machineFile.endsWith(".mch")) {
                log.info("Load State Space for Classical B machine {}", file);
                return api.b_load(machineFile);
            } else if (machineFile.endsWith("*.bpm")) {
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
}
