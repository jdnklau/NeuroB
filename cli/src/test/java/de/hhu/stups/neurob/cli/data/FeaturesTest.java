package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.core.features.predicates.BAst115Features;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatureGenerating;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeaturesTest {

    @Test
    void shouldGetF115() throws Exception {
        PredicateFeatureGenerating gen = Features.parseFormat("f115");

        assertTrue(gen instanceof BAst115Features.Generator);
    }

    @Test
    void shouldGetF115Size() throws Exception {
        int actual = Features.parseFeatureSize("f115");

        assertEquals(115, actual);
    }

}
