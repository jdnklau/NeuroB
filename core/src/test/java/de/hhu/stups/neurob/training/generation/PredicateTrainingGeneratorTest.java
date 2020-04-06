package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.util.PredicateCollection;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PredicateTrainingGeneratorTest {
    private
    PredicateTrainingGenerator generator;

    private TrainingDataFormat<PredicateFeatures, PredicateLabelling> formatMock;
    private PredicateFeatureGenerating<PredicateFeatures> featureGen;
    private PredicateLabelGenerating<PredicateLabelling> labelGen;

    @BeforeEach
    public void setUpMocks() {
        featureGen = (pred, ss) -> new PredicateFeatures(pred, 1., 2., 3.);
        labelGen = (pred, ss) -> new PredicateLabelling(pred, 1., 2., 3.);
        formatMock = mock(TrainingDataFormat.class);

        // NOTE: Generator should be set to null before each test to ensure no
        // test might accidentally run on one initialised by another test.
        // Actually, the generator might better be declared in the tests
        // themselves, but due to generics the signature is waaay to long
        // and would only hurt readability.
        // FIXME: No generics anymore, pull generator into each test?
        generator = null;
    }

    @Test
    public void shouldStreamPredicatesFromCollection() {
        // Mock a collection
        PredicateCollection pc = mock(PredicateCollection.class);
        // Simply return the same list over and over again
        when(pc.getAssertions()).thenReturn(predList("assertion"));
        when(pc.getInvariants()).thenReturn(predList("invariant"));
        when(pc.getProperties()).thenReturn(predList()); // no properties
        when(pc.getOperationNames()).thenReturn(predStringList("operation"));
        when(pc.getMachineType()).thenReturn(MachineType.EVENTB);
        when(pc.getPrimedInvariants()).thenReturn(new HashMap<>());
        when(pc.getBeforeAfterPredicates()).thenReturn(new HashMap<>());
        when(pc.getPreconditions()).thenReturn(new HashMap<>());
        Map<BPredicate, BPredicate> weakPrecondition = new HashMap<>();
        weakPrecondition.put(BPredicate.of("invariant"), BPredicate.of("weakest"));
        Map<String, Map<BPredicate, BPredicate>> weakestPreconditions = new HashMap<>();
        weakestPreconditions.put("operation", weakPrecondition);
        when(pc.getWeakestPreConditions()).thenReturn(weakestPreconditions);

        List<String> expected = new ArrayList<>();
        // assertions
        expected.add("(invariant) & assertion");
        expected.add("(invariant) & not(assertion)");
        expected.add("not((invariant)) => (assertion)");
        // enabling relations
        // ... none as no preconditions exist
        // invariant preservation
        expected.add("invariant & weakest");
        expected.add("invariant & not(weakest)");
        expected.add("(not(invariant) => (weakest))");
        expected.add("(not(invariant) => not(weakest))");
        // multi precondition formulae
        // ... none as no preconditions exist
        // extended precondition formulae
        expected.add("(invariant)");
        // ... no additional ones as no preconditions exist

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

        Stream<String> stream = generator.streamPredicatesFromCollection(pc).map(BPredicate::toString);
        List<String> predicates = stream.collect(Collectors.toList());

        expected.sort(Comparator.naturalOrder());
        predicates.sort(Comparator.naturalOrder());

        assertEquals(expected, predicates,
                "Streamed predicates do not match");

    }

    @Test
    public void shouldGenerateFeaturesFromStreamedPredicates() throws Exception {
        // Prepare 5 predicates to stream
        List<String> predicates = new ArrayList<>();
        predicates.add("predicate1");
        predicates.add("predicate2");
        predicates.add("predicate3");
        predicates.add("predicate4");
        predicates.add("predicate5");

        // Set up spy
        BMachine bMachine = mock(BMachine.class);
        doReturn(mock(MachineAccess.class)).when(bMachine).spawnMachineAccess();
        generator = spy(new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock));
        doReturn(predicates.stream().map(BPredicate::new))
                .when(generator).streamPredicatesFromFile(any(Path.class));
        doReturn(predicates.stream().map(BPredicate::new))
                .when(generator).streamPredicatesFromFile(any(BMachine.class));

        // Expecting 5 training samples
        List<TrainingSample<PredicateFeatures, PredicateLabelling>> expected =
                predicates.stream()
                        .map(pred -> new TrainingSample<>(
                                new PredicateFeatures(pred, 1., 2., 3.),
                                new PredicateLabelling(pred, 1., 2., 3.)))
                        .collect(Collectors.toList());

        Stream<TrainingSample> actualStream =
                generator.streamSamplesFromFile(bMachine);
        List<TrainingSample> actual = actualStream.collect(Collectors.toList());

        assertEquals(expected, actual,
                "Generated training samples do not match");
    }

    @Test
    public void shouldNotStreamNullSamples() throws MachineAccessException {
        // Generating functions throw exceptions for certain predicates
        generator = spy(new PredicateTrainingGenerator(
                (predicate, ss) -> {
                    if (predicate.equals(BPredicate.of("featureException")))
                        throw new FeatureCreationException();
                    return new PredicateFeatures("predicate", 1., 2., 3.);
                },
                (predicate, ss) -> {
                    if (predicate.equals(BPredicate.of("labelException")))
                        throw new LabelCreationException();
                    return new PredicateLabelling("predicate,", 1., 0.);
                },
                null)
        );
        List<String> predicates = new ArrayList<>();
        predicates.add("predicate");
        predicates.add("featureException");
        predicates.add("labelException");
        predicates.add("predicate");
        doReturn(predicates.stream().map(BPredicate::of))
                .when(generator).streamPredicatesFromFile(any(Path.class));
        doReturn(predicates.stream().map(BPredicate::of))
                .when(generator).streamPredicatesFromFile(any(BMachine.class));
        StateSpace ss = mock(StateSpace.class);

        BMachine bMachine = mock(BMachine.class);
        MachineAccess machineAccess = mock(MachineAccess.class);
        doReturn(machineAccess).when(bMachine).spawnMachineAccess();
        doReturn(ss).when(machineAccess).getStateSpace();

        List<String> expected = new ArrayList<>();
        expected.add("predicate");
        expected.add("predicate");

        List<String> actual = generator.streamSamplesFromFile(bMachine)
                .map(TrainingSample::getFeatures)
                .map(f -> (PredicateFeatures) f)
                .map(PredicateFeatures::getPredicate)
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        expected.sort(Comparator.naturalOrder());
        actual.sort(Comparator.naturalOrder());

        assertEquals(expected, actual,
                "Streamed samples do not match");
    }

    @Test
    public void shouldUseGeneratorsToGenerateTrainingSample() throws Exception {
        PredicateFeatures features = new PredicateFeatures("predicate", 1., 2., 3.);
        PredicateLabelling labelling = new PredicateLabelling("predicate", 1., 2., 3.);

        TrainingSample<PredicateFeatures, Labelling> expected =
                new TrainingSample<>(features, labelling);

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

        TrainingSample<PredicateFeatures, Labelling> actual =
                generator.generateSamples(BPredicate.of("predicate")).findFirst().get();

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    @Test
    public void shouldSampleTrainingDataSpecifiedAmountOfTimes() throws Exception {
        PredicateFeatures features = new PredicateFeatures("predicate", 1., 2., 3.);
        PredicateLabelling[] labelling = {
                new PredicateLabelling("predicate", 1., 2., 3.),
                new PredicateLabelling("predicate", 2., 2., 3.),
                new PredicateLabelling("predicate", 3., 2., 3.)
        };

        List expected = new ArrayList<>();
        expected.add(new TrainingSample<>(features, labelling[0]));
        expected.add(new TrainingSample<>(features, labelling[1]));
        expected.add(new TrainingSample<>(features, labelling[2]));

        // stub a generator
        AtomicInteger i = new AtomicInteger(0);
        PredicateLabelGenerating<PredicateLabelling> labelGen = (p, ss) -> labelling[i.getAndIncrement()];

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, 3, formatMock);

        List actual =
                generator.generateSamples(BPredicate.of("predicate")).collect(Collectors.toList());

        assertEquals(expected, actual,
                "Training Samples does not match");
    }

    @Test
    public void shouldUseGeneratorsToGenerateTrainingSampleWhenStateSpaceSupplied()
            throws Exception {
        // Mock StateSpace
        MachineAccess bMachine = mock(MachineAccess.class);

        TrainingSample<PredicateFeatures, PredicateLabelling> expected =
                new TrainingSample<>(
                        featureGen.generate("pred"),
                        labelGen.generate("pred"));

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, null);
        TrainingSample actual =
                generator.generateSamples(BPredicate.of("pred"), bMachine).findFirst().get();

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    @Test
    public void shouldUseGeneratorsForTrainingSampleWhenStateSpaceIsNull()
            throws Exception {
        PredicateFeatures features = new PredicateFeatures("predicate", 1., 2., 3.);
        PredicateLabelling labelling = new PredicateLabelling("predicate", 1., 2., 3.);

        TrainingSample<PredicateFeatures, Labelling> expected =
                new TrainingSample<>(features, labelling);

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

        TrainingSample actual =
                generator.generateSamples(BPredicate.of("predicate"), null).findFirst().get();

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    @Test
    public void shouldStreamSamplesWithSourceInformationWhenCreatingFromFile() throws MachineAccessException {
        Path file = Paths.get("/not/existent/path");
        BMachine bMachine = mock(BMachine.class);
        when(bMachine.getLocation()).thenReturn(file);
        doReturn(mock(MachineAccess.class)).when(bMachine).spawnMachineAccess();

        generator = spy(new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock));

        List<BPredicate> predicates = predList("predicate");
        doReturn(predicates.stream())
                .when(generator).streamPredicatesFromFile(any(Path.class));
        doReturn(predicates.stream())
                .when(generator).streamPredicatesFromFile(any(BMachine.class));

        Path actual = generator.streamSamplesFromFile(bMachine)
                .findFirst().get().getSourceFile();

        assertEquals(file, actual,
                "Training sample contains no information about source file");
    }

    /**
     * Helper to quickly generate a list containing corresponding predicates.
     *
     * @param preds Predicates to be contained in the list.
     *
     * @return
     */
    private List<BPredicate> predList(String... preds) {
        List<BPredicate> predicates = Arrays.stream(preds)
                .map(BPredicate::new)
                .collect(Collectors.toList());
        return predicates;
    }

    /**
     * Helper to quickly generate a list containing corresponding predicates as Strings.
     *
     * @param preds Predicates to be contained in the list.
     *
     * @return
     */
    private List<String> predStringList(String... preds) {
        return Arrays.asList(preds);
    }
}
