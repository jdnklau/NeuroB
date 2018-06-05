package neurob.latex.interfaces;

/**
 * Same as {@link TeXable}, but standalones load tikz and pgfplots
 * @author Jannik Dunkelau
 */
public interface PGFPlotable extends TeXable{
	@Override
	default String getTeXStandalone() {
		String standalone;

		standalone = "\\documentclass{standalone}\n" +
				"\\usepackage{tikz}\n" +
				"\\usepackage{pgfplots}\n" +
				"\\begin{document}\n" +
				getTeX() +
				"\\end{document}";
		return standalone;
	}
}
