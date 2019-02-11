package de.hhu.stups.neurob.core.features.predicates.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adjacency list implementation for aggregating information about identifiers
 * in predicates.
 */
public class AdjacencyList {
    private Map<String, AdjacencyNode> idMap;

    public AdjacencyList() {
        idMap = new HashMap<>();
    }

    /**
     * Add an identifier as node to the adjacency list.
     * If the identifier is already present, this does nothing.
     *
     * @param identifier Identifier to add
     */
    public void addNode(String identifier) {
        identifier = identifier.trim();
        if (!idMap.containsKey(identifier)) {
            idMap.put(identifier, new AdjacencyNode(identifier));
        }
    }

    /**
     * Checks whether the given identifier is already present in the adjacency
     * list
     *
     * @param id Identifier to check for
     *
     * @return true iff the identifier is already present
     */
    public boolean containsId(String id) {
        return idMap.containsKey(id.trim());
    }

    /**
     * Add an edge between two already present identifiers
     *
     * @param id1 First identifier of edge
     * @param id2 Second identifier of edge
     */
    public void addEdge(String id1, String id2) {
        id1 = id1.trim();
        id2 = id2.trim();

        AdjacencyNode n1, n2;
        n1 = idMap.get(id1);
        n2 = idMap.get(id2);

        n1.addEdgeTo(n2);
        n2.addEdgeTo(n1);
    }

    /**
     * Add an edge between two identifiers, if not already present.
     * Further set id1 as lower bound for id2: id1 < id2
     *
     * @param id1 Lower bound for id2
     * @param id2 Upper bound for id1
     */
    public void addLowerBoundRelation(String id1, String id2) {
        id1 = id1.trim();
        id2 = id2.trim();

        addEdge(id1, id2);
        AdjacencyNode n1, n2;
        n1 = idMap.get(id1);
        n2 = idMap.get(id2);

        n1.addUpperBound(n2);
        n2.addLowerBound(n1);
    }

    /**
     * Add an edge between two identifiers, if not already present.
     * Further set id1 as upper bound for id2: id1 > id2
     *
     * @param id1 Upper bound for id2
     * @param id2 Lower bound for id1
     */
    public void addUpperBoundRelation(String id1, String id2) {
        id1 = id1.trim();
        id2 = id2.trim();

        addEdge(id1, id2);
        AdjacencyNode n1, n2;
        n1 = idMap.get(id1);
        n2 = idMap.get(id2);

        n1.addLowerBound(n2);
        n2.addUpperBound(n1);
    }

    /**
     * Sets whether a lower or upper bounding exists for the given identifier's
     * domain.
     * <p>
     * This is additive to already existing boundaries.
     * If the node already is lower bounded, and {@code addBoundaries(false,true)}
     * is called, it remains lower bounded.
     * </p>
     *
     * @param setLowerBound true iff the identifier shall be lower
     *         bounded
     * @param setUpperBound true iff the identifier shall be upper
     *         bounded
     */
    public void addDomainBoundaries(String id, boolean setLowerBound, boolean setUpperBound) {
        idMap.get(id.trim()).addDomainBoundaries(setLowerBound, setUpperBound);
    }

    public boolean areInRelation(String id1, String id2) {
        id1 = id1.trim();
        id2 = id2.trim();

        // if an identifier does not exist, they are obviously not in relation
        if (!containsId(id1) || !containsId(id2))
            return false;

        AdjacencyNode n1 = getIdentifier(id1);
        AdjacencyNode n2 = getIdentifier(id2);
        return n1.getRelatedIds().contains(n2);
    }

    /**
     * Add knowledge of whether the domain type of {@code id} is a known one.
     * <p>
     * What exactly a known type is, is up to the semantics of the algorithm
     * making use of this. E.g. a feature set might only accept native B types
     * as known ones, where as another might also accept enumerated sets.
     * </p>
     * <p>
     * The knowledge is added cumulatively, i.e. once set to true it should
     * not fall back to false.
     * </p>
     *
     * @param id
     * @param isDomainTypeKnown
     */
    public void addTypeKnowledge(String id, boolean isDomainTypeKnown) {
        getIdentifier(id).setTypeKnown(isDomainTypeKnown);
    }

    /**
     * @param id
     * @return Whether {@code id} has its domain type marked as being known.
     */
    public boolean hasKnownType(String id) {
        return getIdentifier(id).hasKnownType();
    }

    public AdjacencyNode getIdentifier(String id) {
        return idMap.get(id.trim());
    }

    public Set<String> getIdentiferSet() {
        return idMap.keySet();
    }

    public Set<AdjacencyNode> getNodeSet() {
        return idMap.keySet().stream().map(idMap::get).collect(Collectors.toSet());
    }

    public static class AdjacencyNode {
        private String id;
        // boundaries
        /** Whether the domain is lower bounded */
        private boolean hasLowerBoundedDomain = false;
        /** Whether the domain is upper bounded */
        private boolean hasUpperBoundedDomain = false;
        /** Whether other identifiers pose a lower bound */
        private boolean hasLowerBoundaries = false;
        /** Whether other identifiers pose an upper bound */
        private boolean hasUpperBoundaries = false;
        private Set<AdjacencyNode> lowerBoundaries;
        private Set<AdjacencyNode> upperBoundaries;
        private Set<AdjacencyNode> relatedIds;
        /** Whether the type of the identifier is known or not */
        private boolean hasKnownType = false;

        public AdjacencyNode(String identifier) {
            id = identifier.trim();

            lowerBoundaries = new HashSet<>();
            upperBoundaries = new HashSet<>();
            relatedIds = new HashSet<>();
        }

        /**
         * Sets whether a lower or upper bounding exists for the identifiers
         * domain.
         * <p>
         * This is additive to already existing boundaries.
         * If the node already is lower bounded, and {@code
         * addBoundaries(false,true)}
         * is called, it remains lower bounded.
         * </p>
         *
         * @param setLowerBound true iff the identifier shall be lower
         *         bounded
         * @param setUpperBound true iff the identifier shall be upper
         *         bounded
         */
        public void addDomainBoundaries(boolean setLowerBound, boolean setUpperBound) {
            // get old values
            boolean hadLowerBoundedDomain = hasLowerBoundedDomain;
            boolean hadUpperBoundedDomain = hasUpperBoundedDomain;

            // update values
            this.hasLowerBoundedDomain = this.hasLowerBoundedDomain || setLowerBound;
            this.hasUpperBoundedDomain = this.hasUpperBoundedDomain || setUpperBound;

            // compare old with new, update related domains accordingly
            if (hadLowerBoundedDomain ^ hasLowerBoundedDomain) {
                // add lower domain boundary to upper boundaries
                upperBoundaries.forEach(n -> n.addDomainBoundaries(true, false));
            }
            if (hadUpperBoundedDomain ^ hasUpperBoundedDomain) {
                // add upper domain boundary to lower boundaries
                lowerBoundaries.forEach(n -> n.addDomainBoundaries(false, true));
            }
        }

        /**
         * The given node is set as lower boundary.
         *
         * @param node Restricting node
         */
        public void addLowerBound(AdjacencyNode node) {
            lowerBoundaries.add(node);
            addEdgeTo(node);
            hasLowerBoundaries = true;
            // new lower bound is also lower bound for all upper bounds
            upperBoundaries.stream()
                    .filter(n -> !n.getLowerBoundaries().contains(node))
                    .forEach(n -> n.addLowerBound(node));
            // lower boundaries of node are lower boundaries of this
            node.getLowerBoundaries().stream()
                    .filter(n -> !lowerBoundaries.contains(n))
                    .forEach(this::addLowerBound);
            // update domains if necessary
            if (!hasLowerBoundedDomain && node.hasLowerBoundedDomain())
                addDomainBoundaries(true, false);
        }

        /**
         * The given node is set as upper boundary.
         *
         * @param node Restricting node
         */
        public void addUpperBound(AdjacencyNode node) {
            upperBoundaries.add(node);
            addEdgeTo(node);
            hasUpperBoundaries = true;
            // new upper bound is also upper bound for all lower bounds
            lowerBoundaries.stream()
                    .filter(n -> !n.getUpperBoundaries().contains(node))
                    .forEach(n -> n.addUpperBound(node));
            // lower boundaries of node are upper boundaries of this
            node.getUpperBoundaries().stream()
                    .filter(n -> !upperBoundaries.contains(n))
                    .forEach(this::addUpperBound);
            // update domains if necessary
            if (!hasUpperBoundedDomain && node.hasUpperBoundedDomain())
                addDomainBoundaries(false, true);
        }

        /**
         * Adds the given node to the set of related Ids
         *
         * @param node Node to be connected
         */
        public void addEdgeTo(AdjacencyNode node) {
            if (!this.equals(node)) // never add edge to yourself
                relatedIds.add(node);
        }

        /**
         * @return Set of identifiers defining a lower boundary for this
         *         identifier
         */
        public Set<AdjacencyNode> getLowerBoundaries() {
            return lowerBoundaries;
        }

        /**
         * @return Set of identifiers defining an upper boundary for this
         *         identifier
         */
        public Set<AdjacencyNode> getUpperBoundaries() {
            return upperBoundaries;
        }

        /**
         * <p>
         * Returns a set of identifiers, that are in relation to this node.
         * This is a superset of the union of lower bounding and upper bounding
         * identifiers.
         * </p>
         *
         * @return Set of identifiers in relation with this identifier
         */
        public Set<AdjacencyNode> getRelatedIds() {
            return relatedIds;
        }

        public boolean hasLowerBoundedDomain() {
            return hasLowerBoundedDomain;
        }

        public boolean hasUpperBoundedDomain() {
            return hasUpperBoundedDomain;
        }

        /**
         * Returns whether an other identifier poses a lower boundary.
         * May be true, even if the domain has no such boundary.
         *
         * @return true iff other identifier poses a lower boundary to this.
         */
        public boolean hasLowerBoundaries() {
            return hasLowerBoundaries;
        }

        /**
         * Returns whether an other identifier poses an upper boundary.
         * May be true, even if the domain has no such boundary.
         *
         * @return true iff other identifier poses an upper boundary to this.
         */
        public boolean hasUpperBoundaries() {
            return hasUpperBoundaries;
        }

        /**
         * Returns whether the domain of this identifier has a lower and an
         * upper boundary
         *
         * @return true iff domain has lower and upper boundary
         */
        public boolean hasBoundedDomain() {
            return hasLowerBoundedDomain && hasUpperBoundedDomain;
        }

        /**
         * Returns whether the domain of this identifier has either a lower or
         * an upper boundary
         *
         * @return true iff domain has either lower or upper boundary, not both
         */
        public boolean hasSemiBoundedDomain() {
            return hasLowerBoundedDomain ^ hasUpperBoundedDomain;
        }

        /**
         * @return ture iff neither has lower nor upper bounded domain
         */
        public boolean hasUnboundedDomain() {
            return !(hasLowerBoundedDomain || hasUpperBoundedDomain);
        }

        /**
         * @return true iff this is both lower or upper bounded
         */
        public boolean isBounded() {
            return hasLowerBoundaries && hasUpperBoundaries;
        }

        /**
         * @return true iff this node is only bounded either by lower or by
         *         upper bound, not both
         */
        public boolean isSemiBounded() {
            return hasLowerBoundaries ^ hasUpperBoundaries;
        }

        /**
         * @return true iff neither has lower nor upper bounds
         */
        public boolean isUnbounded() {
            return !(hasLowerBoundaries || hasUpperBoundaries);
        }

        /**
         * Set information about whether the type of the variable is known or
         * not.
         * <p>
         * Once true, the type cannot be unknown again. Update rule is
         * <code>hasKnownType = hasKnownType | known</code>.
         * </p>
         *
         * @param known
         */
        public void setTypeKnown(boolean known) {
            hasKnownType = hasKnownType | known;
            // propagate information through boundary hierarchy
            lowerBoundaries.stream()
                    .filter(id -> hasKnownType != id.hasKnownType)
                    .forEach(id -> id.setTypeKnown(true));
            upperBoundaries.stream()
                    .filter(id -> hasKnownType != id.hasKnownType)
                    .forEach(id -> id.setTypeKnown(true));
        }

        /**
         * @return Whether the domain of the identifier is of known type.
         */
        public boolean hasKnownType() {
            return hasKnownType;
        }

        public String getId() {
            return id;
        }


        @Override
        public String toString() {
            return id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof AdjacencyNode) {
                return id.equals(((AdjacencyNode) o).getId());
            }
            return false;
        }
    }
}
