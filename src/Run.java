
import neurob.NeuroB;
import neurob.core.nets.DefaultPredicateSolverPredictionNet;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class Run {

	public static void main(String[] args) {
		NeuroB nb = new NeuroB(
				new DefaultPredicateSolverPredictionNet()
				.setSeed(0L)
				.build());
		
		System.out.println("Done");

	}

}
