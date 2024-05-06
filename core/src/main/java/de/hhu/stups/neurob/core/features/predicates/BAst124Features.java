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
 * Extended version of BAst115Features.
 * Containing entries for
 * <ul>
 *     <li>Ratio of unknown ID types</li>
 *     <li>Id ratio with self-relations</li>
 *     <li>enumerable sets per id</li>
 *     <li>Integer set count</li>
 *     <li>Int set count</li>
 *     <li>Natural set count</li>
 *     <li>Nat set count</li>
 *     <li>Natural1 set count</li>
 *     <li>Nat1 set count</li>
 *     <li>log2(max integer used)</li>
 * </ul>
 */
public class BAst124Features extends PredicateFeatures {

    public static final int FEATURE_DIMENSION = 124;

    public BAst124Features(BPredicate predicate, Double[] features) {
        super(predicate, features);

        if (features.length != FEATURE_DIMENSION) {
            throw new IllegalArgumentException("Must have 124 features exactly, got " + features.length);
        }
    }

    public static class Generator implements PredicateFeatureGenerating<BAst124Features> {

        @Override
        public BAst124Features generate(BPredicate predicate, @Nullable MachineAccess machineAccess)
                throws FeatureCreationException {

            BAstFeatureData data = BAstFeatureCollector.collect(predicate, machineAccess);
            return new BAst124Features(predicate, generateArray(data));

        }

        public static Double[] generateArray(BAstFeatureData data) {
            // get some constants
            double conjuncts = data.getConjunctsCount();
            double arithmeticOps =
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
            double quantifiers =
                    data.getExistentialQuantifiersCount()
                            + data.getUniversalQuantifiersCount();
            double setBelongings =
                    data.getMemberCount()
                            + data.getNotMemberCount()
                            + data.getNotSubsetCount()
                            + data.getSubsetCount();
            double setOperations =
                    data.getSetUnionCount() + data.getSetIntersectCount()
                            + data.getSetSubtractionCount()
                            + data.getSetGeneralIntersectCount()
                            + data.getSetGeneralUnionCount()
                            + data.getSetQuantifiedIntersectCount()
                            + data.getSetQuantifiedUnionCount();
            double relations =
                    data.getRelationCount()
                            + data.getRelationTotalCount()
                            + data.getRelationSurjCount()
                            + data.getRelationTotalSurjCount();
            double relationOps =
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
            double functions =
                    data.getFunPartialCount()
                            + data.getFunTotalCount()
                            + data.getFunPartialInjCount()
                            + data.getFunTotalInjCount()
                            + data.getFunPartialSurjCount()
                            + data.getFunTotalSurjCount()
                            + data.getFunPartialBijCount()
                            + data.getFunTotalBijCount()
                            + data.getLambdaCount();
            double sequences =
                    +data.getSeqCount()
                            + data.getIseqCount();
            double sequenceOps =
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
            Double[] features = {
                    // conjunct form
                    log2(conjuncts),
                    saveDiv(data.getMaxDepth() - 1, conjuncts), // average conjunct depth
                    saveDiv(data.getNegationCount(), conjuncts), // average negations
                    saveDiv(data.getNegationMaxDepth(), conjuncts), // average negation depth

                    saveDiv(data.getNegationMaxDepth(), (data.getNegationCount())), // avg depth/negation

                    // simple logical operators
                    saveDiv(logicOps , conjuncts), // & per conjunct
                    saveDiv((data.getConjunctionsCount() - conjuncts + 1), (logicOps)),
                    saveDiv(data.getDisjunctionsCount(), (logicOps)),
                    saveDiv(data.getImplicationsCount(), (logicOps)),
                    saveDiv(data.getEquivalencesCount(), (logicOps)),


                    // booleans
                    saveDiv(data.getBooleanLiteralsCount(), conjuncts),
                    saveDiv(data.getBooleanConversionCount(), conjuncts),

                    // quantifiers
                    saveDiv(quantifiers, conjuncts),

                    saveDiv(data.getExistentialQuantifiersCount(), (quantifiers)),
                    saveDiv(data.getUniversalQuantifiersCount(), (quantifiers)),
                    saveDiv(data.getQuantifierMaxDepthCount(), (quantifiers)),

                    // equality and inequality
                    saveDiv(data.getEqualityCount(), conjuncts),
                    saveDiv(data.getInequalityCount(), conjuncts),

                    // identifiers
                    saveDiv(data.getIdentifiersCount(), conjuncts),

                    // identifiers types over id count
                    saveDiv(data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.INTEGER), (ids)),
                    saveDiv(data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.BOOL), (ids)),
                    saveDiv(data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.SET), (ids)),
                    saveDiv(data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.RELATION), (ids)),
                    saveDiv(data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.FUNCTION), (ids)),

                    // identifiers normalised over id count
                    saveDiv(data.getIdentifierRelationsCount(), (ids)),

                    saveDiv(data.getIdentifierBoundedCount(), (ids)),
                    saveDiv(data.getIdentifierSemiBoundedCount(), (ids)),
                    saveDiv(data.getIdentifierUnboundedCount(), (ids)),
                    saveDiv(data.getIdentifierBoundedDomainCount(), (ids)),
                    saveDiv(data.getIdentifierSemiBoundedDomainCount(), (ids)),
                    saveDiv(data.getIdentifierUnboundedDomainCount(), (ids)),

                    // arithmetic
                    saveDiv(arithmeticOps, conjuncts),

                    // arithmetic normalised over arithmetic
                    saveDiv(data.getArithmeticAdditionCount(), (arithmeticOps)),
                    saveDiv(data.getArithmeticMultiplicationCount(), (arithmeticOps)),
                    saveDiv(data.getArithmeticDivisionCount(), (arithmeticOps)),
                    saveDiv(data.getArithmeticModuloCount(), (arithmeticOps)),
                    saveDiv(data.getSizeComparisonCount(), (arithmeticOps)),
                    saveDiv(data.getArithmeticGeneralisedSumCount(), (arithmeticOps)),
                    saveDiv(data.getArithmeticGeneralisedProductCount(), (arithmeticOps)),
                    saveDiv(data.getSuccCount(), (arithmeticOps)),
                    saveDiv(data.getPredecCount(), (arithmeticOps)),

                    // set theory
                    saveDiv(setBelongings, conjuncts),
                    saveDiv(setOperations, conjuncts),
                    saveDiv(data.getSetComprehensionCount(), conjuncts),
                    saveDiv(data.getSetCardCount(), conjuncts),

                    // set theory over sets
                    saveDiv(data.getMemberCount(), (setBelongings)),
                    saveDiv(data.getNotMemberCount(), (setBelongings)),
                    saveDiv(data.getSubsetCount(), (setBelongings)),
                    saveDiv(data.getNotSubsetCount(), (setBelongings)),

                    saveDiv(data.getSetUnionCount(), (setOperations)),
                    saveDiv(data.getSetIntersectCount(), (setOperations)),
                    saveDiv(data.getSetSubtractionCount(), (setOperations)),
                    saveDiv(data.getSetGeneralUnionCount(), (setOperations)),
                    saveDiv(data.getSetGeneralIntersectCount(), (setOperations)),
                    saveDiv(data.getSetQuantifiedUnionCount(), (setOperations)),
                    saveDiv(data.getSetQuantifiedIntersectCount(), (setOperations)),

                    // power sets
                    saveDiv(data.getPowerSetCount(), conjuncts),
                    saveDiv(data.getPowerSetHigherOrderCounts(), (data.getPowerSetCount())),
                    saveDiv(data.getPowerSetCount(), (setOperations)),
                    saveDiv(data.getMaxPowDepth(), (data.getPowerSetCount())),

                    // relations
                    saveDiv(relations, conjuncts),
                    saveDiv(relationOps, conjuncts),

                    // relations over relation theory
                    saveDiv(data.getRelationCount(), (relations)),
                    saveDiv(data.getRelationTotalCount(), (relations)),
                    saveDiv(data.getRelationSurjCount(), (relations)),
                    saveDiv(data.getRelationTotalSurjCount(), (relations)),

                    saveDiv(data.getRelationalImageCount(), (relationOps)),
                    saveDiv(data.getRelationInverseCount(), (relationOps)),
                    saveDiv(data.getRelationOverrideCount(), (relationOps)),
                    saveDiv(data.getRelationDirectProductCount(), (relationOps)),
                    saveDiv(data.getRelationParallelProductCount(), (relationOps)),
                    saveDiv(data.getDomainCount(), (relationOps)),
                    saveDiv(data.getRangeCount(), (relationOps)),
                    saveDiv(data.getProjection1Count(), (relationOps)),
                    saveDiv(data.getProjection2Count(), (relationOps)),
                    saveDiv(data.getForwardCompositionCount(), (relationOps)),
                    saveDiv(data.getDomainRestrictionCount(), (relationOps)),
                    saveDiv(data.getDomainSubtractionCount(), (relationOps)),
                    saveDiv(data.getRangeRestrictionCount(), (relationOps)),
                    saveDiv(data.getRangeSubtractionCount(), (relationOps)),

                    // functions
                    saveDiv(functions, conjuncts),
                    saveDiv(data.getFunctionApplicationCount(), conjuncts),

                    // functions over function theory
                    saveDiv(data.getFunPartialCount(), (functions)),
                    saveDiv(data.getFunTotalCount(), (functions)),
                    saveDiv(data.getFunPartialInjCount(), (functions)),
                    saveDiv(data.getFunTotalInjCount(), (functions)),
                    saveDiv(data.getFunPartialSurjCount(), (functions)),
                    saveDiv(data.getFunTotalSurjCount(), (functions)),
                    saveDiv(data.getFunPartialBijCount(), (functions)),
                    saveDiv(data.getFunTotalBijCount(), (functions)),
                    saveDiv(data.getLambdaCount(), (functions)),

                    // sequences
                    saveDiv(sequences, conjuncts),
                    saveDiv(sequenceOps, conjuncts),

                    // sequences over sequence theory
                    saveDiv(data.getSeqCount(), (sequences)),
                    saveDiv(data.getIseqCount(), (sequences)),

                    saveDiv(data.getSizeCount(), (sequenceOps)),
                    saveDiv(data.getFirstCount(), (sequenceOps)),
                    saveDiv(data.getTailCount(), (sequenceOps)),
                    saveDiv(data.getLastCount(), (sequenceOps)),
                    saveDiv(data.getFrontCount(), (sequenceOps)),
                    saveDiv(data.getRevCount(), (sequenceOps)),
                    saveDiv(data.getPermCount(), (sequenceOps)),
                    saveDiv(data.getConcatCount(), (sequenceOps)),
                    saveDiv(data.getFrontInsertionCount(), (sequenceOps)),
                    saveDiv(data.getTailInsertionCount(), (sequenceOps)),
                    saveDiv(data.getFrontRestrictionCount(), (sequenceOps)),
                    saveDiv(data.getTailRestrictionCount(), (sequenceOps)),
                    saveDiv(data.getGeneralConcatCount(), (sequenceOps)),

                    // closure and iterations
                    saveDiv(data.getClosureCount(), conjuncts),
                    saveDiv(data.getIterateCount(), conjuncts),

                    // Additional features to F110
                    saveDiv(data.getIdUses(), conjuncts),
                    saveDiv(data.getConjunctBasedDistinctIdUses(), conjuncts),
                    saveDiv(data.getIdUses(), (ids)),
                    saveDiv(data.getConjunctBasedDistinctIdUses(), (ids)),
                    saveDiv(data.getConjunctsWithoutIdUseCount(), conjuncts),

                    // Additional features to F115
                    saveDiv(data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.UNKNOWN), (ids)),
                    saveDiv(data.getIdentifierSelfRelationsCount(), ids),
                    saveDiv((ids < 1) ? 0 : data.getEnumerableSubsetsCount(), ids),

                    saveDiv(data.getIntegerCount(), ids),
                    saveDiv(data.getIntCount(), ids),
                    saveDiv(data.getNaturalCount(), ids),
                    saveDiv(data.getNatCount(), ids),
                    saveDiv(data.getNatural1Count(), ids),
                    saveDiv(data.getNat1Count(), ids),
            };

            return features;
        }
    }

    private static Double log2(double num) {
        return Math.log(num) / Math.log(2);
    }

    private static Double saveDiv(double dividend, double divisor) {
        double epsilon = 1;  // All denominators we use should be integers.
        if (divisor < epsilon) {
            return 0.0;
        }
        return dividend / divisor;
    }

}
