package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.api.backends.KodKodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.features.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.util.FormulaGenerator;
import de.hhu.stups.neurob.training.generation.util.PredicateCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class PredicateTrainingGenerator
        extends TrainingSetGenerator {


    private static Logger log =
            LoggerFactory.getLogger(PredicateFeatureGenerating.class);

    public <F extends PredicateFeatures, L extends PredicateLabelling>
    PredicateTrainingGenerator(
            PredicateFeatureGenerating<F> featureGenerator,
            PredicateLabelGenerating<L> labelGenerator,
            TrainingDataFormat<? super F> format) {
        super(featureGenerator, labelGenerator, format);
    }

    /**
     * Constructor for creating a data base of predicates.
     */
    public PredicateTrainingGenerator() {
        this(
                (pred, ss) -> new PredicateFeatures(pred),
                // Todo: short cut for this?
                new DecisionTimings.Generator(3,
                        new KodKodBackend(),
                        new ProBBackend(),
                        new SmtBackend(),
                        new Z3Backend()),
                new PredicateDbFormat()
        );
    }

    @Override
    public Stream<TrainingSample> streamSamplesFromFile(Path file) {
        log.info("Loading training samples from {}", file);

        // Try to access machine or return empty stream
        MachineAccess bMachine;
        try {
            bMachine = new MachineAccess(file);
        } catch (MachineAccessException e) {
            log.warn("Unable to access {}: {}", file, e.getMessage(), e);
            return Stream.empty();
        }

        return streamSamplesFromFile(bMachine);
    }

    public Stream<TrainingSample> streamSamplesFromFile(MachineAccess bMachine) {
        Stream<String> predicates = streamPredicatesFromFile(bMachine);

        // Stream training samples
        Stream<TrainingSample> samples = predicates.map(
                predicate -> {
                    try {
                        log.trace("Generating sample for {}", predicate);
                        return generateSample(predicate, bMachine);
                    } catch (FeatureCreationException e) {
                        log.warn("Could not create features from {}", predicate, e);
                    } catch (LabelCreationException e) {
                        log.warn("Could not create labelling for {}", predicate, e);
                    }
                    // If any exceptions occur, return nothing
                    return null;
                })
                .onClose(bMachine::close);

        return samples.filter(Objects::nonNull)
                // add source file information
                .map(sample -> new TrainingSample<>(
                        sample.getFeatures(),
                        sample.getLabelling(),
                        bMachine.getSource()));

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
    public TrainingSample generateSample(String predicate)
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
     * @param bMachine Access to the machine file the predicate belongs to.
     *
     * @return
     *
     * @throws FeatureCreationException
     * @throws LabelCreationException
     */
    public TrainingSample generateSample(String predicate, MachineAccess bMachine)
            throws FeatureCreationException, LabelCreationException {
        log.debug("Generating features for {}", predicate);
        Features features = ((PredicateFeatureGenerating) featureGenerator)
                .generate(predicate, bMachine);

        log.debug("Generating labelling for {}", predicate);
        Labelling labelling = ((PredicateLabelGenerating) labelGenerator)
                .generate(predicate, bMachine);

        return new TrainingSample<>(features, labelling);
    }

    @Override
    public boolean dataAlreadyExists(Path sourceFile, Path targetLocation) {
        if (Files.exists(targetLocation, LinkOption.NOFOLLOW_LINKS)) {
            try {
                FileTime sourceLastModified = Files.getLastModifiedTime(
                        sourceFile, LinkOption.NOFOLLOW_LINKS);
                FileTime targetLastModified = Files.getLastModifiedTime(
                        targetLocation, LinkOption.NOFOLLOW_LINKS);

                // last edit source file <= last edit target file -> nothing to do here
                if (sourceLastModified.compareTo(targetLastModified) <= 0) {
                    return true;
                }
            } catch (IOException e) {
                log.warn("Could not determine whether for the source file {} "
                         + "the target location {} is up to date or not. "
                         + "Generating data anyway.",
                        sourceFile, targetLocation, e);
            }
        }
        return false;
    }

    /**
     * Generates predicates from given machine file.
     * The given {@link MachineAccess} belongs to the already loaded machine.
     * If {@code bMachine} is {@code null}, an access will be opened first.
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
        PredicateCollection pc;
        try {
            MachineAccess bMachine = new MachineAccess(file);
            pc = new PredicateCollection(bMachine);
            bMachine.close();
        } catch (MachineAccessException e) {
            log.warn("Could not load {}; no predicates generated", file, e);
            return Stream.empty();
        }

        return streamPredicatesFromCollection(pc);
    }

    /**
     * Generates predicates from given B machine.
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
     * @param bMachine B machine to access.
     *
     * @return Stream of generated predicates.
     */
    public Stream<String> streamPredicatesFromFile(MachineAccess bMachine) {
        PredicateCollection pc = new PredicateCollection(bMachine);
        return streamPredicatesFromCollection(pc);
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
}

