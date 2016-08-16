import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class Main {

	public static void main(String[] args) {
		BParser p = new BParser();
		Start AST;
		System.out.println("yay");
		
		try {
			AST = p.parse("#PREDICATE x=5 & x > 3", false);
			System.out.println(AST);
		} catch (BException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
