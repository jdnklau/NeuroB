package de.hhu.stups.neurob.core.features.predicates.util;

import de.be4.classicalb.core.parser.node.AIntegerExpression;
import de.be4.classicalb.core.parser.node.TIntegerLiteral;
import de.prob.unicode.node.TIn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BAstFeatureWalkerTest {

    @Test
    void shouldSetMaxIntegerValueWhenLiteralIsGreater() {
        BAstFeatureWalker walker = new BAstFeatureWalker();

        TIntegerLiteral literal = new TIntegerLiteral("112500000000000000000000000000000000000000000000000000");
        AIntegerExpression node = new AIntegerExpression(literal);

        walker.outAIntegerExpression(node);

        int expected = Integer.MAX_VALUE;
        int actual = walker.getFeatureData().getMaxIntegerUsed();

        assertEquals(expected, actual);
    }

    @Test
    void shouldSetMinIntegerValueWhenLiteralIsLesser() {
        BAstFeatureWalker walker = new BAstFeatureWalker();

        TIntegerLiteral literal = new TIntegerLiteral("-112500000000000000000000000000000000000000000000000000");
        AIntegerExpression node = new AIntegerExpression(literal);

        walker.outAIntegerExpression(node);

        int expected = Integer.MIN_VALUE;
        int actual = walker.getFeatureData().getMaxIntegerUsed();

        assertEquals(expected, actual);
    }

}
