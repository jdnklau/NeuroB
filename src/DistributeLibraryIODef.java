import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * As in the examples some machines are referencing LibraryIO.def, but it does not come with each file,
 * running this class ensures, that each and every directory and subdirectory has a copy of it.
 * 
 * @author jannik
 *
 */
public class DistributeLibraryIODef {
	public static final Path libraryIOpath = Paths.get("prob_examples/LibraryIO.def");

	public static void main(String[] args) {
		DistributeLibraryIODef.distribute(Paths.get("prob_examples/public_examples/B/"));
	}
	
	public static void distribute(Path directory){
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
			
	        for (Path entry : stream) {

	        	// check if directory or not; recursion if so
	            if (Files.isDirectory(entry)) {
	            	
	            	Path newLibraryIOPath = entry.resolve("LibraryIO.def");
	            	try{
	            		Files.copy(libraryIOpath, newLibraryIOPath, StandardCopyOption.REPLACE_EXISTING);
	            		System.out.println("Created: "+newLibraryIOPath);
	            	} catch (IOException e){
	            		System.out.println("COULD NOT CREATE "+newLibraryIOPath+": "+e.getMessage());
	            	}
	            	
	            	distribute(entry); //  distribute the file recusively
	            	
	            }
	            
	        }
	    }
		catch (IOException e){
			System.out.println("Could not access directory "+directory+": "+e.getMessage());
		}
	}

}
