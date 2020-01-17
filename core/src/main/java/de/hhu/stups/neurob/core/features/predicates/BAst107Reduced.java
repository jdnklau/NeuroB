package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.predicates.util.AdjacencyList;
import de.hhu.stups.neurob.core.features.predicates.util.BAstFeatureCollector;
import de.hhu.stups.neurob.core.features.predicates.util.BAstFeatureData;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Reduced version of F275.
 */
public class BAst107Reduced extends PredicateFeatures {

    public static final int FEATURE_DIMENSION = 107;

    public BAst107Reduced(BPredicate predicate, Double[] features) {
        super(predicate, features);

        if (features.length != FEATURE_DIMENSION) {
            throw new IllegalArgumentException("Must have 275 features exactly.");
        }
    }

    public static class Generator implements PredicateFeatureGenerating<BAst107Reduced> {

        @Override
        public BAst107Reduced generate(BPredicate predicate, @Nullable MachineAccess machineAccess)
                throws FeatureCreationException {

            BAstFeatureData data = BAstFeatureCollector.collect(predicate, machineAccess);
            return new BAst107Reduced(predicate, generateArray(data));

        }

        public static Double[] generateArray(BAstFeatureData data) {
            // get some constants
            final double epsilon = 0.000001; // for division if something could be 0
            final double conjuncts = data.getConjunctsCount();
            final double arithmeticOps =
                    data.getArithmeticAdditionCount()
                    + data.getArithmeticDivisionCount()
                    + data.getArithmeticMultiplicationCount()
                    + data.getArithmeticModuloCount()
                    + data.getArithmeticExponentialCount()
                    + data.getArithmeticGeneralisedProductCount()
                    + data.getArithmeticGeneralisedSumCount()
                    + data.getSuccCount()
                    + data.getPredecCount()
                    + data.getSizeComparisonCount();
            final double quantifiers =
                    data.getExistentialQuantifiersCount()
                    + data.getUniversalQuantifiersCount();
            final double setBelongings =
                    data.getMemberCount()
                    + data.getNotMemberCount()
                    + data.getNotSubsetCount()
                    + data.getSubsetCount();
            final double setOperations =
                    data.getSetUnionCount() + data.getSetIntersectCount()
                    + data.getSetSubtractionCount()
                    + data.getSetGeneralIntersectCount()
                    + data.getSetGeneralUnionCount()
                    + data.getSetQuantifiedIntersectCount()
                    + data.getSetQuantifiedUnionCount();
            final double relations =
                    data.getRelationCount()
                    + data.getRelationTotalCount()
                    + data.getRelationSurjCount()
                    + data.getRelationTotalSurjCount();
            final double relationOps =
                    data.getRelationalImageCount()
                    + data.getRelationInverseCount()
                    + data.getRelationOverrideCount()
                    + data.getRelationDirectProductCount()
                    + data.getRelationParallelProductCount()
                    + data.getDomainCount()
                    + data.getRangeCount()
                    + data.getProjection1Count()
                    + data.getProjection2Count()
                    + data.getForwardCompositionCount()
                    + data.getDomainRestrictionCount()
                    + data.getDomainSubtractionCount()
                    + data.getRangeRestrictionCount()
                    + data.getRangeSubtractionCount();
            final double functions =
                    data.getFunPartialCount()
                    + data.getFunTotalCount()
                    + data.getFunPartialInjCount()
                    + data.getFunTotalInjCount()
                    + data.getFunPartialSurjCount()
                    + data.getFunTotalSurjCount()
                    + data.getFunPartialBijCount()
                    + data.getFunTotalBijCount()
                    + data.getLambdaCount();
            final double sequences =
                    +data.getSeqCount()
                    + data.getIseqCount();
            final double sequenceOps =
                    +data.getSizeCount()
                    + data.getFirstCount()
                    + data.getTailCount()
                    + data.getLastCount()
                    + data.getFrontCount()
                    + data.getRevCount()
                    + data.getPermCount()
                    + data.getConcatCount()
                    + data.getFrontInsertionCount()
                    + data.getTailInsertionCount()
                    + data.getFrontRestrictionCount()
                    + data.getTailRestrictionCount()
                    + data.getGeneralConcatCount();
            double ids = data.getIdentifiersCount();
            double logicOps = ((data.getConjunctionsCount() - conjuncts + 1)
                               + data.getDisjunctionsCount()
                               + data.getImplicationsCount()
                               + data.getEquivalencesCount());

            // setting up the data
            double[] features = {
                    // conjunct form
                    log2(conjuncts),
                    data.getMaxDepth() / conjuncts, // average conjunct depth
                    data.getNegationCount() / conjuncts, // average negations
                    data.getNegationMaxDepth() / conjuncts, // average negation depth

                    data.getNegationMaxDepth() / (data.getNegationCount() + epsilon), // avg depth/negation

                    // simple logical operators
                    logicOps / conjuncts, // & per conjunct
                    (data.getConjunctionsCount() - conjuncts + 1) / (logicOps + epsilon),
                    data.getDisjunctionsCount() / (logicOps + epsilon),
                    data.getImplicationsCount() / (logicOps + epsilon),
                    data.getEquivalencesCount() / (logicOps + epsilon),


                    // booleans
                    data.getBooleanLiteralsCount() / conjuncts,
                    data.getBooleanConversionCount() / conjuncts,

                    // quantifiers
                    quantifiers / conjuncts,

                    data.getExistentialQuantifiersCount() / (quantifiers + epsilon),
                    data.getUniversalQuantifiersCount() / (quantifiers + epsilon),
                    data.getQuantifierMaxDepthCount() / (quantifiers + epsilon),

                    // equality and inequality
                    data.getEqualityCount() / conjuncts,
                    data.getInequalityCount() / conjuncts,

                    // identifiers
                    data.getIdentifiersCount() / conjuncts,

                    // identifiers types over id count
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.INTEGER) / (ids + epsilon),
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.BOOL) / (ids + epsilon),
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.SET) / (ids + epsilon),

                    // identifiers normalised over id count
                    data.getIdentifierRelationsCount() / (ids + epsilon),

                    data.getIdentifierBoundedCount() / (ids + epsilon),
                    data.getIdentifierSemiBoundedCount() / (ids + epsilon),
                    data.getIdentifierUnboundedCount() / (ids + epsilon),
                    data.getIdentifierBoundedDomainCount() / (ids + epsilon),
                    data.getIdentifierSemiBoundedDomainCount() / (ids + epsilon),
                    data.getIdentifierUnboundedDomainCount() / (ids + epsilon),

                    // arithmetic
                    arithmeticOps / conjuncts,

                    // arithmetic normalised over arithmetic
                    data.getArithmeticAdditionCount() / (arithmeticOps + epsilon),
                    data.getArithmeticMultiplicationCount() / (arithmeticOps + epsilon),
                    data.getArithmeticDivisionCount() / (arithmeticOps + epsilon),
                    data.getArithmeticModuloCount() / (arithmeticOps + epsilon),
                    data.getSizeComparisonCount() / (arithmeticOps + epsilon),
                    data.getArithmeticGeneralisedSumCount() / (arithmeticOps + epsilon),
                    data.getArithmeticGeneralisedProductCount() / (arithmeticOps + epsilon),
                    data.getSuccCount() / (arithmeticOps + epsilon),
                    data.getPredecCount() / (arithmeticOps + epsilon),

                    // set theory
                    setBelongings / conjuncts,
                    setOperations / conjuncts,
                    data.getSetComprehensionCount() / conjuncts,

                    // set theory over sets
                    data.getMemberCount() / (setBelongings + epsilon),
                    data.getNotMemberCount() / (setBelongings + epsilon),
                    data.getSubsetCount() / (setBelongings + epsilon),
                    data.getNotSubsetCount() / (setBelongings + epsilon),

                    data.getSetUnionCount() / (setOperations + epsilon),
                    data.getSetIntersectCount() / (setOperations + epsilon),
                    data.getSetSubtractionCount() / (setOperations + epsilon),
                    data.getSetGeneralUnionCount() / (setOperations + epsilon),
                    data.getSetGeneralIntersectCount() / (setOperations + epsilon),
                    data.getSetQuantifiedUnionCount() / (setOperations + epsilon),
                    data.getSetQuantifiedIntersectCount() / (setOperations + epsilon),

                    // power sets
                    data.getPowerSetCount() / conjuncts,
                    data.getPowerSetHigherOrderCounts() / (data.getPowerSetCount() + epsilon),
                    data.getPowerSetCount() / (setOperations + epsilon),
                    data.getMaxPowDepth() / (data.getPowerSetCount() + epsilon),

                    // relations
                    relations / conjuncts,
                    relationOps / conjuncts,

                    // relations over relation theory
                    data.getRelationCount() / (relations + epsilon),
                    data.getRelationTotalCount() / (relations + epsilon),
                    data.getRelationSurjCount() / (relations + epsilon),
                    data.getRelationTotalSurjCount() / (relations + epsilon),

                    data.getRelationalImageCount() / (relationOps + epsilon),
                    data.getRelationInverseCount() / (relationOps + epsilon),
                    data.getRelationOverrideCount() / (relationOps + epsilon),
                    data.getRelationDirectProductCount() / (relationOps + epsilon),
                    data.getRelationParallelProductCount() / (relationOps + epsilon),
                    data.getDomainCount() / (relationOps + epsilon),
                    data.getRangeCount() / (relationOps + epsilon),
                    data.getProjection1Count() / (relationOps + epsilon),
                    data.getProjection2Count() / (relationOps + epsilon),
                    data.getForwardCompositionCount() / (relationOps + epsilon),
                    data.getDomainRestrictionCount() / (relationOps + epsilon),
                    data.getDomainSubtractionCount() / (relationOps + epsilon),
                    data.getRangeRestrictionCount() / (relationOps + epsilon),
                    data.getRangeSubtractionCount() / (relationOps + epsilon),

                    // functions
                    functions / conjuncts,
                    data.getFunctionApplicationCount() / conjuncts,

                    // functions over function theory
                    data.getFunPartialCount() / functions,
                    data.getFunTotalCount() / functions,
                    data.getFunPartialInjCount() / functions,
                    data.getFunTotalInjCount() / functions,
                    data.getFunPartialSurjCount() / functions,
                    data.getFunTotalSurjCount() / functions,
                    data.getFunPartialBijCount() / functions,
                    data.getFunTotalBijCount() / functions,
                    data.getLambdaCount() / functions,

                    // sequences
                    sequences / conjuncts,
                    sequenceOps / conjuncts,

                    // sequences over sequence theory
                    data.getSeqCount() / (sequences + epsilon),
                    data.getIseqCount() / (sequences + epsilon),

                    data.getSizeCount() / (sequenceOps + epsilon),
                    data.getFirstCount() / (sequenceOps + epsilon),
                    data.getTailCount() / (sequenceOps + epsilon),
                    data.getLastCount() / (sequenceOps + epsilon),
                    data.getFrontCount() / (sequenceOps + epsilon),
                    data.getRevCount() / (sequenceOps + epsilon),
                    data.getPermCount() / (sequenceOps + epsilon),
                    data.getConcatCount() / (sequenceOps + epsilon),
                    data.getFrontInsertionCount() / (sequenceOps + epsilon),
                    data.getTailInsertionCount() / (sequenceOps + epsilon),
                    data.getFrontRestrictionCount() / (sequenceOps + epsilon),
                    data.getTailRestrictionCount() / (sequenceOps + epsilon),
                    data.getGeneralConcatCount() / (sequenceOps + epsilon),

                    // closure and iterations
                    data.getClosureCount() / conjuncts,
                    data.getIterateCount() / conjuncts,
            };

            // features is a double[] type, so we have to box it to Double[].
            return Arrays.stream(features).boxed().toArray(Double[]::new);
        }
    }

    private static double log2(double num) {
        return Math.log(num) / Math.log(2);
    }

}
