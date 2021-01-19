package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BAst185FeaturesTest {

    @Test
    void shouldGenerateFeaturesForPredicate() throws FeatureCreationException {
        String rawPred =
                "(users<:USERS) & (groups<:GROUPS) & (files<:FILES) & (open:files+->users) & "
                + "(ingroup:users<->groups) & (access:files-->groups) & "
                + "(!f.(f:dom(open) => access(f):ingroup[{open(f)}])) & "
                + "(not(((user,group):ingroup)) => (group:GROUPS) & (group/:groups))";
        BPredicate pred = BPredicate.of(rawPred);

        BAst185Features features = new BAst185Features.Generator().generate(pred);

        Double[] expected = {
                0.375, 0.125, 0.125, 0.9999990000010001, 0.125, 0.0, 0.25, 0.0, 0.375, 0.0, 0.0,
                0.0, 0.125, 0.125, 0.125000125, 0.0, 0.9999990000010001, 0.9999990000010001, 0.0,
                0.0, 1.5, 0.0, 0.0, 0.0, 1.5, 0.0, 0.0, 1.5, 0.0, 0.0, 0.0, 0.9999999166666736, 0.0,
                0.0, 0.9999999166666736, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.25E-7, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.75, 0.25, 0.375, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.375000125, 1.25E-7, 0.5454544958677732,
                0.1818181652892577, 0.2727272479338866, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.125, 0.0, 0.0, 0.0, 0.125, 0.0, 0.0,
                0.0, 0.0, 0.125, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.125000125, 0.250000125,
                0.9999990000010001, 0.0, 0.0, 0.0, 0.499999750000125, 0.0, 0.0, 0.0, 0.0,
                0.499999750000125, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.125, 0.125, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.25, 0.250000125, 0.500000125, 0.499999750000125,
                0.499999750000125, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.49999987500003124, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.25E-7, 1.25E-7,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        };
        Double[] actual = features.getFeatureArray();

        assertArrayEquals(expected, actual);
    }

}
