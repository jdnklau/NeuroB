package de.hhu.stups.neurob.core.features.predicates;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.node.Node;
import de.be4.classicalb.core.parser.util.PrettyPrinter;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.predicates.util.IdNormaliser;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.prolog.output.PrologTermStringOutput;

import javax.annotation.Nullable;

/**
 * Use the non-typed Prolog AST from the Java parser as features.
 */
public class PrologBAst extends PredicateFeatures {
    private final String ast;

    public PrologBAst(String ast) {
        super(vectorizePred(ast));
        this.ast = ast;
    }

    private static Double[] vectorizePred(String pred) {
        String raw = pred.trim();
        Double[] vec = new Double[raw.length()];

        for (int i = 0; i < raw.length(); i++) {
            vec[i] = (double) raw.charAt(i);
        }

        return vec;
    }

    public String getAst() {
        return ast;
    }

    public static class Generator implements PredicateFeatureGenerating<PrologBAst> {

        @Override
        public PrologBAst generate(BPredicate pred, @Nullable MachineAccess bMachine) throws FeatureCreationException {
            IBEvalElement ast;

            boolean closeEmptyAccess = false;
            try {
                // Try to parse directly over the state space, if given
                if (bMachine == null) {
                    bMachine = BMachine.EMPTY.spawnMachineAccess();
                    closeEmptyAccess = true;
                }
                ast = ((IBEvalElement) bMachine.parseFormula(pred));

            } catch (Exception e) {
                throw new FeatureCreationException(
                        "Unable to parse predicate" + pred, e);
            } finally {
                if (closeEmptyAccess) {
                    bMachine.close();
                }
            }

            var prolog = new PrologTermStringOutput();
            ast.printProlog(prolog);

            return new PrologBAst(prolog.toString());
        }
    }
}
