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
public class BAst125Features extends PredicateFeatures {

    public static final int FEATURE_DIMENSION = 125;

    public BAst125Features(BPredicate predicate, Double[] features) {
        super(predicate, features);

        if (features.length != FEATURE_DIMENSION) {
            throw new IllegalArgumentException("Must have 124 features exactly, got " + features.length);
        }
    }

    public static class Generator implements PredicateFeatureGenerating<BAst125Features> {

        @Override
        public BAst125Features generate(BPredicate predicate, @Nullable MachineAccess machineAccess)
                throws FeatureCreationException {

            BAstFeatureData data = BAstFeatureCollector.collect(predicate, machineAccess);
            return new BAst125Features(predicate, generateArray(data));

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

            final double epsilon = Double.MIN_VALUE; // for division if something could be 0
            arithmeticOps = (arithmeticOps == 0.0) ? epsilon : arithmeticOps;
            quantifiers = (quantifiers == 0.0) ? epsilon : quantifiers;
            setBelongings = (setBelongings == 0.0) ? epsilon : setBelongings;
            setOperations = (setOperations == 0.0) ? epsilon : setOperations;
            relations = (relations == 0.0) ? epsilon : relations;
            relationOps = (relationOps == 0.0) ? epsilon : relationOps;
            functions = (functions == 0.0) ? epsilon : functions;
            sequences = (sequences == 0.0) ? epsilon : sequences;
            sequenceOps = (sequenceOps == 0.0) ? epsilon : sequenceOps;
            ids = (ids == 0.0) ? epsilon : ids;
            logicOps = (logicOps == 0.0) ? epsilon : logicOps;

            // setting up the data
            double[] features = {
                    // conjunct form
                    log2(conjuncts),
                    data.getMaxDepth() - 1 / conjuncts, // average conjunct depth
                    data.getNegationCount() / conjuncts, // average negations
                    data.getNegationMaxDepth() / conjuncts, // average negation depth

                    data.getNegationMaxDepth() / (data.getNegationCount() + epsilon), // avg depth/negation

                    // simple logical operators
                    logicOps / conjuncts, // & per conjunct
                    (data.getConjunctionsCount() - conjuncts + 1) / (logicOps ),
                    data.getDisjunctionsCount() / (logicOps ),
                    data.getImplicationsCount() / (logicOps ),
                    data.getEquivalencesCount() / (logicOps ),


                    // booleans
                    data.getBooleanLiteralsCount() / conjuncts,
                    data.getBooleanConversionCount() / conjuncts,

                    // quantifiers
                    quantifiers / conjuncts,

                    data.getExistentialQuantifiersCount() / (quantifiers ),
                    data.getUniversalQuantifiersCount() / (quantifiers ),
                    data.getQuantifierMaxDepthCount() / (quantifiers ),

                    // equality and inequality
                    data.getEqualityCount() / conjuncts,
                    data.getInequalityCount() / conjuncts,

                    // identifiers
                    data.getIdentifiersCount() / conjuncts,

                    // identifiers types over id count
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.INTEGER) / (ids ),
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.BOOL) / (ids ),
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.SET) / (ids ),
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.RELATION) / (ids ),
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.FUNCTION) / (ids ),

                    // identifiers normalised over id count
                    data.getIdentifierRelationsCount() / (ids ),

                    data.getIdentifierBoundedCount() / (ids ),
                    data.getIdentifierSemiBoundedCount() / (ids ),
                    data.getIdentifierUnboundedCount() / (ids ),
                    data.getIdentifierBoundedDomainCount() / (ids ),
                    data.getIdentifierSemiBoundedDomainCount() / (ids ),
                    data.getIdentifierUnboundedDomainCount() / (ids ),

                    // arithmetic
                    arithmeticOps / conjuncts,

                    // arithmetic normalised over arithmetic
                    data.getArithmeticAdditionCount() / (arithmeticOps ),
                    data.getArithmeticMultiplicationCount() / (arithmeticOps ),
                    data.getArithmeticDivisionCount() / (arithmeticOps ),
                    data.getArithmeticModuloCount() / (arithmeticOps ),
                    data.getSizeComparisonCount() / (arithmeticOps ),
                    data.getArithmeticGeneralisedSumCount() / (arithmeticOps ),
                    data.getArithmeticGeneralisedProductCount() / (arithmeticOps ),
                    data.getSuccCount() / (arithmeticOps ),
                    data.getPredecCount() / (arithmeticOps ),

                    // set theory
                    setBelongings / conjuncts,
                    setOperations / conjuncts,
                    data.getSetComprehensionCount() / conjuncts,
                    data.getSetCardCount() / conjuncts,

                    // set theory over sets
                    data.getMemberCount() / (setBelongings ),
                    data.getNotMemberCount() / (setBelongings ),
                    data.getSubsetCount() / (setBelongings ),
                    data.getNotSubsetCount() / (setBelongings ),

                    data.getSetUnionCount() / (setOperations ),
                    data.getSetIntersectCount() / (setOperations ),
                    data.getSetSubtractionCount() / (setOperations ),
                    data.getSetGeneralUnionCount() / (setOperations ),
                    data.getSetGeneralIntersectCount() / (setOperations ),
                    data.getSetQuantifiedUnionCount() / (setOperations ),
                    data.getSetQuantifiedIntersectCount() / (setOperations ),

                    // power sets
                    data.getPowerSetCount() / conjuncts,
                    data.getPowerSetHigherOrderCounts() / (data.getPowerSetCount() + epsilon),
                    data.getPowerSetCount() / (setOperations ),
                    data.getMaxPowDepth() / (data.getPowerSetCount() + epsilon),

                    // relations
                    relations / conjuncts,
                    relationOps / conjuncts,

                    // relations over relation theory
                    data.getRelationCount() / (relations ),
                    data.getRelationTotalCount() / (relations ),
                    data.getRelationSurjCount() / (relations ),
                    data.getRelationTotalSurjCount() / (relations ),

                    data.getRelationalImageCount() / (relationOps ),
                    data.getRelationInverseCount() / (relationOps ),
                    data.getRelationOverrideCount() / (relationOps ),
                    data.getRelationDirectProductCount() / (relationOps ),
                    data.getRelationParallelProductCount() / (relationOps ),
                    data.getDomainCount() / (relationOps ),
                    data.getRangeCount() / (relationOps ),
                    data.getProjection1Count() / (relationOps ),
                    data.getProjection2Count() / (relationOps ),
                    data.getForwardCompositionCount() / (relationOps ),
                    data.getDomainRestrictionCount() / (relationOps ),
                    data.getDomainSubtractionCount() / (relationOps ),
                    data.getRangeRestrictionCount() / (relationOps ),
                    data.getRangeSubtractionCount() / (relationOps ),

                    // functions
                    functions / conjuncts,
                    data.getFunctionApplicationCount() / conjuncts,

                    // functions over function theory
                    data.getFunPartialCount() / (functions ),
                    data.getFunTotalCount() / (functions ),
                    data.getFunPartialInjCount() / (functions ),
                    data.getFunTotalInjCount() / (functions ),
                    data.getFunPartialSurjCount() / (functions ),
                    data.getFunTotalSurjCount() / (functions ),
                    data.getFunPartialBijCount() / (functions ),
                    data.getFunTotalBijCount() / (functions ),
                    data.getLambdaCount() / (functions ),

                    // sequences
                    sequences / conjuncts,
                    sequenceOps / conjuncts,

                    // sequences over sequence theory
                    data.getSeqCount() / (sequences ),
                    data.getIseqCount() / (sequences ),

                    data.getSizeCount() / (sequenceOps ),
                    data.getFirstCount() / (sequenceOps ),
                    data.getTailCount() / (sequenceOps ),
                    data.getLastCount() / (sequenceOps ),
                    data.getFrontCount() / (sequenceOps ),
                    data.getRevCount() / (sequenceOps ),
                    data.getPermCount() / (sequenceOps ),
                    data.getConcatCount() / (sequenceOps ),
                    data.getFrontInsertionCount() / (sequenceOps ),
                    data.getTailInsertionCount() / (sequenceOps ),
                    data.getFrontRestrictionCount() / (sequenceOps ),
                    data.getTailRestrictionCount() / (sequenceOps ),
                    data.getGeneralConcatCount() / (sequenceOps ),

                    // closure and iterations
                    data.getClosureCount() / conjuncts,
                    data.getIterateCount() / conjuncts,

                    // Additional features to F110
                    data.getIdUses() / conjuncts,
                    data.getConjunctBasedDistinctIdUses() / conjuncts,
                    data.getIdUses() / (ids ),
                    data.getConjunctBasedDistinctIdUses() / (ids ),
                    data.getConjunctsWithoutIdUseCount() / conjuncts,

                    // Additional features to F115
                    data.identifierOfTypeCount(AdjacencyList.AdjacencyNodeTypes.UNKNOWN) / (ids ),
                    data.getIdentifierSelfRelationsCount()/ ids,
                    data.getEnumerableSubsetsCount()/ids,

                    data.getIntegerCount()/ids,
                    data.getIntCount()/ids,
                    data.getNaturalCount()/ids,
                    data.getNatCount()/ids,
                    data.getNatural1Count()/ids,
                    data.getNat1Count()/ids,

                    log2(data.getMaxIntegerUsed())
            };

            // features is a double[] type, so we have to box it to Double[].
            return Arrays.stream(features).boxed().toArray(Double[]::new);
        }
    }

    private static double log2(double num) {
        return Math.log(num) / Math.log(2);
    }

}
