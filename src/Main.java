import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.FeatureCollector;
import neurob.core.features.FeatureData;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class Main {

	public static void main(String[] args) {
		BParser p = new BParser();
		Start ast;
		FeatureCollector fc = new FeatureCollector();
		
		try {
			// get AST from predicate
			ast = p.parse("#PREDICATE ! x . ( x : NAT => # y . (y : NAT & y > x))"
					+ " & # n . (n = 2)", false);
			
			// get features
			ast.apply(fc);
			
			FeatureData fd = fc.getFeatureData();
			System.out.println(fd);
			
		} catch (BException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
