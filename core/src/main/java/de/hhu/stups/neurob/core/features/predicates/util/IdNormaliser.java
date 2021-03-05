package de.hhu.stups.neurob.core.features.predicates.util;

import de.be4.classicalb.core.parser.analysis.DepthFirstAdapter;
import de.be4.classicalb.core.parser.node.AIdentifierExpression;
import de.be4.classicalb.core.parser.node.APrimedIdentifierExpression;
import de.be4.classicalb.core.parser.node.TIdentifierLiteral;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IdNormaliser extends DepthFirstAdapter {

    private int nextId = 0;
    private Map<String, String> idMappings = new HashMap<>();

    @Override
    public void inAIdentifierExpression(AIdentifierExpression node) {
        super.inAIdentifierExpression(node);
        node.setIdentifier(generaliseIds(node.getIdentifier()));
    }

    @Override
    public void inAPrimedIdentifierExpression(APrimedIdentifierExpression node) {
        super.inAPrimedIdentifierExpression(node);
        node.setIdentifier(generaliseIds(node.getIdentifier()));
    }

    List<TIdentifierLiteral> generaliseIds(List<TIdentifierLiteral> ids) {
        int count = ids.size();
        List<TIdentifierLiteral> normIds = new LinkedList<>();

        for (TIdentifierLiteral id : ids) {
            String idName = id.getText();
            TIdentifierLiteral normId;
            if (!idMappings.containsKey(idName)) {
                String idMapping = "id" + (nextId++);
                idMappings.put(idName, idMapping);
            }
            normId = new TIdentifierLiteral(idMappings.get(idName));
            normIds.add(normId);
        }

        return normIds;
    }
}
