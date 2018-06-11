package de.hhu.stups.neurob.core.features.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jannik Dunkelau
 */
public class IdentifierRelationsHandlerTest {

    @Test
    public void adjacencyContainsTest() {
        AdjacencyList al = new AdjacencyList();

        boolean actual = al.containsId("a");
        assertFalse(actual,
                "Containment check for nonexistent node failed");

        al.addNode("a");
        actual = al.containsId("a ");
        assertTrue(actual,
                "Containment check for existing node failed");
    }

    @Test
    public void adjacencyBoundariesTest() {
        AdjacencyList al = new AdjacencyList();
        String id = "a";
        al.addNode(id);

        al.addDomainBoundaries(id, true, false);
        al.addDomainBoundaries(id, false, false);

        boolean actual = al.getIdentifier(id).hasLowerBoundedDomain();
        assertTrue(actual,
                "Identifier should be lower bounded but is not");

        actual = al.getIdentifier(id).hasUpperBoundedDomain();
        assertFalse(actual,
                "Identifier should not be upper bounded but is");
    }

    @Test
    public void adjacencyRelationTest() {
        AdjacencyList al = new AdjacencyList();
        String id1 = "a", id2 = "b", id3 = "c";
        al.addNode(id1);
        al.addNode(id2);
        al.addNode(id3);

        al.addEdge(id1, id2);

        boolean actual = al.areInRelation(id1, id2);
        assertTrue(actual,
                "Related identifiers are stated as unrelated");

        actual = al.areInRelation(id2, id1);
        assertTrue(actual,
                "identifier relation is not symmetrical");

        actual = al.areInRelation(id1, id3);
        assertFalse(actual,
                "Unrelated identifiers are stated as related");
    }

    @Test
    public void adjacencyBoundedRelationTest() {
        AdjacencyList al = new AdjacencyList();
        String id1 = "a", id2 = "b", id3 = "c";
        al.addNode(id1);
        al.addNode(id2);
        al.addNode(id3);

        al.addLowerBoundRelation(id1, id2);

        boolean actual = al.getIdentifier(id1).hasLowerBoundaries();
        assertFalse(actual,
                "Identifier has no lower boundary but states so");

        actual = al.getIdentifier(id1).hasUpperBoundaries();
        assertTrue(actual,
                "Identifier has upper boundary but states otherwise");

        actual = al.getIdentifier(id2).hasLowerBoundaries();
        assertTrue(actual,
                "Identifier has implied lower boundary but states otherwise");

        al.addUpperBoundRelation(id1, id3);

        actual = al.getIdentifier(id1).hasLowerBoundaries();
        assertTrue(actual,
                "Identifier now has implied lower boundary but states otherwise");

        actual = al.getIdentifier(id3).hasUpperBoundaries();
        assertTrue(actual,
                "Identifier now has upper boundary but states otherwise");
    }

    @Test
    public void adjacencyLowerBoundedDomainRelationTest() {
        AdjacencyList al = new AdjacencyList();
        String id1 = "a", id2 = "b", id3 = "c";
        al.addNode(id1);
        al.addNode(id2);
        al.addNode(id3);

        al.addLowerBoundRelation(id1, id2);
        al.addLowerBoundRelation(id2, id3);

        boolean actual = al.getIdentifier(id1).hasUnboundedDomain();
        assertTrue(actual,
                "Identifier has no bounded domain, but states so");

        al.addDomainBoundaries(id1, true, false);

        actual = al.getIdentifier(id1).hasLowerBoundedDomain();
        assertTrue(actual,
                "Identifier has bounded domain, but states otherwise");

        actual = al.getIdentifier(id2).hasLowerBoundedDomain();
        assertTrue(actual,
                "Second identifier has implied bounded domain, but states otherwise");

        actual = al.getIdentifier(id3).hasLowerBoundedDomain();
        assertTrue(actual,
                "Third identifier has implied bounded domain, but states otherwise");
    }

    @Test
    public void adjacencyUpperBoundedDomainRelationTest() {
        AdjacencyList al = new AdjacencyList();
        String id1 = "a", id2 = "b", id3 = "c";
        al.addNode(id1);
        al.addNode(id2);
        al.addNode(id3);

        al.addUpperBoundRelation(id1, id2);
        al.addUpperBoundRelation(id2, id3);

        boolean actual = al.getIdentifier(id1).hasUnboundedDomain();
        assertTrue(actual,
                "Identifier has no bounded domain, but states so");

        al.addDomainBoundaries(id1, false, true);

        actual = al.getIdentifier(id1).hasUpperBoundedDomain();
        assertTrue(actual,
                "Identifier has bounded domain, but states otherwise");

        actual = al.getIdentifier(id2).hasUpperBoundedDomain();
        assertTrue(actual,
                "Second identifier has implied bounded domain, but states otherwise");

        actual = al.getIdentifier(id3).hasUpperBoundedDomain();
        assertTrue(actual,
                "Third identifier has implied bounded domain, but states otherwise");
    }

    @Test
    public void handlerRelationsTest() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifierRelation(id1, id2);

        boolean actual = h.containsIdRelation(id1, id2);
        assertTrue(actual,
                "Identifiers in relation are not stated as such");

        actual = h.containsIdRelation(id1, id3);
        assertFalse(actual,
                "Identifiers not relation are not stated as such");
    }

    @Test
    public void handlerRelationsCountTest() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifierRelation(id1, id2);
        h.addIdentifierRelation(id1, id2); // to check whether doubling this line influences result
        h.addIdentifierRelation(id1, id3);

        int expected = 2;
        int actual = h.getIdRelationsCount();

        assertEquals(expected, actual,
                "Amount of relations does not match");
    }

    @Test
    public void handlerBoundedDomainsCountTest() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b";

        h.addIdentifier(id1);
        h.addIdentifier(id2);

        h.addDomainBoundaries(id1, true, false);
        h.addDomainBoundaries(id2, false, true);
        h.addLowerBoundRelation(id1, id2);

        int expected = 2;
        int actual = h.getBoundedDomainsCount();
        assertEquals(expected, actual,
                "Amount of bounded domains does not match");

        // counter check
        expected = 0;
        actual = h.getSemiBoundedDomainsCount();
        assertEquals(expected, actual,
                "Amount of semi-bounded domains does not match");
    }

    @Test
    public void handlerSemiBoundedDomainsCountTest() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifier(id1);
        h.addIdentifier(id2);
        h.addIdentifier(id3);

        h.addDomainBoundaries(id1, true, false);
        h.addLowerBoundRelation(id1, id2);
        h.addLowerBoundRelation(id3, id1);

        int expected = 2;
        int actual = h.getSemiBoundedDomainsCount();
        assertEquals(expected, actual,
                "Amount of semi-bounded domains does not match");

        // counter check
        expected = 1;
        actual = h.getUnboundedDomainsCount();
        assertEquals(expected, actual,
                "Amount of unbounded domains does not match");
    }


    @Test
    public void handlerUnboundedDomainsCountTest() {
        IdentifierRelationsHandler h = new IdentifierRelationsHandler();
        String id1 = "a", id2 = "b", id3 = "c";

        h.addIdentifier(id1);
        h.addIdentifier(id2);
        h.addIdentifier(id3);

        h.addLowerBoundRelation(id1, id2);
        h.addLowerBoundRelation(id2, id3);

        int expected = 3;
        int actual = h.getUnboundedDomainsCount();
        assertEquals(expected, actual,
                "Amount of unbounded domains does not match");

        // counter check
        expected = 0;
        actual = h.getUnboundedIdCount();
        assertEquals(expected, actual,
                "Amount of unbounded ids does not match");
    }


}
