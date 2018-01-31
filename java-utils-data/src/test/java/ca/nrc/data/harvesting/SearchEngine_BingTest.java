package ca.nrc.data.harvesting;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.nrc.data.harvesting.BingSearchEngine;
import ca.nrc.data.harvesting.SearchEngine;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;
import ca.nrc.testing.AssertHelpers;

public class SearchEngine_BingTest extends SearchEngineTest {

	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}


	@Override
	protected SearchEngine makeSearchEngine() throws IOException, SearchEngineException {
		return new BingSearchEngine();
	}

	@Test
	public void test__getHitDirectURL__HappyPath() throws MalformedURLException, IOException, URISyntaxException, SearchEngineException {
		String bingURL = "https://www.bing.com/cr?IG=4F40DA1845D24932A9D0F66DC4FC14EA&CID=2B66391AD87368C00D4B30F1D9426911&rd=1&h=8If1ewpZIAj-EF57E375KJmLrrKvbvJmfeNbDiXXQwE&v=1&r=https%3a%2f%2fwww.vesselfinder.com%2fvessels%2fARCTIC-IMO-9315173-MMSI-636014506&p=DevEx,5098.1";
		String gotDirectURL = new BingSearchEngine().getHitDirectURL(bingURL);
		String expDirectURL = "https://www.vesselfinder.com/vessels/ARCTIC-IMO-9315173-MMSI-636014506";
		AssertHelpers.assertStringEquals(expDirectURL, gotDirectURL);
	}

	@Test
	public void test__makeBingQueryString__MultipleORedTerms() throws Exception {
		String[] terms = new String[]{"machine learning", "pattern recognition"};
		SearchEngine.Query query = new SearchEngine.Query(terms);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "machine learning, pattern recognition";
		AssertHelpers.assertStringEquals(expQueryString, gotQueryString);
	}

	@Test
	public void test__makeBingQueryString__MultipleORedTermsWithSite() throws Exception {
		String[] terms = new String[]{"machine learning", "pattern recognition"};
		String site = "somesite.com";
		SearchEngine.Query query = new SearchEngine.Query(terms).setSite(site);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "+site:somesite.com machine learning, pattern recognition";
		AssertHelpers.assertStringEquals(expQueryString, gotQueryString);
	}
	
	
	@Test
	public void test__makeBingQueryString__SingleTerm() throws Exception {
		String[] terms = new String[]{"machine learning"};
		SearchEngine.Query query = new SearchEngine.Query(terms);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "machine learning";
		AssertHelpers.assertStringEquals(expQueryString, gotQueryString);
	}

	@Test
	public void test__makeBingQueryString__FuzzyQuery() throws Exception {
		String fuzzyQuery = "this is a fuzzy query";
		SearchEngine.Query query = new SearchEngine.Query(fuzzyQuery);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "this is a fuzzy query";
		AssertHelpers.assertStringEquals(expQueryString, gotQueryString);
	}
	
	@Test
	public void test__makeBingQueryString__FuzzyQuery_NEWSType() throws Exception {
		String fuzzyQuery = "this is a fuzzy query";
		SearchEngine.Query query = new SearchEngine.Query(fuzzyQuery).setType(SearchEngine.Type.NEWS);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "this is a fuzzy query AND +(\"news\")";
		AssertHelpers.assertStringEquals(expQueryString, gotQueryString);
	}		
	
	private void printHitUrls(List<SearchEngine.Hit> hits, String heading) {
		System.out.println("\n\nHits for: "+heading);
		if (hits.size() == 0) {
			System.out.println("   NO HITS FOUND");
		} else {
			for (SearchEngine.Hit hit: hits) {
				System.out.println("   "+hit.url.toString());
			}			
		}
	}
	
	

}
