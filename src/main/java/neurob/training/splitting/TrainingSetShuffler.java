package neurob.training.splitting;

import javafx.util.Pair;
import neurob.core.NeuroB;
import neurob.exceptions.NeuroBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * @author Jannik Dunkelau
 */
public class TrainingSetShuffler {
	private static final Logger log = LoggerFactory.getLogger(TrainingSetShuffler.class);

	/**
	 * Shuffles the predicate dump and writes
	 * the shuffled samples into the target file
	 * @param predicateDump
	 * @param target
	 */
	public static void shuffle(Path predicateDump, Path target) throws NeuroBException{
		shuffle(predicateDump, target, new Random());
	}

	/**
	 * Shuffles the predicate dump and writes the shuffled samples into the target file.
	 * The shuffling happens with respect to the given RNG.
	 * @param predicateDump
	 * @param target
	 * @param rng
	 * @throws NeuroBException
	 */
	public static void shuffle(Path predicateDump, Path target, Random rng) throws NeuroBException {
		log.info("Begin shuffling of {}", predicateDump);
		// get data
		List<Pair<Path, Pair<String,Integer>>> dataList = getDataList(predicateDump);

		// permute data
		Collections.shuffle(dataList, rng);

		// create target file
		log.info("Create target {} containing shuffled data", target);
		try {
			log.trace("Setting up target");
			BufferedWriter targetFile = Files.newBufferedWriter(target);

			dataList.stream().forEach(p -> copyLineToFile(p, targetFile));

		} catch (IOException e) {
			throw new NeuroBException("Could not create target file", e);
		}
	}

	/**
	 * Iterates over the predicate dump and returns a list of pairs
	 * of (file path, line number) for each sample in the predicate dump.
	 * @param predicateDump
	 * @return
	 */
	private static List<Pair<Path, Pair<String,Integer>>> getDataList(Path predicateDump) throws NeuroBException {
		List<Pair<Path, Pair<String,Integer>>> list = new ArrayList<>();

		try(Stream<Path> files = Files.walk(predicateDump)){
			log.trace("Generating data list to shuffle from {}", predicateDump);

			files.filter(Files::isRegularFile).forEach(f -> list.addAll(getDataListFromFile(f)));
		} catch (IOException e) {
			log.error("Could not access predicate dump correctly", e);
			throw new NeuroBException("Could not access predicate dump correctly", e);
		}

		return list;
	}

	private static List<Pair<Path,Pair<String, Integer>>> getDataListFromFile(Path file) {
		log.trace("Getting samples from {}", file);
		List<Pair<Path, Pair<String, Integer>>> list = new ArrayList<>();

		// count entries
		AtomicInteger counter = new AtomicInteger(0); // number of lines in the file that contain data
		AtomicReference<String> sourceAnno = new AtomicReference<>(); // capture source annotation
		try(Stream<String> lines = Files.lines(file)){
//			counter = lines.filter(l -> !l.startsWith("#")).count(); // skip #-lines; are comment
			lines.forEach(l->{
				if(l.startsWith("#")){
					// do not count but get possible source machine file
					if(l.startsWith("#source:")){
						sourceAnno.set(l.substring(8));
					}
				} else {
					// entry to add to list
					list.add(new Pair<>(file, new Pair<>(sourceAnno.get(),counter.getAndIncrement())));
				}
			});
		} catch (IOException e) {
			log.error("Could not access file correctly", e);
		}

		return list;
	}

	private static void copyLineToFile(Pair<Path, Pair<String,Integer>> pair, BufferedWriter targetFile) {
		// split up pair
		Path source = pair.getKey();
		String srcMch = pair.getValue().getKey();
		int line = pair.getValue().getValue();

		log.trace("Copy line {} from {}", line, source);



		// get correct line and write to target file
		try(Stream<String> lines = Files.lines(source)){
			Optional<String> str =
					lines
							.filter(l->!l.startsWith("#"))
							.skip(line)
							.findFirst();

			if(str.isPresent()){
				targetFile.write("#source:"+srcMch);
				targetFile.newLine();
				targetFile.write(str.get());
				targetFile.newLine();
				targetFile.flush();
			} else {
				log.warn("No line #{} found in {}", line, source);
			}
		} catch (IOException e) {
			log.warn("Could not copy line {} from {}", line, source);
		}
	}
}
