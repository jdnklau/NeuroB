package de.hhu.stups.neurob.core.features.predicates.util;

import java.io.StringWriter;

import de.be4.classicalb.core.parser.node.*;
import de.be4.classicalb.core.parser.util.BasePrettyPrinter;


/**
 * Pretty Printing class used to print B predicates. Extends the use of the default PrettyPrinter by fixing missing
 * Nodes to be supported.
 */
public class NeuroBPrettyPrinter extends BasePrettyPrinter {

    public NeuroBPrettyPrinter() {
        super(new StringWriter());
    }

    public String getPrettyPrint() {
        this.flush();
        return this.getWriter().toString();
    }

    @Override
    public void caseAMultiplicationExpression(AMultiplicationExpression node) {
        // We replace this node to ensure we get proper pretty printing. AMultiplicationExpression is currently not
        // supported by the BasePrettyPrinter.
        AMultOrCartExpression newNode = new AMultOrCartExpression(node.getLeft(), node.getRight());
        node.replaceBy(newNode);
        super.caseAMultOrCartExpression(newNode);
    }

    @Override
    public void caseAMinusExpression(AMinusExpression node) {
        AMinusOrSetSubtractExpression newNode = new AMinusOrSetSubtractExpression(node.getLeft(), node.getRight());
        node.replaceBy(newNode);
        super.caseAMinusOrSetSubtractExpression(newNode);
    }
}
