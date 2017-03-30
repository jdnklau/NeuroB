package neurob.training.generators.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import neurob.core.util.ProblemType;

/**
 * Wrapper class for training data, consisting of a pairing of feature and labelling data
 * @author jannik
 */
public class TrainingData {
	private double[] features;
	private double[] labels;
	private Path source;
	private String comment;
	
	public TrainingData(double[] features, double[] labels, Path source) {
		this(features, labels, source, "");
	}
	
	public TrainingData(double[] features, double[] labels, Path source, String comment) {
		this.features = features;
		this.labels = labels;
		
		this.source = source;
		
		this.comment = (comment == null) ? "" : comment;
	}
	
	public double[] getFeatures(){return features;}
	/**
	 * @return The label vector
	 * @see #getClassificationLabels()
	 */
	public double[] getLabels(){return labels;}
	/**
	 * @return The label vector as integers.
	 */
	public int[] getIntLabels(){
		return Arrays.stream(labels).mapToInt(d->new Double(d).intValue()).toArray();
	}
	
	/**
	 * 
	 * @return The connotated path to the source or a path to nosource.nsrc file.
	 */
	public Path getSource(){return source;}
	public String getComment(){return comment;}

	public String getFeatureString(){return getCSVString(features);}
	public String getLabelString(){return getCSVString(labels);}

	protected String getCSVString(int[] data){
		return String.join(",", 
				Arrays.stream(data)
				.mapToObj(Integer::toString)
				.collect(Collectors.toList()));
	}
	protected String getCSVString(double[] data){
		return String.join(",", 
				Arrays.stream(data)
				.mapToObj(Double::toString)
				.collect(Collectors.toList()));
	}
	
	/**
	 * Returns a concatenated version of features and labels as one vector
	 * @return
	 */
	public double[] getTrainingVector(){
		return DoubleStream.concat(Arrays.stream(features), Arrays.stream(labels)).toArray();
	}
	
	/**
	 * Returns a concatenated version of features and labels as
	 * comma separated string.
	 * @return
	 */
	public String getTrainingVectorString(){
		return getTrainingVectorString(ProblemType.REGRESSION);
	}
	
	/**
	 * Returns a concatenated version of features and labels as comma separated string.
	 * The labels are cast to integers, thus do not have any decimal places.
	 * @param p
	 * @return
	 */
	public String getTrainingVectorString(ProblemType p){
		switch(p){
		case CLASSIFICATION:
			return getCSVString(getFeatures())+","+getCSVString(getIntLabels());
		case REGRESSION:
		default:
			return getCSVString(getFeatures())+","+getCSVString(getLabels());
		}
	}
}
