package de.hhu.stups.neurob.core.features.predicates.util;

import de.hhu.stups.neurob.core.features.predicates.TheoryFeatures;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Data object for collecting the feature values for
 * {@link TheoryFeatures}.
 */
public class TheoryFeatureData {
    // Dimensions
    public static final int featureCount = 17;
    // Helpers
    protected IdentifierRelationsHandler ids;
    // Features
    protected int fExistsQuantifiersCount; // number of existential quantifiers
    protected int fForAllQuantifiersCount; // number of universal quantifiers
    protected int fArithmOperatorsCount; // number of arithmetic operators
    protected int fCompOperatorsCount; // number of comparison operators
    protected int fConjunctionsCount; // number of conjunctions
    protected int fDisjunctionsCount; // number of disjunctions
    protected int fNegationsCount; // number of negations
    protected int fSetOperatorsCount; // number of set operators
    protected int fSetMemberCount; // number of memberships to sets
    protected int fFunctionsCount; // number of functions
    protected int fRelationOperatorsCount;
    protected int fImplicationsCount; // count implications used (=>)
    protected int fEquivalencesCount; // count equivalences used (<=>)

    public TheoryFeatureData() {
        // Quantifiers
        fExistsQuantifiersCount = 0;
        fForAllQuantifiersCount = 0;
        // Operators
        fArithmOperatorsCount = 0;
        fCompOperatorsCount = 0;
        fConjunctionsCount = 0;
        fDisjunctionsCount = 0;
        fNegationsCount = 0;
        // implications
        fImplicationsCount = 0;
        fEquivalencesCount = 0;
        // identifiers
        ids = new IdentifierRelationsHandler();
        // Sets
        fSetOperatorsCount = 0;
        fSetMemberCount = 0;
        // functions
        fFunctionsCount = 0;
        fRelationOperatorsCount = 0;
    }

    @Override
    public String toString() {
        return Arrays.stream(toArray())
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    public final Double[] toArray() {
        Double[] features = new Double[]{
                (double) fArithmOperatorsCount,
                (double) fCompOperatorsCount,
                (double) fForAllQuantifiersCount,
                (double) fExistsQuantifiersCount,
                (double) fConjunctionsCount,
                (double) fDisjunctionsCount,
                (double) fNegationsCount,
                (double) fSetOperatorsCount,
                (double) fSetMemberCount,
                (double) fFunctionsCount,
                (double) fRelationOperatorsCount,
                (double) getUniqueIdentifiersCount(),
                (double) ids.getBoundedDomainsCount(),
                (double) ids.getSemiBoundedDomainsCount() + ids.getUnboundedDomainsCount(),
                (double) ids.getUnknownTypedCount(),
                (double) fImplicationsCount,
                (double) fEquivalencesCount
        };
        return features;
    }

    public final int getExistsQuantifiersCount() {
        return fExistsQuantifiersCount;
    }

    public final void incExistsQuantifiersCount() {
        fExistsQuantifiersCount++;
    }

    public final int getForAllQuantifiersCount() {
        return fForAllQuantifiersCount;
    }

    public final void incForAllQuantifiersCount() {
        fForAllQuantifiersCount++;
    }

    public final int getArithmOperatorsCount() {
        return fArithmOperatorsCount;
    }

    public final void incArithmOperatorsCount() {
        fArithmOperatorsCount++;
    }

    public final int getCompOperatorsCount() {
        return fCompOperatorsCount;
    }

    public final void incCompOperatorsCount() {
        fCompOperatorsCount++;
    }

    public final int getConjunctionsCount() {
        return fConjunctionsCount;
    }

    public final void incConjunctionsCount() {
        fConjunctionsCount++;
    }

    public final int getDisjunctionsCount() {
        return fDisjunctionsCount;
    }

    public final void incDisjunctionsCount() {
        fDisjunctionsCount++;
    }

    public final int getNegationsCount() {
        return fNegationsCount;
    }

    public final void incNegationsCount() {
        fNegationsCount++;
    }

    public final int getSetOperatorsCount() {
        return fSetOperatorsCount;
    }

    public final void incSetOperatorsCount() {
        fSetOperatorsCount++;
    }

    public final int getSetMemberCount() {
        return fSetMemberCount;
    }

    public final void incSetMemberCount() {
        fSetMemberCount++;
    }

    public final int getFunctionsCount() {
        return fFunctionsCount;
    }

    public final void incFunctionsCount() {
        fFunctionsCount++;
    }

    public final int getRelationOperatorsCount() {
        return fRelationOperatorsCount;
    }

    public final void incRelationOperatorsCount() {
        fRelationOperatorsCount++;
    }

    public final int getUniqueIdentifiersCount() {
        return ids.getIdCount();
    }

    public final void addIdentifier(String id) {
        ids.addIdentifier(id);
    }

    public final int getImplicationsCount() {
        return fImplicationsCount;
    }

    public final void incImplicationsCount() {
        fImplicationsCount++;
    }

    public final int getEquivalencesCount() {
        return fEquivalencesCount;
    }

    public final void incEquivalencesCount() {
        fEquivalencesCount++;
    }

    public final void setIdentifierDomain(String id, boolean hasLowerBound,
            boolean hasUpperBound) {
        ids.addDomainBoundaries(id, hasLowerBound, hasUpperBound);
    }

    public final void setUpperBoundRelationToIdentifier(String id) {
        ids.addDomainBoundaries(id, false, true);
    }

    public final void setUpperBoundRelationToIdentifier(String id,
            String restrictingID) {
        // Not implemented at point of MA, maybe should not make it into new version?
//        ids.addLowerBoundRelation(id, restrictingID);
    }

    public final void setLowerBoundRelationToIdentifier(String id) {
        ids.addDomainBoundaries(id, true, false);
    }

    public final void setLowerBoundRelationToIdentifier(String id,
            String restrictingID) {
        // Not implemented at point of MA, maybe should not make it into new version?
//        ids.addUpperBoundRelation(id, restrictingID);
    }

    public final void setIdentifierDomainTypeKnowledge(String id,
            boolean isKnownType) {
        ids.addTypeKnowledge(id, isKnownType);
    }

}
