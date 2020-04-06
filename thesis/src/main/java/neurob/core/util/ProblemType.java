package neurob.core.util;

/**
 * Enumeration class for the different types of problems Neural Networks try to solve.
 * <p>
 * Those problems are
 * <ol>
 * 	<li>{@code CLASSIFICATION},
 * 		deciding the most probable class for a given input
 * 	</li>
 * 	<li>{@code REGRESSION},
 * 		approximating a continuous function over the possible input domain
 * 	</li>
 * </ol>
 * 
 * @author jannik
 *
 */
public enum ProblemType {
	CLASSIFICATION,
	REGRESSION,
	// ATTRIBUTE_CLASSIFICATION // TODO: implement this possibility?
}
