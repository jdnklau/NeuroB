package de.hhu.stups.neurob.core.features.predicates.util;

import de.be4.classicalb.core.parser.BParser;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;

import de.be4.classicalb.core.parser.node.Node;
import de.prob.animator.domainobjects.IBEvalElement;

/**
 * Collects {@link BAstFeatureData} over a given
 * {@link de.hhu.stups.neurob.core.api.bmethod.BPredicate}.
 */
public class BAstFeatureCollector {

    public static BAstFeatureData collect(BPredicate pred) throws FeatureCreationException {
        return collect(pred, null);
    }

    public static BAstFeatureData collect(BPredicate pred, MachineAccess bMachine)
            throws FeatureCreationException {
        Node ast;

        try {
            // Try to parse directly over the state space, if given
            if (bMachine != null) {
                ast = ((IBEvalElement) bMachine.parseFormula(pred)).getAst();
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
        BAstFeatureWalker walker = new BAstFeatureWalker();
        ast.apply(walker);
        return walker.getFeatureData();
    }
}
