package de.hhu.stups.neurob.core.features.predicates;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.node.Node;
import de.be4.classicalb.core.parser.util.PrettyPrinter;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.predicates.util.GenericNormaliser;
import de.hhu.stups.neurob.core.features.predicates.util.IdNormaliser;
import de.hhu.stups.neurob.core.features.predicates.util.NeuroBPrettyPrinter;
import de.prob.animator.domainobjects.IBEvalElement;

import javax.annotation.Nullable;

/**
 * Generically normalised form of predicates.
 * <p>
 * Normalisation routine:
 * <ul>
 *     <li>Each identifier is mapped to an enumerated "idX" with X being a natural
 *         number starting from 0. Each identifier gets their unique id and X.</li>
 * </ul>
 */
public class NormalisedPredicate extends PredicateFeatures {

    public NormalisedPredicate(String pred) {
        this(BPredicate.of(pred));
    }

    public NormalisedPredicate(BPredicate pred) {
        super(pred, vectorizePred(pred));
    }

    private static Double[] vectorizePred(BPredicate pred) {
        String raw = pred.getPredicate().trim();
        Double[] vec = new Double[raw.length()];

        for (int i = 0; i < raw.length(); i++) {
            vec[i] = (double) raw.charAt(i);
        }

        return vec;
    }

    public static class Generator implements PredicateFeatureGenerating<NormalisedPredicate> {

        @Override
        public NormalisedPredicate generate(BPredicate pred, @Nullable MachineAccess bMachine) throws FeatureCreationException {
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
            ast.apply(new IdNormaliser());
            NeuroBPrettyPrinter prettyPrinter = new NeuroBPrettyPrinter();
            ast.apply(prettyPrinter);

            return new NormalisedPredicate(prettyPrinter.getPrettyPrint());
        }
    }
}
