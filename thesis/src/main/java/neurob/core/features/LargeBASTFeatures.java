package neurob.core.features;

import de.be4.classicalb.core.parser.BParser;
import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.PredicateASTFeatures;
import neurob.core.features.util.IdentifierRelationsHandler;
import neurob.core.features.util.LargeBASTFeatureData;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.PredicateTrainingCSVGenerator;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.interfaces.LabelGenerator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Jannik Dunkelau
 */
public class LargeBASTFeatures implements PredicateASTFeatures {
	private Path sourceFile;
	private BParser bParser;
	private MachineType mtype = MachineType.CLASSICALB; // type of machine currently working on
	private StateSpace ss;

	public static final int featureDimension = 185;

	public LargeBASTFeatures(){
		sourceFile = null;
		bParser = new BParser();
	}

	@Override
	public void setStateSpace(StateSpace ss) {
		this.ss = ss;
	}

	@Override
	public void setMachine(Path machineFile) throws NeuroBException {
		String fileName = machineFile.getFileName().toString();
		if(fileName.endsWith(".mch")) {
			mtype = MachineType.CLASSICALB;
			try {
				bParser = new BParser(machineFile.toString());
				bParser.parseFile(machineFile.toFile(), false);
			} catch (IOException e) {
				throw new NeuroBException("Could not access source file "+machineFile, e);
			} catch (Exception e) {
				throw new NeuroBException("Could not parse source file "+machineFile, e);
			}
		} else if(fileName.endsWith(".bcm")){
			bParser = new BParser();
			mtype = MachineType.EVENTB;
		}
		sourceFile = machineFile;
	}

	@Override
	public INDArray generateFeatureNDArray(String source) throws NeuroBException {
		return Nd4j.create(generateFeatureArray(source));
	}

	@Override
	public double[] generateFeatureArray(String source) throws NeuroBException {
		return calcFeatures(source);
	}

	@Override
	public int getFeatureDimension() {
		return featureDimension;
	}

	@Override
	public Path getSourceFile() {
		return sourceFile;
	}

	@Override
	public TrainingDataGenerator getTrainingDataGenerator(LabelGenerator lg) {
		return new PredicateTrainingCSVGenerator(this, lg);
	}


	private double[] calcFeatures(String source) throws NeuroBException {
		LargeBASTFeatureData data = new LargeBASTFeatureData(source, bParser, mtype, ss);

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
				data.getSetUnionCount()+data.getSetIntersectCount()
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
				+ data.getSeqCount()
				+ data.getIseqCount();
		final double sequenceOps =
				+ data.getSizeCount()
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

		// identifiers
		double ids = data.getIdentifiersCount();


		// setting up the data
		double[] features = {
				// conjunct form
				data.getMaxDepth()/conjuncts, // average conjunct depth
				data.getNegationCount()/conjuncts, // average negations
				data.getNegationMaxDepth()/conjuncts, // average negation depth

				data.getNegationMaxDepth()/(data.getNegationCount()+epsilon), // avg depth/negation

				// simple logical operators
				(data.getConjunctionsCount()-conjuncts+1)/conjuncts, // & per conjunct
				data.getDisjunctionsCount()/conjuncts, // `or` per conjunct
				data.getImplicationsCount()/conjuncts, // => per conjunct
				data.getEquivalencesCount()/conjuncts, // <=>

				((data.getConjunctionsCount()-conjuncts+1)
					+data.getDisjunctionsCount()
					+data.getImplicationsCount()
					+data.getEquivalencesCount()) / conjuncts,

				// booleans
				data.getBooleanLiteralsCount()/conjuncts,
				data.getBooleanConversionCount()/conjuncts,

				// quantifiers
				data.getExistentialQuantifiersCount()/conjuncts, // exist. quant. per conjunct
				data.getUniversalQuantifiersCount()/conjuncts, // univ. quant. per conjunct
				data.getQuantifierMaxDepthCount()/conjuncts,

				quantifiers/conjuncts,

				data.getExistentialQuantifiersCount()/(quantifiers+epsilon),
				data.getUniversalQuantifiersCount()/(quantifiers+epsilon),
				data.getQuantifierMaxDepthCount()/(quantifiers+epsilon),

				// equality and inequality
				data.getEqualityCount()/conjuncts,
				data.getInequalityCount()/conjuncts,

				// identifiers over conjuncts
				data.getIdentifiersCount()/conjuncts,
				data.getIdentifierRelationsCount()/conjuncts,

				data.getIdentifierBoundedCount()/conjuncts,
				data.getIdentifierSemiBoundedCount()/conjuncts,
				data.getIdentifierUnboundedCount()/conjuncts,
				data.getIdentifierBoundedDomainCount()/conjuncts,
				data.getIdentifierSemiBoundedDomainCount()/conjuncts,
				data.getIdentifierUnboundedDomainCount()/conjuncts,

				// identifiers normalised over id count
				data.getIdentifierRelationsCount()/ids,

				data.getIdentifierBoundedCount()/ids,
				data.getIdentifierSemiBoundedCount()/ids,
				data.getIdentifierUnboundedCount()/ids,
				data.getIdentifierBoundedDomainCount()/ids,
				data.getIdentifierSemiBoundedDomainCount()/ids,
				data.getIdentifierUnboundedDomainCount()/ids,

				// arithmetic normalised over conjunct
				data.getArithmeticAdditionCount()/conjuncts,
				data.getArithmeticMultiplicationCount()/conjuncts,
				data.getArithmeticDivisionCount()/conjuncts,
				data.getArithmeticModuloCount()/conjuncts,
				data.getSizeComparisonCount()/conjuncts,
				data.getArithmeticGeneralisedSumCount()/conjuncts,
				data.getArithmeticGeneralisedProductCount()/conjuncts,
				data.getSuccCount()/conjuncts,
				data.getPredecCount()/conjuncts,

				arithmeticOps/conjuncts,

				// arithmetic normalised over arithmetic
				data.getArithmeticAdditionCount()/arithmeticOps,
				data.getArithmeticMultiplicationCount()/arithmeticOps,
				data.getArithmeticDivisionCount()/arithmeticOps,
				data.getArithmeticModuloCount()/arithmeticOps,
				data.getSizeComparisonCount()/arithmeticOps,
				data.getArithmeticGeneralisedSumCount()/arithmeticOps,
				data.getArithmeticGeneralisedProductCount()/arithmeticOps,
				data.getSuccCount()/arithmeticOps,
				data.getPredecCount()/arithmeticOps,

				// set theory over conjuncts
				data.getMemberCount()/conjuncts,
				data.getNotMemberCount()/conjuncts,
				data.getSubsetCount()/conjuncts,
				data.getNotSubsetCount()/conjuncts,

				data.getFiniteSetRequirementsCount()/conjuncts,
				data.getInfiniteSetRequirementsCount()/conjuncts,
				data.getSetCardCount()/conjuncts,

				data.getSetUnionCount()/conjuncts,
				data.getSetIntersectCount()/conjuncts,
				data.getSetSubtractionCount()/conjuncts,
				data.getSetGeneralUnionCount()/conjuncts,
				data.getSetGeneralIntersectCount()/conjuncts,
				data.getSetQuantifiedUnionCount()/conjuncts,
				data.getSetQuantifiedIntersectCount()/conjuncts,

				data.getSetComprehensionCount()/conjuncts,

				setBelongings/conjuncts,
				setOperations/conjuncts,

				// set theory over sets
				data.getMemberCount()/(setBelongings+epsilon),
				data.getNotMemberCount()/(setBelongings+epsilon),
				data.getSubsetCount()/(setBelongings+epsilon),
				data.getNotSubsetCount()/(setBelongings+epsilon),

				data.getSetUnionCount()/(setOperations+epsilon),
				data.getSetIntersectCount()/(setOperations+epsilon),
				data.getSetSubtractionCount()/(setOperations+epsilon),
				data.getSetGeneralUnionCount()/(setOperations+epsilon),
				data.getSetGeneralIntersectCount()/(setOperations+epsilon),
				data.getSetQuantifiedUnionCount()/(setOperations+epsilon),
				data.getSetQuantifiedIntersectCount()/(setOperations+epsilon),

				data.getSetComprehensionCount()/((setBelongings+epsilon)+data.getSetComprehensionCount()),
				data.getSetComprehensionCount()/((setOperations+epsilon)+data.getSetComprehensionCount()),

				// power sets
				data.getPowerSetCount()/conjuncts,
				data.getPowerSetHigherOrderCounts()/conjuncts,

				data.getPowerSetHigherOrderCounts()/(data.getPowerSetCount()+epsilon),

				data.getPowerSetCount()/((setOperations+epsilon)+data.getPowerSetCount()),
				data.getPowerSetHigherOrderCounts()/((setOperations+epsilon)+data.getPowerSetHigherOrderCounts()),

				data.getMaxPowDepth()/(data.getPowerSetCount()+epsilon),
				data.getMaxPowDepth()/(data.getPowerSetHigherOrderCounts()+epsilon),

				// relations over conjuncts
				data.getRelationCount()/conjuncts,
				data.getRelationTotalCount()/conjuncts,
				data.getRelationSurjCount()/conjuncts,
				data.getRelationTotalSurjCount()/conjuncts,

				data.getRelationalImageCount()/conjuncts,
				data.getRelationInverseCount()/conjuncts,
				data.getRelationOverrideCount()/conjuncts,
				data.getRelationDirectProductCount()/conjuncts,
				data.getRelationParallelProductCount()/conjuncts,
				data.getDomainCount()/conjuncts,
				data.getRangeCount()/conjuncts,
				data.getProjection1Count()/conjuncts,
				data.getProjection2Count()/conjuncts,
				data.getForwardCompositionCount()/conjuncts,
				data.getDomainRestrictionCount()/conjuncts,
				data.getDomainSubtractionCount()/conjuncts,
				data.getRangeRestrictionCount()/conjuncts,
				data.getRangeSubtractionCount()/conjuncts,

				relations/conjuncts,
				relationOps/conjuncts,

				// relations over relation theory
				data.getRelationCount()/(relations+epsilon),
				data.getRelationTotalCount()/(relations+epsilon),
				data.getRelationSurjCount()/(relations+epsilon),
				data.getRelationTotalSurjCount()/(relations+epsilon),

				data.getRelationalImageCount()/(relationOps+epsilon),
				data.getRelationInverseCount()/(relationOps+epsilon),
				data.getRelationOverrideCount()/(relationOps+epsilon),
				data.getRelationDirectProductCount()/(relationOps+epsilon),
				data.getRelationParallelProductCount()/(relationOps+epsilon),
				data.getDomainCount()/(relationOps+epsilon),
				data.getRangeCount()/(relationOps+epsilon),
				data.getProjection1Count()/(relationOps+epsilon),
				data.getProjection2Count()/(relationOps+epsilon),
				data.getForwardCompositionCount()/(relationOps+epsilon),
				data.getDomainRestrictionCount()/(relationOps+epsilon),
				data.getDomainSubtractionCount()/(relationOps+epsilon),
				data.getRangeRestrictionCount()/(relationOps+epsilon),
				data.getRangeSubtractionCount()/(relationOps+epsilon),

				// functions over conjuncts
				data.getFunPartialCount()/conjuncts,
				data.getFunTotalCount()/conjuncts,
				data.getFunPartialInjCount()/conjuncts,
				data.getFunTotalInjCount()/conjuncts,
				data.getFunPartialSurjCount()/conjuncts,
				data.getFunTotalSurjCount()/conjuncts,
				data.getFunPartialBijCount()/conjuncts,
				data.getFunTotalBijCount()/conjuncts,
				data.getLambdaCount()/conjuncts,

				data.getFunctionApplicationCount()/conjuncts,

				functions/conjuncts,
				(functions+data.getFunctionApplicationCount())/conjuncts,

				// functions over function theory
				data.getFunPartialCount()/(functions+epsilon),
				data.getFunTotalCount()/(functions+epsilon),
				data.getFunPartialInjCount()/(functions+epsilon),
				data.getFunTotalInjCount()/(functions+epsilon),
				data.getFunPartialSurjCount()/(functions+epsilon),
				data.getFunTotalSurjCount()/(functions+epsilon),
				data.getFunPartialBijCount()/(functions+epsilon),
				data.getFunTotalBijCount()/(functions+epsilon),
				data.getLambdaCount()/(functions+epsilon),

				data.getFunctionApplicationCount()/((functions+epsilon)+data.getFunctionApplicationCount()),

				// sequences over conjuncts
				data.getSeqCount()/conjuncts,
				data.getIseqCount()/conjuncts,
				data.getSizeCount()/conjuncts,
				data.getFirstCount()/conjuncts,
				data.getTailCount()/conjuncts,
				data.getLastCount()/conjuncts,
				data.getFrontCount()/conjuncts,
				data.getRevCount()/conjuncts,
				data.getPermCount()/conjuncts,
				data.getConcatCount()/conjuncts,
				data.getFrontInsertionCount()/conjuncts,
				data.getTailInsertionCount()/conjuncts,
				data.getFrontRestrictionCount()/conjuncts,
				data.getTailRestrictionCount()/conjuncts,
				data.getGeneralConcatCount()/conjuncts,

				sequences/conjuncts,
				sequenceOps/conjuncts,

				// sequences over sequence theory
				data.getSeqCount()/(sequences+epsilon),
				data.getIseqCount()/(sequences+epsilon),

				data.getSizeCount()/(sequenceOps+epsilon),
				data.getFirstCount()/(sequenceOps+epsilon),
				data.getTailCount()/(sequenceOps+epsilon),
				data.getLastCount()/(sequenceOps+epsilon),
				data.getFrontCount()/(sequenceOps+epsilon),
				data.getRevCount()/(sequenceOps+epsilon),
				data.getPermCount()/(sequenceOps+epsilon),
				data.getConcatCount()/(sequenceOps+epsilon),
				data.getFrontInsertionCount()/(sequenceOps+epsilon),
				data.getTailInsertionCount()/(sequenceOps+epsilon),
				data.getFrontRestrictionCount()/(sequenceOps+epsilon),
				data.getTailRestrictionCount()/(sequenceOps+epsilon),
				data.getGeneralConcatCount()/(sequenceOps+epsilon),

				// closure and iterations
				data.getClosureCount()/conjuncts,
				data.getIterateCount()/conjuncts,


		};

		return features;
	}
}
