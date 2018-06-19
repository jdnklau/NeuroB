package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
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
    PredicateTrainingGenerator<PredicateFeatures, PredicateLabelling> generator;

    @BeforeEach
    public void mockPredicateTrainingGenerator() {
        generator = mock(PredicateTrainingGenerator.class);
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
        expected.add("not((invariant)) => assertion");
        // enabling relations
        // ... none as no preconditions exist
        // invariant preservation
        expected.add("invariant & weakest");
        expected.add("invariant & not(weakest)");
        expected.add("(not(invariant) => weakest)");
        expected.add("(not(invariant) => not(weakest))");
        // multi precondition formulae
        // ... none as no preconditions exist
        // extended precondition formulae
        expected.add("(invariant)");
        // ... no additional ones as no preconditions exist

        // Stub generator
        when(generator.streamPredicatesFromCollection(pc)).thenCallRealMethod();

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

        generator = spy(
                new PredicateTrainingGenerator<>(
                        (predicate, ss) -> features,
                        (predicate, ss) -> labelling,
                        null)
        );

        // Stream 5 "generated" predicates
        List<String> predicates = new ArrayList<>();
        predicates.add("predicate1");
        predicates.add("predicate2");
        predicates.add("predicate3");
        predicates.add("predicate4");
        predicates.add("predicate5");
        doReturn(predicates.stream()).when(generator).streamPredicatesFromFile(any());
        doReturn(predicates.stream()).when(generator).streamPredicatesFromFile(any(), any());
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

        Stream<TrainingSample<PredicateFeatures, PredicateLabelling>> actualStream =
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

        // Partial Mock of generator
        // Generating functions throw exceptions for certain predicates
        generator = spy(
                new PredicateTrainingGenerator<>(
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
        doReturn(predicates.stream()).when(generator).streamPredicatesFromFile(any());
        doReturn(predicates.stream()).when(generator).streamPredicatesFromFile(any(), any());
        StateSpace ss = mock(StateSpace.class);
        doReturn(ss).when(generator).loadStateSpace(any());

        List<String> expected = new ArrayList<>();
        expected.add("predicate");
        expected.add("predicate");

        List<String> actual = generator.streamSamplesFromFile(null)
                .map(TrainingSample::getFeatures)
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
        PredicateFeatures features = mock(PredicateFeatures.class);
        when(features.getFeatureArray()).thenReturn(new Double[]{1., 2., 3.});
        // Mock labelling
        PredicateLabelling labelling = mock(PredicateLabelling.class);
        when(labelling.getLabellingArray()).thenReturn(new Double[]{1., 0.});

        TrainingSample<PredicateFeatures, Labelling> expected =
                new TrainingSample<>(features, labelling);

        TrainingSample actual =
                new PredicateTrainingGenerator<>(
                        (predicate, ss) -> features,
                        (predicate, ss) -> labelling,
                        null).generateSample("predicate");

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    @Test
    public void shouldUseGeneratorsToGenerateTrainingSampleWhenStateSpaceSupplied()
            throws Exception {
        // Mock features
        PredicateFeatures features = mock(PredicateFeatures.class);
        when(features.getFeatureArray()).thenReturn(new Double[]{1., 2., 3.});
        // Mock labelling
        PredicateLabelling labelling = mock(PredicateLabelling.class);
        when(labelling.getLabellingArray()).thenReturn(new Double[]{1., 0.});
        // Mock StateSpace
        StateSpace stateSpace = mock(StateSpace.class);

        TrainingSample<PredicateFeatures, Labelling> expected =
                new TrainingSample<>(features, labelling);

        TrainingSample actual =
                new PredicateTrainingGenerator<>(
                        (predicate, ss) -> features,
                        (predicate, ss) -> labelling,
                        null).generateSample("predicate", stateSpace);

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    @Test
    public void shouldUseGeneratorsForTrainingSampleWhenStateSpaceIsNull()
            throws Exception {
        // Mock features
        PredicateFeatures features = mock(PredicateFeatures.class);
        when(features.getFeatureArray()).thenReturn(new Double[]{1., 2., 3.});
        // Mock labelling
        PredicateLabelling labelling = mock(PredicateLabelling.class);
        when(labelling.getLabellingArray()).thenReturn(new Double[]{1., 0.});

        TrainingSample<PredicateFeatures, Labelling> expected =
                new TrainingSample<>(features, labelling);

        TrainingSample actual =
                new PredicateTrainingGenerator<>(
                        (predicate, ss) -> features,
                        (predicate, ss) -> labelling,
                        null).generateSample("predicate", null);

        assertEquals(expected, actual,
                "Training Sample does not match");
    }

    /**
     * Helper to quickly generate a list containing corresponding predicates.
     *
     * @param preds Predicates to be contained in the list.
     *
     * @return
     */
    private List<String> predList(String... preds) {
        List<String> predicates = new ArrayList<>();
        predicates.addAll(Arrays.asList(preds));
        return predicates;
    }
}