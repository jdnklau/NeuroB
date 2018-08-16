package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.features.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PredicateTrainingGeneratorTest {
    private
    PredicateTrainingGenerator generator;

    private TrainingDataFormat<PredicateFeatures> formatMock;
    private PredicateFeatureGenerating<PredicateFeatures> featureGen;
    private PredicateLabelGenerating<PredicateLabelling> labelGen;

    @BeforeEach
    public void setUpMocks() {
        featureGen = (pred, ss) -> generateMockedFeatures(pred);
        labelGen = (pred, ss) -> generateMockedLabels(pred);
        formatMock = mock(TrainingDataFormat.class);

        // NOTE: Generator should be set to null before each test to ensure no
        // test might accidentally run on one initialised by another test.
        // Actually, the generator might better be declared in the tests
        // themselves, but due to generics the signature is waaay to long
        // and would only hurt readability.
        generator = null;
    }

    private PredicateFeatures generateMockedFeatures(String pred) {
        PredicateFeatures f = mock(PredicateFeatures.class);
        when(f.getFeatureArray()).thenReturn(new Double[]{1., 2., 3.});
        when(f.getPredicate()).thenReturn(pred);
        return f;
    }

    private PredicateLabelling generateMockedLabels(String pred) {
        PredicateLabelling f = mock(PredicateLabelling.class);
        when(f.getLabellingArray()).thenReturn(new Double[]{1., 2., 3.});
        when(f.getPredicate()).thenReturn(pred);
        return f;
    }

    @Test
    public void shouldStreamPredicatesFromCollection() {
        // Mock a collection
        PredicateCollection pc = mock(PredicateCollection.class);
        // Simply return the same list over and over again
        when(pc.getAssertions()).thenReturn(predList("assertion"));
        when(pc.getInvariants()).thenReturn(predList("invariant"));
        when(pc.getProperties()).thenReturn(predList()); // no properties
        when(pc.getOperationNames()).thenReturn(predList("operation"));
        when(pc.getMachineType()).thenReturn(MachineType.EVENTB);
        when(pc.getPrimedInvariants()).thenReturn(new HashMap<>());
        when(pc.getBeforeAfterPredicates()).thenReturn(new HashMap<>());
        when(pc.getPreconditions()).thenReturn(new HashMap<>());
        Map<String, String> weakPrecondition = new HashMap<>();
        weakPrecondition.put("invariant", "weakest");
        Map<String, Map<String, String>> weakestPreconditions = new HashMap<>();
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

        Stream<String> stream = generator.streamPredicatesFromCollection(pc);
        List<String> predicates = stream.collect(Collectors.toList());

        expected.sort(Comparator.naturalOrder());
        predicates.sort(Comparator.naturalOrder());

        assertEquals(expected, predicates,
                "Streamed predicates do not match");

    }

    @Test
    public void shouldGenerateFeaturesFromStreamedPredicates() throws Exception {
        PredicateFeatures features = mock(PredicateFeatures.class);
        when(features.getFeatureArray()).thenReturn(new Double[]{1., 2., 3.});

        PredicateLabelling labelling = mock(PredicateLabelling.class);
        when(labelling.getLabellingArray()).thenReturn(new Double[]{1., 0.});

        // Prepare 5 predicates to stream
        List<String> predicates = new ArrayList<>();
        predicates.add("predicate1");
        predicates.add("predicate2");
        predicates.add("predicate3");
        predicates.add("predicate4");
        predicates.add("predicate5");

        // Set up spy
        generator = spy(new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock));
        doReturn(predicates.stream())
                .when(generator).streamPredicatesFromFile(any());
        doReturn(predicates.stream())
                .when(generator).streamPredicatesFromFile(any(), any());
        doReturn(mock(StateSpace.class)).when(generator).loadStateSpace(any());

        // Expecting 5 training samples (all the same)
        List<TrainingSample<PredicateFeatures, Labelling>> expected = new ArrayList<>();
        TrainingSample<PredicateFeatures, Labelling> singleSample =
                new TrainingSample<>(features, labelling);
        doReturn(singleSample).when(generator).generateSample(anyString());
        expected.add(singleSample);
        expected.add(singleSample);
        expected.add(singleSample);
        expected.add(singleSample);
        expected.add(singleSample);

        Stream<TrainingSample> actualStream =
                generator.streamSamplesFromFile(null);
        List<TrainingSample> actual = actualStream.collect(Collectors.toList());

        assertEquals(expected, actual,
                "Generated training samples do not match");
    }

    @Test
    public void shouldNotStreamNullSamples() throws Exception {
        PredicateFeatures features = mock(PredicateFeatures.class);
        when(features.getFeatureArray()).thenReturn(new Double[]{1., 2., 3.});
        when(features.getPredicate()).thenReturn("predicate");

        PredicateLabelling labelling = mock(PredicateLabelling.class);
        when(labelling.getLabellingArray()).thenReturn(new Double[]{1., 0.});

        // Generating functions throw exceptions for certain predicates
        generator = spy(new PredicateTrainingGenerator(
                (predicate, ss) -> {
                    if (predicate.equals("featureException"))
                        throw new FeatureCreationException();
                    return features;
                },
                (predicate, ss) -> {
                    if (predicate.equals("labelException"))
                        throw new LabelCreationException();
                    return labelling;
                },
                null)
        );
        List<String> predicates = new ArrayList<>();
        predicates.add("predicate");
        predicates.add("featureException");
        predicates.add("labelException");
        predicates.add("predicate");
        doReturn(predicates.stream())
                .when(generator).streamPredicatesFromFile(any());
        doReturn(predicates.stream())
                .when(generator).streamPredicatesFromFile(any(), any());
        StateSpace ss = mock(StateSpace.class);
        doReturn(ss).when(generator).loadStateSpace(any());

        List<String> expected = new ArrayList<>();
        expected.add("predicate");
        expected.add("predicate");

        List<String> actual = generator.streamSamplesFromFile(null)
                .map(TrainingSample::getFeatures)
                .map(f -> (PredicateFeatures) f)
                .map(PredicateFeatures::getPredicate)
                .collect(Collectors.toList());

        expected.sort(Comparator.naturalOrder());
        actual.sort(Comparator.naturalOrder());

        assertEquals(expected, actual,
                "Streamed samples do not match");
    }

    @Test
    public void shouldUseGeneratorsToGenerateTrainingSample() throws Exception {
        // Mock features
        PredicateFeatures features = generateMockedFeatures("");
        // Mock labelling
        PredicateLabelling labelling = generateMockedLabels("");

        TrainingSample<PredicateFeatures, Labelling> expected =
                new TrainingSample<>(features, labelling);

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

        TrainingSample actual = generator.generateSample("predicate");

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    @Test
    public void shouldUseGeneratorsToGenerateTrainingSampleWhenStateSpaceSupplied()
            throws Exception {
        // Mock StateSpace
        StateSpace stateSpace = mock(StateSpace.class);

        TrainingSample<PredicateFeatures, PredicateLabelling> expected =
                new TrainingSample<>(
                        generateMockedFeatures(""),
                        generateMockedLabels(""));

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, null);
        TrainingSample actual = generator.generateSample("pred", stateSpace);

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    @Test
    public void shouldUseGeneratorsForTrainingSampleWhenStateSpaceIsNull()
            throws Exception {
        // Mock features
        PredicateFeatures features = generateMockedFeatures("");
        // Mock labelling
        PredicateLabelling labelling = generateMockedLabels("");

        TrainingSample<PredicateFeatures, Labelling> expected =
                new TrainingSample<>(features, labelling);

        generator = new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock);

        TrainingSample actual = generator.generateSample("predicate", null);

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    @Test
    public void shouldStreamSamplesWithSourceInformationWhenCreatingFromFile()
            throws Exception {
        Path file = Paths.get("/not/existent/path");

        PredicateFeatures features = mock(PredicateFeatures.class);
        when(features.getFeatureArray()).thenReturn(new Double[]{1., 2., 3.});

        PredicateLabelling labelling = mock(PredicateLabelling.class);
        when(labelling.getLabellingArray()).thenReturn(new Double[]{1., 0.});

        generator = spy(new PredicateTrainingGenerator(
                featureGen, labelGen, formatMock));

        List<String> predicates = predList("predicate");
        doReturn(predicates.stream())
                .when(generator).streamPredicatesFromFile(any());
        doReturn(predicates.stream())
                .when(generator).streamPredicatesFromFile(any(), any());
        StateSpace ss = mock(StateSpace.class);
        doReturn(ss).when(generator).loadStateSpace(any());

        Path actual = generator.streamSamplesFromFile(file)
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
    private List<String> predList(String... preds) {
        List<String> predicates = new ArrayList<>(Arrays.asList(preds));
        return predicates;
    }
}
