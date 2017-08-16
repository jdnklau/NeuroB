package neurob.latex.hyperparametersearch;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Jannik Dunkelau
 */
public class SearchResultEntry {
	private int index;
	private double value;
	private Path source;

	public SearchResultEntry(int index, double value, Path source) {
		this.index = index;
		this.value = value;
		this.source = source;
	}

	public boolean isGreaterThan(SearchResultEntry s){
		return value > s.getValue();
	}

	public int getIndex() {
		return index;
	}

	public double getValue() {
		return value;
	}

	public Path getSource() {
		return source;
	}

	@Override
	public String toString() {
		return "("+index+","+value+")";
	}
}
