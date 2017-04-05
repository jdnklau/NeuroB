package neurob.training.splitting;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class containing common methods for training set splitting.
 * @author jannik
 *
 */
public class TrainingSetSplitter {
	private static Logger log = LoggerFactory.getLogger(TrainingSetSplitter.class);

	/**
	 * Iterates over the files in the source directory and splits them to the given target directories.
	 * <p>
	 * Using the given RNG, the files are distributed in a way that a {@code ratio} sized part will
	 * be located in {@code first}, and the rest in {@code second}. 
	 * For this, the found files are iterated linewise, and for each line the decision is made whether it will
	 * go first or second.
	 * This means especially, that for each source file found two corresponding target files will be created, one in
	 * {@code first}, the other in {@code second}. Those target files hold the corresponding lines of the source file.
	 * 
	 * @param source The source directory to search for training files in
	 * @param first The first target directory; ratio samples will be placed here
	 * @param second The second target directory; 1-ratio samples will be placed here
	 * @param ratio A number from the interval [0,1]
	 * @param rng RNG to decide the target directory per sample
	 */
	public static void splitLinewise(Path source, Path first, Path second, double ratio, Random rng){
		splitLinewise(source, first, second, ratio, rng, false);
	}
	
	/**
	 * Iterates over the files in the source directory and splits them to the given target directories.
	 * <p>
	 * Using the given RNG, the files are distributed in a way that a {@code ratio} sized part will
	 * be located in {@code first}, and the rest in {@code second}. 
	 * For this, the found files are iterated linewise, and for each line the decision is made whether it will
	 * go first or second.
	 * This means especially, that for each source file found two corresponding target files will be created, one in
	 * {@code first}, the other in {@code second}. Those target files hold the corresponding lines of the source file.
	 * <p>
	 * Setting {@code copyHeader} to true makes the first line in the found source files to be treated as header line.
	 * Thus it will be copied to both targets.
	 *
	 * @param source The source directory to search for training files in
	 * @param first The first target directory; ratio samples will be placed here
	 * @param second The second target directory; 1-ratio samples will be placed here
	 * @param ratio A number from the interval [0,1]
	 * @param rng RNG to decide the target directory per sample
	 * @param copyHeader Whether the first line should be handled as header or not
	 */
	public static void splitLinewise(Path source, Path first, Path second, 
			double ratio, Random rng, boolean copyHeader){
		splitLinewise(source, first, second, ratio, rng, false, "");
	}
	
	/**
	 * Iterates over the files in the source directory and splits them to the given target directories.
	 * <p>
	 * Using the given RNG, the files are distributed in a way that a {@code ratio} sized part will
	 * be located in {@code first}, and the rest in {@code second}. 
	 * For this, the found files are iterated linewise, and for each line the decision is made whether it will
	 * go first or second.
	 * This means especially, that for each source file found two corresponding target files will be created, one in
	 * {@code first}, the other in {@code second}. Those target files hold the corresponding lines of the source file.
	 * <p>
	 * Setting {@code copyHeader} to true makes the first line in the found source files to be treated as header line.
	 * Thus it will be copied to both targets.
	 * <p>
	 * The {@code extension} will be used to filter the source files. Only source files ending with the given extension
	 * will be taken into account. Examples: ".csv" only looks for csv files, "v" would take all files with names ending
	 * in a v into account, and "" (empty string) uses them all.
	 * 
	 * @param source The source directory to search for training files in
	 * @param first The first target directory; ratio samples will be placed here
	 * @param second The second target directory; 1-ratio samples will be placed here
	 * @param ratio A number from the interval [0,1]
	 * @param rng RNG to decide the target directory per sample
	 * @param copyHeader Whether the first line should be handled as header or not
	 * @param extension Only files ending with this string will be taken into account
	 */
	public static void splitLinewise(Path source, Path first, Path second, 
			double ratio, Random rng, boolean copyHeader, String extension){
		checkRatio(ratio);
		
		log.info("Splitting training set {} to {} and {}, by ratio of {}", source, first, second, ratio);
		
		try(Stream<Path> stream = Files.walk(source)){
			
			stream
				.filter(Files::isRegularFile)
				.filter(p->p.toString().endsWith(extension))
				.forEach(p->splitFileLinewise(source, p,first,second,ratio,copyHeader,rng));
			
		} catch (IOException e) {
			log.error("Could not split training set correctly.", e);
		}
	}
	
	/**
	 * Splits all csv files found in the {@code source} directory.
	 * <p>
	 * Like {@link #splitLinewise(Path, Path, Path, double, Random, boolean, String)} with
	 * {@code copyHeader=true} and {@code extension=".csv"}
	 * @param source The source directory to search for training files in
	 * @param first The first target directory; ratio samples will be placed here
	 * @param second The second target directory; 1-ratio samples will be placed here
	 * @param ratio A number from the interval [0,1]
	 * @param rng RNG to decide the target directory per sample
	 * @see #splitLinewise(Path, Path, Path, double, Random, boolean, String)
	 */
	public static void splitCSV(Path source, Path first, Path second, double ratio, Random rng){
		splitLinewise(source, first, second, ratio, rng, true, ".csv");
	}

	private static void splitFileLinewise(Path sourceDir, Path sourceFile, Path first, Path second, 
			double ratio, boolean copyHeader, Random rng) {
		log.debug("Splitting {}", sourceFile);
		
		// for path/to/source/subdir/file with path/to/source as source dir, get subdir/
		Path sourceSubDir = sourceDir.relativize(sourceFile).getParent();
		
		// set up paths for new training data files
		Path sourceFileName = sourceFile.getFileName();
		Path fstFile = first.resolve(sourceSubDir).resolve(sourceFileName);
		Path sndFile = second.resolve(sourceSubDir).resolve(sourceFileName);
		
		log.debug("\tSplitting {} to {} and {}", sourceFile, fstFile,sndFile);
		
		// create target files
		BufferedWriter wrFst;
		BufferedWriter wrSnd;
		try {
			// set up directories
			Files.createDirectories(fstFile.getParent());
			Files.createDirectories(sndFile.getParent());
			
			// set up new files to write
			wrFst = Files.newBufferedWriter(fstFile);
			wrSnd = Files.newBufferedWriter(sndFile);
		} catch (IOException e) {
			log.error("Could not split up {} correctly.", sourceFile, e);
			return;
		}
		
		// copy header line
		if(copyHeader){
			copyHeader(sourceFile, wrFst, wrSnd);
		}
		

		try(Stream<String> lines = Files.lines(sourceFile)){
			lines
			.skip(copyHeader ? 1 : 0)
			.forEach(line->splitFileLinewise(line,wrFst,wrSnd,ratio,rng));
		} catch (IOException e) {
			log.error("Could not split up {}", sourceFile, e);
		}
	}

	private static void splitFileLinewise(String line, BufferedWriter wrFst, BufferedWriter wrSnd, 
			double ratio, Random rng) {
		// decide which target file should be used
		BufferedWriter wr = (rng.nextDouble() <= ratio) ? wrFst : wrSnd;
		
		try {
			wr.write(line);
			wr.newLine();
			wr.flush();
		} catch (IOException e) {
			log.error("Could not split for line {}", line, e);
		}
	}

	private static void copyHeader(Path sourceFile, BufferedWriter wrFst, BufferedWriter wrSnd) {
		try(Stream<String> lines = Files.lines(sourceFile)){
			Optional<String> header = lines.findFirst();
			
			wrFst.write(header.get());
			wrFst.newLine();
			wrFst.flush();
			
			wrSnd.write(header.get());
			wrSnd.newLine();
			wrSnd.flush();
		} catch (IOException | NoSuchElementException e) {
			log.error("Could not copy header from {}", sourceFile, e);
			return;
		}
	}

	private static void checkRatio(double ratio){
		if(ratio<0 || ratio > 1){
			throw new IllegalArgumentException("ratio parameter has to be a number in the interval [0,1],)"
					+ " but got "+ratio);
		}
	}
	
	/**
	 * Iterates over the files in the source directory and splits them to the given target directories.
	 * <p>
	 * Using the given RNG, the files are distributed in a way that a {@code ratio} sized part will
	 * be located in {@code first}, and the rest in {@code second}. 
	 * For this, the found files are copied to the respective location. 
	 * 
	 * @param source The source directory to search for training files in
	 * @param first The first target directory; ratio samples will be placed here
	 * @param second The second target directory; 1-ratio samples will be placed here
	 * @param ratio A number from the interval [0,1]
	 * @param rng RNG to decide the target directory per sample
	 */
	public static void splitFilewise(Path source, Path first, Path second, double ratio, Random rng){
		splitFilewise(source, first, second, ratio, rng, "");
	}

	
	/**
	 * Iterates over the files in the source directory and splits them to the given target directories.
	 * <p>
	 * Using the given RNG, the files are distributed in a way that a {@code ratio} sized part will
	 * be located in {@code first}, and the rest in {@code second}. 
	 * For this, the found files are copied to the respective location. 
	 * <p>
	 * The {@code extension} will be used to filter the source files. Only source files ending with the given extension
	 * will be taken into account. Examples: ".csv" only looks for csv files, "v" would take all files with names ending
	 * in a v into account, and "" (empty string) uses them all.
	 * 
	 * @param source The source directory to search for training files in
	 * @param first The first target directory; ratio samples will be placed here
	 * @param second The second target directory; 1-ratio samples will be placed here
	 * @param ratio A number from the interval [0,1]
	 * @param rng RNG to decide the target directory per sample
	 * @param extension Only files ending with this string will be taken into account
	 */
	public static void splitFilewise(Path source, Path first, Path second, double ratio, Random rng, String extension){
		checkRatio(ratio);
		
		log.info("Splitting training set {} to {} and {}, by ratio of {}", source, first, second, ratio);
		
		try(Stream<Path> stream = Files.walk(source)){
			stream
			.filter(Files::isRegularFile)
			.filter(p->p.toString().endsWith(extension))
			.forEach(p->copyFileToTarget(source, p, (rng.nextDouble()<=ratio) ? first : second));
		} catch (IOException e) {
			log.error("Could not split up training data from {}", source, e);
		}
		
		
	}

	private static void copyFileToTarget(Path sourceDir, Path sourceFile, Path targetDir) {
		log.debug("Splitting {}", sourceFile);
		
		// for path/to/source/subdir/file with path/to/source as source dir, get subdir/
		Path sourceSubDir = sourceDir.relativize(sourceFile).getParent();
		
		// set up paths for new training data files
		Path sourceFileName = sourceFile.getFileName();
		Path targetFile = targetDir.resolve(sourceSubDir).resolve(sourceFileName);
		
		try{
			Files.createDirectories(targetFile.getParent());
			log.debug("\tCopying {} to {}", sourceFile, targetFile);
			Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			log.error("Could not set up target file {}", targetFile, e);
		}
	}
}

