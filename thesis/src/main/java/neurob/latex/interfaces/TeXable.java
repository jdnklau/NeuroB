package neurob.latex.interfaces;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementing classes support saving their data to LaTex format.
 * @author Jannik Dunkelau
 */
public interface TeXable {
	/**
	 * @return TeX-String for displaying object data
	 */
	String getTeX();

	/**
	 * @return TeX-String for standalone TeX-File displaying data
	 */
	default
	String getTeXStandalone(){
		String standalone;

		standalone = "\\documentclass{standalone}\n" +
				"\\begin{document}\n" +
				getTeX() +
				"\\end{document}";
		 return standalone;
	}

	/**
	 * Writes the string returned by {@link #getTeX()} to the specified target file
	 * @param targetFile
	 * @throws IOException
	 */
	default
	void writeTeX(Path targetFile) throws IOException{
		try(BufferedWriter writer = Files.newBufferedWriter(targetFile)){
			writer.write(getTeX());
			writer.flush();
		} catch(IOException e) {
			// unsure if this catch block is necessary, or try with resources would close
			// the writer either way if an exception is thrown
			throw e;
		}
	}

	/**
	 * Writes the string returned by {@link #getTeXStandalone()} to the specified target file
	 * @param targetFile
	 * @throws IOException
	 */
	default
	void writeTeXStandalone(Path targetFile) throws IOException{
		try(BufferedWriter writer = Files.newBufferedWriter(targetFile)){
			writer.write(getTeXStandalone());
			writer.flush();
		} catch(IOException e) {
			// unsure if this catch block is necessary, or try with resources would close
			// the writer either way if an exception is thrown
			throw e;
		}
	}
}
