package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BAst115FeaturesTest {

    @Test
    void shouldHaveNewFeatures() throws FeatureCreationException {
        BPredicate pred = BPredicate.of("x:INT & y:INT & z:INT & 4>3 & (x+y>2 => x>7)");
        BAst115Features feats = new BAst115Features.Generator().generate(pred, (MachineAccess) null);

        double[] expected = {
                6./5.,
                5./5.,
                6./3.,
                5./3.,
                1./5.
        };
        Double[] featArray = feats.getFeatureArray();
        double[] actual = {featArray[110], featArray[111], featArray[112], featArray[113], featArray[114]};

        assertArrayEquals(expected, actual);
    }

}
