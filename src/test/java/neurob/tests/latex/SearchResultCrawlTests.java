package neurob.tests.latex;

import static org.junit.Assert.*;

import neurob.latex.hyperparametersearch.SearchResultCrawler;
import neurob.latex.hyperparametersearch.SearchResultEntry;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Jannik Dunkelau
 */
public class SearchResultCrawlTests {
	private final Path dir = Paths.get("src/test/resources/test_epochs");

	@Test
	public void resultCountTest(){
		int n = 3;
		SearchResultCrawler crawler = new SearchResultCrawler(3);

		int expected = n;
		int actual = crawler.crawl(dir);

		assertEquals("Number of top n results does not match", expected, actual);
	}

	@Test
	public void resultModelTest(){
		SearchResultCrawler crawler = new SearchResultCrawler(3);
		crawler.crawl(dir);

		List<SearchResultEntry> results = crawler.getResults();

		int expected = 5;
		int actual = results.get(0).getIndex();
		assertEquals("Top element does not match",expected,actual);

		expected = 0;
		actual = results.get(1).getIndex();
		assertEquals("Second element does not match",expected,actual);

		expected = 1;
		actual = results.get(2).getIndex();
		assertEquals("Third element does not match",expected,actual);
	}

	@Test
	public void resultPerformancesTest(){
		SearchResultCrawler crawler = new SearchResultCrawler(3);
		crawler.crawl(dir);

		List<SearchResultEntry> results = crawler.getResults();

		double expected = 0.9;
		double actual = results.get(0).getValue();
		assertEquals("Top element does not match",expected,actual,0.001);

		expected = 0.7;
		actual = results.get(1).getValue();
		assertEquals("Second element does not match",expected,actual,0.001);

		expected = 0.6;
		actual = results.get(2).getValue();
		assertEquals("Third element does not match",expected,actual,0.001);
	}
}
