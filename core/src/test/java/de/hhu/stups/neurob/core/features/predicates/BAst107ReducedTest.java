package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BAst107ReducedTest {

    @Test
    void shouldMatchCount() throws FeatureCreationException {

        String rawPred =
                "(users<:USERS) & (groups<:GROUPS) & (files<:FILES) & (open:files+->users) & "
                + "(ingroup:users<->groups) & (access:files-->groups) & "
                + "(!f.(f:dom(open) => access(f):ingroup[{open(f)}])) & "
                + "(not(((user,group):ingroup)) => (group:GROUPS) & (group/:groups))";
        BPredicate pred = BPredicate.of(rawPred);

        BAst107Reduced features = new BAst107Reduced.Generator().generate(pred);

        assertEquals(107, features.getFeatureArray().length);
    }

}
