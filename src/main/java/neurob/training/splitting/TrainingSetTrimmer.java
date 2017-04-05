package neurob.training.splitting;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.TrainingDataGenerator;

public class TrainingSetTrimmer {
	private static Logger log = LoggerFactory.getLogger(TrainingSetTrimmer.class);
	
	public static void trimLineWise(Path sourceDirectory, Path targetDirectory, TrainingAnalysisData analysisData,
			TrainingDataGenerator generator){
		trimLineWise(sourceDirectory, targetDirectory, analysisData, generator, false);
	}

	public static void trimLineWise(Path sourceDirectory, Path targetDirectory, TrainingAnalysisData analysisData,
			TrainingDataGenerator generator, boolean copyHeader){
		trimLineWise(sourceDirectory, targetDirectory, analysisData, generator, copyHeader, "");
	}
	
	public static void trimLineWise(Path sourceDirectory, Path targetDirectory, TrainingAnalysisData analysisData,
			TrainingDataGenerator generator, boolean copyHeader, String suffix){
		log.info("Trimming training set {} to {}", sourceDirectory, targetDirectory);
		
		try(Stream<Path> stream = Files.walk(sourceDirectory)){
			stream
				.filter(Files::isRegularFile)
				.filter(p->p.toString().endsWith(suffix))
				.forEach(p->trimFileLinewise(sourceDirectory, p, targetDirectory,copyHeader,analysisData, generator));
			
		} catch (IOException e) {
			log.error("Could not split training set correctly.", e);
		}
	}

	private static void trimFileLinewise(Path sourceDir, Path sourceFile, Path targetDir, boolean copyHeader,
			TrainingAnalysisData analysisData, TrainingDataGenerator generator) {
		
		// for path/to/source/subdir/file with path/to/source as source dir, get subdir/
		Path sourceSubDir = sourceDir.relativize(sourceFile).getParent();
		
		// set up paths for new training data files
		Path sourceFileName = sourceFile.getFileName();
		Path targetFile = targetDir.resolve(sourceSubDir).resolve(sourceFileName);
		
		log.debug("\tTrimming {}, writing result to {}", sourceFile, targetFile);
		
		// set up target file
		BufferedWriter target;
		try {
			Files.createDirectories(targetFile.getParent());
			target= Files.newBufferedWriter(targetFile);
		} catch (IOException e) {
			log.error("\tCould not setup target file {}", sourceFile, e);
			return;
		}
		
		if(copyHeader)
			copyHeader(sourceFile, target);
		
		try(Stream<String> lines = Files.lines(sourceFile)){
			lines
			.skip(copyHeader?1:0)
			.filter(line->!analysisData.canSampleBeTrimmed(generator.labellingFromSample(line)))
			.forEach(line->trimLinewise(line,target));
		} catch (IOException e) {
			log.error("\tCould not trim {}", sourceFile, e);
		}
	}

	private static void copyHeader(Path sourceFile, BufferedWriter target) {
		try(Stream<String> lines = Files.lines(sourceFile)){
			Optional<String> header = lines.findFirst();
			
			target.write(header.get());
			target.newLine();
			target.flush();
		} catch (IOException | NoSuchElementException e) {
			log.warn("\tCould not copy header from {}", sourceFile, e);
		}
	}
	
	/**
	 * Actually does nothing but writing the sample as new line to the target file
	 * @param sample
	 * @param targetFile
	 */
	private static void trimLinewise(String sample, BufferedWriter targetFile){
		try {
			targetFile.write(sample);
			targetFile.newLine();
			targetFile.flush();
		} catch (IOException e) {
			log.warn("\tCould not write sample {} to trimmed file", sample, e);
		}
	}
	
	
	
	public static void trimFilewise(Path source, Path target, TrainingAnalysisData analysisData,
			TrainingDataGenerator generator){
		trimFilewise(source, target, analysisData, generator, "");
	}
	
	public static void trimFilewise(Path source, Path target, TrainingAnalysisData analysisData,
			TrainingDataGenerator generator, String suffix){
		log.info("Trimming training set {} to {}", source, target);
		
		try(Stream<Path> stream = Files.walk(source)){
			stream
			.filter(Files::isRegularFile)
			.filter(p->p.toString().endsWith(suffix))
			.filter(p->!analysisData.canSampleBeTrimmed(generator.labellingFromSample(p.toString())))
			.forEach(p->copyFileToTarget(source, p, target));
		} catch (IOException e) {
			log.error("Could not trim training data from {}", source, e);
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
