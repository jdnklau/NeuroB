package de.hhu.stups.neurob.core.features.predicates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RawPredFeatureTest {

    @Test
    void shouldVectorizeToAsciiValues() {
        String pred = "foo = bar";
        RawPredFeature feat = new RawPredFeature(pred);

        //                   'f'   'o'   'o'   ' '  '='  ' '  'b'  'a'  'r'
        Double[] expected = {102., 111., 111., 32., 61., 32., 98., 97., 114.};
        Double[] actual = feat.getFeatureArray();

        assertArrayEquals(expected, actual);
    }

}
