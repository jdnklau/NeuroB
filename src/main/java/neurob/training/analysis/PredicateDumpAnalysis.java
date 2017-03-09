package neurob.training.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredicateDumpAnalysis implements TrainingAnalysisData {
	private int samplesSeen;
	private int filesSeen;
	private int emptyFilesSeen;
	
	// data over solvers
	private double[] sampleSums;
	private long[] negSamples;
	private List<List<Double>> posSamples;
	private final int solversAccountedFor = 3;
	
	// logger
	private static final Logger log = LoggerFactory.getLogger(PredicateDumpAnalysis.class);
	
	public PredicateDumpAnalysis() {
		samplesSeen=0;
		filesSeen = 0;
		emptyFilesSeen = 0;
		
		// Note: using 4 values here, but generating only data for first three solvers
		sampleSums = new double[]{0.,0.,0.,0.};
		negSamples = new long[]{0L,0L,0L,0L};
		posSamples = new ArrayList<>(4);
		for(int i=0; i<4; i++){
			posSamples.add(new ArrayList<>());
		}
	}

	@Override
	public void log() {
		log.info("Analysis of training data");
		
		if(filesSeen > 0){
			int relevantFiles = filesSeen-emptyFilesSeen;
			log.info("Files found: {}", filesSeen);
			log.info("Of these were {} seemingly empty", emptyFilesSeen);
			log.info("\t=> {} relevant files", relevantFiles);
		}
		
		// log boxplot values
		String[] solverNames = new String[]{"ProB", "KodKod", "Z3", "ProB+Z3"};
		for(int i=0; i<solversAccountedFor; i++){
			List<Double> samps = posSamples.get(i);
			Collections.sort(samps);
			int posSamplesSeen = samps.size();
			
			log.info("# Overview for {}", solverNames[i]);
			log.info("{} of {} samples could be decided:", posSamplesSeen, samplesSeen);
			log.info("\tMinimum: {}, Maximum: {}", samps.get(0), samps.get(samps.size()-1));
			
			double mean = sampleSums[i]/posSamplesSeen;
			
			log.info("\tMean: {}", mean);
			
			// variance and std dev
			double sqrsum = 0;
			for(Double d : samps){
				sqrsum += Math.pow(d-mean, 2); // for each data point: squared distance to mean
			}
			double variance = sqrsum/posSamplesSeen;
			
			log.info("\tVariance: {}", variance);
			log.info("\tStandard deviation: {}", Math.sqrt(variance));
			
			// median, first and third quartile
			int medianIndex = posSamplesSeen/2;
			int firstQIndex = (int) (posSamplesSeen*0.25);
			int thirdQIndex = (int) (posSamplesSeen*0.75);
			
			double median = (medianIndex %2 == 1) ? samps.get(medianIndex) : (samps.get(medianIndex-1) + samps.get(medianIndex))/2.0;
			double firstQ = (firstQIndex %2 == 1) ? samps.get(firstQIndex) : (samps.get(firstQIndex-1) + samps.get(firstQIndex))/2.0;
			double thirdQ = (thirdQIndex %2 == 1) ? samps.get(thirdQIndex) : (samps.get(thirdQIndex-1) + samps.get(thirdQIndex))/2.0;
			
			log.info("\tMedian: {}", median);
			log.info("\tFirst Quartile: {}, Third Quartile: {}", firstQ, thirdQ);
			
		}
		
		log.info("*****************************");
	}

	@Override
	public void countFileSeen() {
		filesSeen++;
	}

	@Override
	public int getFilesCount() {
		return filesSeen;
	}

	@Override
	public void countEmptyFileSeen() {
		emptyFilesSeen++;
	}

	@Override
	public int getEmptyFilesCount() {
		return emptyFilesSeen;
	}

	@Override
	public int getSamplesCount() {
		return samplesSeen;
	}

	@Override
	public void analyseSample(double[] features, double[] labels) {
		// for each solver
		for(int s=0; s<labels.length; s++){
			double l=labels[s];
			
			/*
			 * negative values are counted as negSamples.
			 * Otherwise, the time the solver needed is added to sampleSums
			 */
			
			if(l<0){
				negSamples[s]++;
			}
			else {
				sampleSums[s]+=l;
				posSamples.get(s).add(l);
			}
		}
		
		samplesSeen++; // count sample total
		
	}
	
	@Override
	public void analyseTrainingDataSample(String sampleString) {
		double[] labels = Arrays.stream(sampleString.split(":")[0].split(","))
				.mapToDouble(Double::parseDouble).toArray();
		analyseSample(null, labels);
	}

}
