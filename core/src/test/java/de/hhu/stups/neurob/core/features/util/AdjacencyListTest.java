package de.hhu.stups.neurob.core.features.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdjacencyListTest {

    @Test
    public void shouldNotContainNonexistingNode() {
        AdjacencyList al = new AdjacencyList();

        boolean actual = al.containsId("a");
        assertFalse(actual,
                "Containment check for nonexistent node failed");
    }

    @Test
    public void shouldReplyIdIsContainedIndependenOfSurroundingWhiteSpace() {
        AdjacencyList al = new AdjacencyList();

        al.addNode("a");
        assertAll("Contained node not correctly recognised",
                () -> assertTrue(al.containsId("a"),
                        "Does not contain without white space"),
                () -> assertTrue(al.containsId("a "),
                        "Does not contain with trailing space"),
                () -> assertTrue(al.containsId(" a"),
                        "Does not contain with trailing space"),
                () -> assertTrue(al.containsId(" a "),
                        "Does not contain with surrounding space")

        );
    }

    @Test
    public void shouldBeUnboundWhenIdentifierIsNew() {
        AdjacencyList al = new AdjacencyList();
        String id = "a";
        al.addNode(id);

        assertAll("New identifier has boundings",
                () -> assertFalse(al.getIdentifier("a").hasLowerBoundedDomain(),
                        "New identifier has lower bounded domain"),
                () -> assertFalse(al.getIdentifier("a").hasUpperBoundedDomain(),
                        "New identifier has upper bounded domain"),
                () -> assertFalse(al.getIdentifier("a").hasLowerBoundaries(),
                        "New identifier has lower boundaries"),
                () -> assertFalse(al.getIdentifier("a").hasUpperBoundaries(),
                        "New identifier has upper boundaries"),
                () -> assertFalse(al.getIdentifier("a").hasBoundedDomain(),
                        "New identifier has bounded domain"),
                () -> assertTrue(al.getIdentifier("a").isUnbounded(),
                        "New identifier is not unbounded")
        );
    }

    @Test
    public void shouldBeLowerButNotUpperBounded() {
        AdjacencyList al = new AdjacencyList();
        String id = "a";
        al.addNode(id);

        al.addDomainBoundaries(id, true, false);
        al.addDomainBoundaries(id, false, false);

        assertAll(
                () -> assertTrue(al.getIdentifier(id).hasLowerBoundedDomain(),
                        "Identifier should be lower bounded but is not"),
                () -> assertFalse(al.getIdentifier(id).hasUpperBoundedDomain(),
                        "Identifier should not be upper bounded but is")
        );
    }

    @Test
    public void shouldRelateTwoIdsInGraphOfThree() {
        AdjacencyList al = new AdjacencyList();
        String id1 = "a", id2 = "b", id3 = "c";
        al.addNode(id1);
        al.addNode(id2);
        al.addNode(id3);

        al.addEdge(id1, id2);

        assertAll(
                () -> assertTrue(al.areInRelation(id1, id2),
                        "Related identifiers are stated as unrelated"),
                () -> assertTrue(
                        al.areInRelation(id2, id1) && al.areInRelation(id1, id2),
                        "Identifier relation should be symmetrical"),
                () -> assertFalse(al.areInRelation(id1, id3),
                        "Unrelated identifiers are stated as related")
        );
    }

    @Test
    public void shouldSetBoundariesTransitivelyForThreeIds() {
        AdjacencyList al = new AdjacencyList();
        String id1 = "a", id2 = "b", id3 = "c";
        al.addNode(id1);
        al.addNode(id2);
        al.addNode(id3);

        al.addLowerBoundRelation(id1, id2); // a < b
        al.addUpperBoundRelation(id1, id3); // a > c

        assertAll(
                () -> assertTrue(al.getIdentifier(id1).hasUpperBoundaries(),
                        "Identifier should have upper boundary"),
                () -> assertTrue(al.getIdentifier(id2).hasLowerBoundaries(),
                        "Identifier should have lower boundary"),
                () -> assertTrue(al.getIdentifier(id1).hasLowerBoundaries(),
                        "Identifier should have lower boundary"),
                () -> assertTrue(al.getIdentifier(id3).hasUpperBoundaries(),
                        "Identifier should have upper boundary"),
                () -> assertTrue(al.areInRelation(id2, id3),
                        "Identifiers should be transitively related")
        );
    }

    @Test
    public void shouldPropagateLowerBoundedDomainToUpperBoundaries() {
        AdjacencyList al = new AdjacencyList();
        String id1 = "a", id2 = "b", id3 = "c";
        al.addNode(id1);
        al.addNode(id2);
        al.addNode(id3);

        al.addLowerBoundRelation(id1, id2); // a < b
        al.addLowerBoundRelation(id2, id3); // b < c
        // Set lower bound to domain of a
        al.addDomainBoundaries(id1, true, false);

        assertAll(
                () -> assertTrue(al.getIdentifier(id1).hasLowerBoundedDomain(),
                        "Identifier should have lower bounded domain"),
                () -> assertTrue(al.getIdentifier(id2).hasLowerBoundedDomain(),
                        "Second identifier should have implied bounded domain"),
                () -> assertTrue(al.getIdentifier(id3).hasLowerBoundedDomain(),
                        "Third identifier should have implied bounded domain")
        );
    }

    @Test
    public void shouldPropagateUpperBoundedDomainToLowerBoundaries() {
        AdjacencyList al = new AdjacencyList();
        String id1 = "a", id2 = "b", id3 = "c";
        al.addNode(id1);
        al.addNode(id2);
        al.addNode(id3);

        al.addUpperBoundRelation(id1, id2); // a > b
        al.addUpperBoundRelation(id2, id3); // b > c
        // Set upper bound to a's domain
        al.addDomainBoundaries(id1, false, true);

        assertAll(
                () -> assertTrue(al.getIdentifier(id1).hasUpperBoundedDomain(),
                        "Identifier should have bounded domain"),
                () -> assertTrue(al.getIdentifier(id2).hasUpperBoundedDomain(),
                        "Second identifier should have implied bounded domain"),
                () -> assertTrue(al.getIdentifier(id3).hasUpperBoundedDomain(),
                        "Third identifier should have implied bounded domain")
        );
    }
}