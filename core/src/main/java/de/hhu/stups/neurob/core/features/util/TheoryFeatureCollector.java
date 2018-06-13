package de.hhu.stups.neurob.core.features.util;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.node.Node;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.statespace.StateSpace;

/**
 * Utility class for collection data of
 * {@link de.hhu.stups.neurob.core.features.TheoryFeatures} from a predicate.
 */
public class TheoryFeatureCollector {

    public static TheoryFeatureData collect(String pred, StateSpace ss)
            throws FeatureCreationException {
        Node ast;

        try {
            // Try to parse directly over the state space, if given
            if (ss != null) {
                ast = ((IBEvalElement) ss.getModel().parseFormula(pred)).getAst();
            } else {
                BParser parser = new BParser();
                String input = BParser.PREDICATE_PREFIX + pred;
                ast = parser.parse(input, false, parser.getContentProvider());
            }
        } catch (Exception e) {
            throw new FeatureCreationException(
                    "Unable to parse predicate" + pred, e);
        }

        // Walk AST, return data
        TheoryFeatureAstWalker walker = new TheoryFeatureAstWalker();
        ast.apply(walker);
        return walker.getFeatureData();
    }

}
