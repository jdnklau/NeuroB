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
	 * @return The path to the connoted source.
	 */
	public Path getSource(){return source;}

	/**
	 * @return Connotated comment to this training sample. May be an empty string, if no comment is present.
	 */
	public String getComment(){return comment;}

	/**
	 * The feature vector as comma separated string
	 * @return
	 */
	public String getFeatureString(){return getCSVString(features);}

	/**
	 * @return The label vector as comma separated string
	 */
	public String getLabelString(){return getLabelString(ProblemType.REGRESSION);}

	/**
	 * Returns the label vector as comma separated string.
	 * <p>
	 * For classification problems, the entries are integers. Otherwise,
	 * they are double values and thus have decimal places.
	 * @param p
	 * @return The label vector as comma separated string
	 */
	public String getLabelString(ProblemType p){
		switch (p) {
		case CLASSIFICATION:
			return getCSVString(getIntLabels());
		case REGRESSION:
		default:
			return getCSVString(getLabels());
		}
	}

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
		return getFeatureString()+","+getLabelString(p);
	}
}
