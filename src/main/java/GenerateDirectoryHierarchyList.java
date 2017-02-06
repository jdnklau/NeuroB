import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class is meant to generate a simple list containing a directory hierarchy.
 * 
 * Each line is a path in this hierarchy.
 * 
 * It seems necessary for now, to reduce the time investment needed to sort through the machines causing trouble with ProB2.
 * The list entries themselves can then be simply deleted, as soon as the directory has been successfully searched for machines 
 * and their features.
 * 
 * @author jannik
 *
 */
@Deprecated
public class GenerateDirectoryHierarchyList {
	BufferedWriter out;
	Path mainDir;
	
	public GenerateDirectoryHierarchyList(){
		Path listfile = Paths.get("prob_examples/directories.txt");
		mainDir = Paths.get("prob_examples/reduced_examples/");
		try {
			out = Files.newBufferedWriter(listfile);
			findDirectories(mainDir);
			out.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		new GenerateDirectoryHierarchyList();

	}
	
	private void findDirectories(Path directory){
		try{
			
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
    	        for (Path entry : stream) {
    	        	// check if directory or not; recursion if so
    	            if (Files.isDirectory(entry)) {
    	            	findDirectories(entry); //  distribute the file recusively
    	            }
    	            
    	        }
    	    }
    		catch (IOException e){
    			System.out.println("Could not access directory "+directory+": "+e.getMessage());
    		}
			
    		out.write(mainDir.relativize(directory).toString());
    		out.write("\n");
    		out.flush();
    		System.out.println("Added: "+directory);
    		
    		
    		
    	} catch (IOException e){
    		System.out.println("COULD NOT ADD "+directory+": "+e.getMessage());
    	}
		
		
	}

}
