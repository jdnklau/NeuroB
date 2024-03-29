package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.predicates.util.BAstFeatureCollector;
import de.hhu.stups.neurob.core.features.predicates.util.BAstFeatureData;

import javax.annotation.Nullable;
import java.util.Arrays;

public class BAst185Features extends PredicateFeatures {

    public static final int FEATURE_DIMENSION = 185;

    public BAst185Features(BPredicate predicate, Double[] features) {
        super(predicate, features);

        if (features.length != FEATURE_DIMENSION) {
            throw new IllegalArgumentException("Must have 275 features exactly.");
        }
    }

    public static class Generator implements PredicateFeatureGenerating<BAst185Features> {

        @Override
        public BAst185Features generate(BPredicate predicate, @Nullable MachineAccess machineAccess)
                throws FeatureCreationException {

            BAstFeatureData data = BAstFeatureCollector.collect(predicate, machineAccess);
            return new BAst185Features(predicate, generateArray(data));

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
                    + data.getSizeComparisonCount()
                    + epsilon;
            final double quantifiers =
                    data.getExistentialQuantifiersCount()
                    + data.getUniversalQuantifiersCount()
                    + epsilon;
            final double setBelongings =
                    data.getMemberCount()
                    + data.getNotMemberCount()
                    + data.getNotSubsetCount()
                    + data.getSubsetCount()
                    + epsilon;
            final double setOperations =
                    data.getSetUnionCount() + data.getSetIntersectCount()
                    + data.getSetSubtractionCount()
                    + data.getSetGeneralIntersectCount()
                    + data.getSetGeneralUnionCount()
                    + data.getSetQuantifiedIntersectCount()
                    + data.getSetQuantifiedUnionCount()
                    + epsilon;
            final double relations =
                    data.getRelationCount()
                    + data.getRelationTotalCount()
                    + data.getRelationSurjCount()
                    + data.getRelationTotalSurjCount()
                    + epsilon;
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
                    + data.getRangeSubtractionCount()
                    + epsilon;
            final double functions =
                    data.getFunPartialCount()
                    + data.getFunTotalCount()
                    + data.getFunPartialInjCount()
                    + data.getFunTotalInjCount()
                    + data.getFunPartialSurjCount()
                    + data.getFunTotalSurjCount()
                    + data.getFunPartialBijCount()
                    + data.getFunTotalBijCount()
                    + data.getLambdaCount()
                    + epsilon;
            final double sequences =
                    +data.getSeqCount()
                    + data.getIseqCount()
                    + epsilon;
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
                    + data.getGeneralConcatCount()
                    + epsilon;

            // identifiers
            double ids = data.getIdentifiersCount() + epsilon;


            // setting up the data
            double[] features = {
                    // conjunct form
                    data.getMaxDepth() / conjuncts, // average conjunct depth
                    data.getNegationCount() / conjuncts, // average negations
                    data.getNegationMaxDepth() / conjuncts, // average negation depth

                    data.getNegationMaxDepth() / (data.getNegationCount() + epsilon), // avg depth/negation

                    // simple logical operators
                    (data.getConjunctionsCount() - conjuncts + 1) / conjuncts, // & per conjunct
                    data.getDisjunctionsCount() / conjuncts, // `or` per conjunct
                    data.getImplicationsCount() / conjuncts, // => per conjunct
                    data.getEquivalencesCount() / conjuncts, // <=>

                    ((data.getConjunctionsCount() - conjuncts + 1)
                     + data.getDisjunctionsCount()
                     + data.getImplicationsCount()
                     + data.getEquivalencesCount()) / conjuncts,

                    // booleans
                    data.getBooleanLiteralsCount() / conjuncts,
                    data.getBooleanConversionCount() / conjuncts,

                    // quantifiers
                    data.getExistentialQuantifiersCount() / conjuncts, // exist. quant. per conjunct
                    data.getUniversalQuantifiersCount() / conjuncts, // univ. quant. per conjunct
                    data.getQuantifierMaxDepthCount() / conjuncts,

                    quantifiers / conjuncts,

                    data.getExistentialQuantifiersCount() / quantifiers,
                    data.getUniversalQuantifiersCount() / quantifiers,
                    data.getQuantifierMaxDepthCount() / quantifiers,

                    // equality and inequality
                    data.getEqualityCount() / conjuncts,
                    data.getInequalityCount() / conjuncts,

                    // identifiers over conjuncts
                    data.getIdentifiersCount() / conjuncts,
                    data.getIdentifierRelationsCount() / conjuncts,

                    data.getIdentifierBoundedCount() / conjuncts,
                    data.getIdentifierSemiBoundedCount() / conjuncts,
                    data.getIdentifierUnboundedCount() / conjuncts,
                    data.getIdentifierBoundedDomainCount() / conjuncts,
                    data.getIdentifierSemiBoundedDomainCount() / conjuncts,
                    data.getIdentifierUnboundedDomainCount() / conjuncts,

                    // identifiers normalised over id count
                    data.getIdentifierRelationsCount() / ids,

                    data.getIdentifierBoundedCount() / ids,
                    data.getIdentifierSemiBoundedCount() / ids,
                    data.getIdentifierUnboundedCount() / ids,
                    data.getIdentifierBoundedDomainCount() / ids,
                    data.getIdentifierSemiBoundedDomainCount() / ids,
                    data.getIdentifierUnboundedDomainCount() / ids,

                    // arithmetic normalised over conjunct
                    data.getArithmeticAdditionCount() / conjuncts,
                    data.getArithmeticMultiplicationCount() / conjuncts,
                    data.getArithmeticDivisionCount() / conjuncts,
                    data.getArithmeticModuloCount() / conjuncts,
                    data.getSizeComparisonCount() / conjuncts,
                    data.getArithmeticGeneralisedSumCount() / conjuncts,
                    data.getArithmeticGeneralisedProductCount() / conjuncts,
                    data.getSuccCount() / conjuncts,
                    data.getPredecCount() / conjuncts,

                    arithmeticOps / conjuncts,

                    // arithmetic normalised over arithmetic
                    data.getArithmeticAdditionCount() / arithmeticOps,
                    data.getArithmeticMultiplicationCount() / arithmeticOps,
                    data.getArithmeticDivisionCount() / arithmeticOps,
                    data.getArithmeticModuloCount() / arithmeticOps,
                    data.getSizeComparisonCount() / arithmeticOps,
                    data.getArithmeticGeneralisedSumCount() / arithmeticOps,
                    data.getArithmeticGeneralisedProductCount() / arithmeticOps,
                    data.getSuccCount() / arithmeticOps,
                    data.getPredecCount() / arithmeticOps,

                    // set theory over conjuncts
                    data.getMemberCount() / conjuncts,
                    data.getNotMemberCount() / conjuncts,
                    data.getSubsetCount() / conjuncts,
                    data.getNotSubsetCount() / conjuncts,

                    data.getFiniteSetRequirementsCount() / conjuncts,
                    data.getInfiniteSetRequirementsCount() / conjuncts,
                    data.getSetCardCount() / conjuncts,

                    data.getSetUnionCount() / conjuncts,
                    data.getSetIntersectCount() / conjuncts,
                    data.getSetSubtractionCount() / conjuncts,
                    data.getSetGeneralUnionCount() / conjuncts,
                    data.getSetGeneralIntersectCount() / conjuncts,
                    data.getSetQuantifiedUnionCount() / conjuncts,
                    data.getSetQuantifiedIntersectCount() / conjuncts,

                    data.getSetComprehensionCount() / conjuncts,

                    setBelongings / conjuncts,
                    setOperations / conjuncts,

                    // set theory over sets
                    data.getMemberCount() / setBelongings,
                    data.getNotMemberCount() / setBelongings,
                    data.getSubsetCount() / setBelongings,
                    data.getNotSubsetCount() / setBelongings,

                    data.getSetUnionCount() / setOperations,
                    data.getSetIntersectCount() / setOperations,
                    data.getSetSubtractionCount() / setOperations,
                    data.getSetGeneralUnionCount() / setOperations,
                    data.getSetGeneralIntersectCount() / setOperations,
                    data.getSetQuantifiedUnionCount() / setOperations,
                    data.getSetQuantifiedIntersectCount() / setOperations,

                    data.getSetComprehensionCount() / (setBelongings + data.getSetComprehensionCount()),
                    data.getSetComprehensionCount() / (setOperations + data.getSetComprehensionCount()),

                    // power sets
                    data.getPowerSetCount() / conjuncts,
                    data.getPowerSetHigherOrderCounts() / conjuncts,

                    data.getPowerSetHigherOrderCounts() / (data.getPowerSetCount() + epsilon),

                    data.getPowerSetCount() / (setOperations + data.getPowerSetCount()),
                    data.getPowerSetHigherOrderCounts() / (setOperations + data.getPowerSetHigherOrderCounts()),

                    data.getMaxPowDepth() / (data.getPowerSetCount() + epsilon),
                    data.getMaxPowDepth() / (data.getPowerSetHigherOrderCounts() + epsilon),

                    // relations over conjuncts
                    data.getRelationCount() / conjuncts,
                    data.getRelationTotalCount() / conjuncts,
                    data.getRelationSurjCount() / conjuncts,
                    data.getRelationTotalSurjCount() / conjuncts,

                    data.getRelationalImageCount() / conjuncts,
                    data.getRelationInverseCount() / conjuncts,
                    data.getRelationOverrideCount() / conjuncts,
                    data.getRelationDirectProductCount() / conjuncts,
                    data.getRelationParallelProductCount() / conjuncts,
                    data.getDomainCount() / conjuncts,
                    data.getRangeCount() / conjuncts,
                    data.getProjection1Count() / conjuncts,
                    data.getProjection2Count() / conjuncts,
                    data.getForwardCompositionCount() / conjuncts,
                    data.getDomainRestrictionCount() / conjuncts,
                    data.getDomainSubtractionCount() / conjuncts,
                    data.getRangeRestrictionCount() / conjuncts,
                    data.getRangeSubtractionCount() / conjuncts,

                    relations / conjuncts,
                    relationOps / conjuncts,

                    // relations over relation theory
                    data.getRelationCount() / relations,
                    data.getRelationTotalCount() / relations,
                    data.getRelationSurjCount() / relations,
                    data.getRelationTotalSurjCount() / relations,

                    data.getRelationalImageCount() / relationOps,
                    data.getRelationInverseCount() / relationOps,
                    data.getRelationOverrideCount() / relationOps,
                    data.getRelationDirectProductCount() / relationOps,
                    data.getRelationParallelProductCount() / relationOps,
                    data.getDomainCount() / relationOps,
                    data.getRangeCount() / relationOps,
                    data.getProjection1Count() / relationOps,
                    data.getProjection2Count() / relationOps,
                    data.getForwardCompositionCount() / relationOps,
                    data.getDomainRestrictionCount() / relationOps,
                    data.getDomainSubtractionCount() / relationOps,
                    data.getRangeRestrictionCount() / relationOps,
                    data.getRangeSubtractionCount() / relationOps,

                    // functions over conjuncts
                    data.getFunPartialCount() / conjuncts,
                    data.getFunTotalCount() / conjuncts,
                    data.getFunPartialInjCount() / conjuncts,
                    data.getFunTotalInjCount() / conjuncts,
                    data.getFunPartialSurjCount() / conjuncts,
                    data.getFunTotalSurjCount() / conjuncts,
                    data.getFunPartialBijCount() / conjuncts,
                    data.getFunTotalBijCount() / conjuncts,
                    data.getLambdaCount() / conjuncts,

                    data.getFunctionApplicationCount() / conjuncts,

                    functions / conjuncts,
                    (functions + data.getFunctionApplicationCount()) / conjuncts,

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

                    data.getFunctionApplicationCount() / (functions + data.getFunctionApplicationCount()),

                    // sequences over conjuncts
                    data.getSeqCount() / conjuncts,
                    data.getIseqCount() / conjuncts,
                    data.getSizeCount() / conjuncts,
                    data.getFirstCount() / conjuncts,
                    data.getTailCount() / conjuncts,
                    data.getLastCount() / conjuncts,
                    data.getFrontCount() / conjuncts,
                    data.getRevCount() / conjuncts,
                    data.getPermCount() / conjuncts,
                    data.getConcatCount() / conjuncts,
                    data.getFrontInsertionCount() / conjuncts,
                    data.getTailInsertionCount() / conjuncts,
                    data.getFrontRestrictionCount() / conjuncts,
                    data.getTailRestrictionCount() / conjuncts,
                    data.getGeneralConcatCount() / conjuncts,

                    sequences / conjuncts,
                    sequenceOps / conjuncts,

                    // sequences over sequence theory
                    data.getSeqCount() / sequences,
                    data.getIseqCount() / sequences,

                    data.getSizeCount() / sequenceOps,
                    data.getFirstCount() / sequenceOps,
                    data.getTailCount() / sequenceOps,
                    data.getLastCount() / sequenceOps,
                    data.getFrontCount() / sequenceOps,
                    data.getRevCount() / sequenceOps,
                    data.getPermCount() / sequenceOps,
                    data.getConcatCount() / sequenceOps,
                    data.getFrontInsertionCount() / sequenceOps,
                    data.getTailInsertionCount() / sequenceOps,
                    data.getFrontRestrictionCount() / sequenceOps,
                    data.getTailRestrictionCount() / sequenceOps,
                    data.getGeneralConcatCount() / sequenceOps,

                    // closure and iterations
                    data.getClosureCount() / conjuncts,
                    data.getIterateCount() / conjuncts,


            };

            // features is a double[] type, so we have to box it to Double[].
            return Arrays.stream(features).boxed().toArray(Double[]::new);
        }
    }

}
