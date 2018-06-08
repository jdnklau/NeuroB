package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.api.Features;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureGeneratorTest {

    @Test
    void shouldThrowExceptionWhenExpectingFeaturesInterfaceDirectly() {
        assertThrows(InstantiationException.class,
                () -> FeatureGenerator.create(Features.class, null));
    }

}