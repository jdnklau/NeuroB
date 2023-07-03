package de.hhu.stups.neurob.core.features.predicates.util;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.AIdentifierExpression;
import de.be4.classicalb.core.parser.node.APrimedIdentifierExpression;
import de.be4.classicalb.core.parser.node.TIdentifierLiteral;

import java.util.LinkedList;
import java.util.List;

public class GenericNormaliser extends DepthFirstAdapter {

    @Override
    public void inAIdentifierExpression(AIdentifierExpression node) {
        super.inAIdentifierExpression(node);
        node.setIdentifier(generaliseIds(node.getIdentifier()));
    }

    @Override
    public void inAPrimedIdentifierExpression(APrimedIdentifierExpression node) {
        super.inAPrimedIdentifierExpression(node);
        node.replaceBy(new AIdentifierExpression(generaliseIds(node.getIdentifier())));
    }

    List<TIdentifierLiteral> generaliseIds(List<TIdentifierLiteral> ids) {
        int count = ids.size();
        List<TIdentifierLiteral> normIds = new LinkedList<>();

        TIdentifierLiteral normalisedId = new TIdentifierLiteral("idn");

        for (int i=0; i<count; i++) {
            normIds.add(normalisedId);
        }

        return normIds;
    }
}
