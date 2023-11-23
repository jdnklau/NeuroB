package de.hhu.stups.neurob.core.features.predicates;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.ParseOptions;
import de.be4.classicalb.core.parser.node.Node;
import de.be4.classicalb.core.parser.util.PrettyPrinter;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.predicates.util.BAstFeatureWalker;
import de.hhu.stups.neurob.core.features.predicates.util.GenericNormaliser;
import de.prob.animator.domainobjects.EventBParserBase;
import de.prob.animator.domainobjects.IBEvalElement;

import javax.annotation.Nullable;

/**
 * Generically normalised form of predicates.
 *
 * Normalisation routine:
 * <ul>
 *     <li>Each identifier is mapped to "idn"</li>
 * </ul>
 */
public class GenericNormalisedPredicate extends PredicateFeatures {
    private final BPredicate pred;

    public GenericNormalisedPredicate(String pred) {
        this(BPredicate.of(pred));
    }

    public GenericNormalisedPredicate(BPredicate pred) {
        super(vectorizePred(pred));
        this.pred = pred;
    }

    private static Double[] vectorizePred(BPredicate pred) {
        String raw = pred.getPredicate().trim();
        Double[] vec = new Double[raw.length()];

        for (int i = 0; i < raw.length(); i++) {
            vec[i] = (double) raw.charAt(i);
        }

        return vec;
    }

    public BPredicate getPred() {
        return pred;
    }

    public static class Generator implements PredicateFeatureGenerating<GenericNormalisedPredicate> {

        @Override
        public GenericNormalisedPredicate generate(BPredicate pred, @Nullable MachineAccess bMachine) throws FeatureCreationException {
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
            ast.apply(new GenericNormaliser());
            PrettyPrinter prettyPrinter = new PrettyPrinter();
            ast.apply(prettyPrinter);

            return new GenericNormalisedPredicate(prettyPrinter.getPrettyPrint());
        }
    }
}
