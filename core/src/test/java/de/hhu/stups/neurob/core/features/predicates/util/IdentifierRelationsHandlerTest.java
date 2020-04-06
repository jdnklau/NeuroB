package de.hhu.stups.neurob.core.features.predicates.util;

import de.hhu.stups.neurob.core.features.predicates.util.IdentifierRelationsHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IdentifierRelationsHandlerTest {


    @Test
    public void shouldNotRelateThirdIdWhenOnlyFirstTwoAreStatedAsRelated() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifierRelation(id1, id2);

        assertAll(
                () -> assertTrue(h.containsIdRelation(id1, id2),
                        "Identifiers in relation are not stated as such"),
                () -> assertFalse(h.containsIdRelation(id1, id3),
                        "Identifiers not in relation are not stated as such")
        );
    }

    @Test
    public void shouldNotCountTheSameRelationTwice() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b";

        h.addIdentifierRelation(id1, id2);
        h.addIdentifierRelation(id1, id2);
        h.addIdentifierRelation(id2, id1); // switched position of a and b

        int expected = 1;
        int actual = h.getIdRelationsCount();

        assertEquals(expected, actual,
                "Amount of relations does not match");
    }

    @Test
    public void shouldIdentifyLowerDomainBoundaryAsSemiBoundedDomain() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id = "a";

        h.addDomainBoundaries(id, true, false);

        assertEquals(1, h.getSemiBoundedDomainsCount(),
                "Identifier should be semi-bounded");
    }

    @Test
    public void shouldIdentifyUpperDomainBoundaryAsSemiBoundedDomain() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id = "a";

        h.addDomainBoundaries(id, false, true);

        assertEquals(1, h.getSemiBoundedDomainsCount(),
                "Identifier should be semi-bounded");
    }

    @Test
    public void shouldCountBoundedDomainsNotAsSemiBoundedDomainsAsWell() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b";

        h.addIdentifier(id1);
        h.addIdentifier(id2);

        h.addDomainBoundaries(id1, true, false);
        h.addDomainBoundaries(id2, false, true);
        h.addLowerBoundRelation(id1, id2);

        assertAll(
                () -> assertEquals(2, h.getBoundedDomainsCount(),
                        "Amount of bounded domains does not match"),
                () -> assertEquals(0, h.getSemiBoundedDomainsCount(),
                        "Should not have any semi-bounded domains")
        );
    }

    @Test
    public void shouldCountSemiBoundedDomainsNotAsUnboundedAsWell() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifier(id1);
        h.addIdentifier(id2);
        h.addIdentifier(id3);

        h.addDomainBoundaries(id1, true, false);
        h.addLowerBoundRelation(id1, id2);
        h.addLowerBoundRelation(id3, id1);

        assertAll(
                () -> assertEquals(2, h.getSemiBoundedDomainsCount(),
                        "Amount of semi-bounded domains does not match"),
                () -> assertEquals(1, h.getUnboundedDomainsCount(),
                        "Amount of unbounded domains does not match")
        );
    }

    @Test
    public void shouldNotTreatLowerBoundRelationsAsDomainBoundaries() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifier(id1);
        h.addIdentifier(id2);
        h.addIdentifier(id3);

        h.addLowerBoundRelation(id1, id2);
        h.addLowerBoundRelation(id2, id3);

        assertAll(
                () -> assertEquals(3, h.getUnboundedDomainsCount(),
                        "Amount of unbounded domains does not match"),
                () -> assertEquals(0, h.getUnboundedIdCount(),
                        "Amount of unbounded ids does not match")
        );
    }

    @Test
    public void shouldCountThreeUnknownTypes() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifier(id1);
        h.addIdentifier(id2);
        h.addIdentifier(id3);

        assertEquals(3, h.getUnknownTypedCount(),
                "Amount of unknown typed ids does not match");
    }

    @Test
    public  void shouldCountTwoKnownTypes() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifier(id1);
        h.addIdentifier(id2);
        h.addIdentifier(id3);

        h.addUpperBoundRelation(id1, id2); // a < b
        h.addTypeKnowledge(id1, true);

        assertAll(
                () -> assertEquals(2, h.getKnownTypedCount(),
                        "Identifiers a and b should be of known type"),
                () -> assertEquals(1, h.getUnknownTypedCount(),
                        "Identifier c should be of unknown type")
        );
    }


}
