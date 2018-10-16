package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.core.features.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
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

    // TODO: Change way of data extraction, transformation, etc.
    public <F, L extends PredicateLabelling>
    PredicateTrainingGenerator(
            PredicateFeatureGenerating<F> featureGenerator,
            PredicateLabelGenerating<L> labelGenerator,
            TrainingDataFormat<? super F, ? super L> format) {
        super(featureGenerator, labelGenerator, format);
    }

    /**
     * Constructor for creating a data base of predicates.
     */
    public PredicateTrainingGenerator() {
        this(
                (pred, ss) -> pred,
                JsonDbFormat.LABEL_GENERATOR,
                new JsonDbFormat()
        );
    }

    @Override
    public Stream<TrainingSample> streamSamplesFromFile(Path file) {
        log.info("Loading training samples from {}", file);

        return streamSamplesFromFile(new BMachine(file));
    }

    public Stream<TrainingSample> streamSamplesFromFile(BMachine bMachine) {
        Stream<BPredicate> predicates = null;
        try {
            predicates = streamPredicatesFromFile(bMachine);
        } catch (MachineAccessException e) {
            log.warn("Unable to access {}", bMachine, e);
            return Stream.empty();
        }

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
                .onClose(bMachine::closeMachineAccess);

        return samples.filter(Objects::nonNull)
                // add source file information
                .map(sample -> new TrainingSample<>(
                        sample.getData(),
                        sample.getLabelling(),
                        bMachine.getLocation()));

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
    public TrainingSample generateSample(BPredicate predicate)
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
    public TrainingSample generateSample(BPredicate predicate, BMachine bMachine)
            throws FeatureCreationException, LabelCreationException {
        log.debug("Generating features for {}", predicate);
        Object features = ((PredicateFeatureGenerating) featureGenerator)
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
    public Stream<BPredicate> streamPredicatesFromFile(Path file) {
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
    public Stream<BPredicate> streamPredicatesFromFile(BMachine bMachine) throws MachineAccessException {
        PredicateCollection pc = new PredicateCollection(bMachine.getMachineAccess());
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
    public Stream<BPredicate> streamPredicatesFromCollection(
            PredicateCollection collection) {
        // stream different predicates created with FeatureGenerator
        List<Function<PredicateCollection, List<BPredicate>>> generations =
                new ArrayList<>();
        generations.add(FormulaGenerator::assertions);
        generations.add(FormulaGenerator::enablingRelationships);
        generations.add(FormulaGenerator::invariantPreservations);
        generations.add(FormulaGenerator::multiPreconditionFormulae);
        generations.add(FormulaGenerator::extendedPreconditionFormulae);

        return generations.stream()
                .flatMap(gen -> gen.apply(collection).stream());
    }
}

