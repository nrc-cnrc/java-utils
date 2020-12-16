package ca.nrc.data.harvesting;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import ca.nrc.config.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;
import ca.nrc.testing.AssertString;

public class SearchEngine_BingTest extends SearchEngineTest {

	private static final String bingTestKeyPropName = "ca.nrc.data.harvesting.bingTestAPIKey";
	private String bingTestAPIKey = null;

	protected static String assumeTestBingKeyIsDefined() throws Exception {
		String key = Config.getConfigProperty(bingTestKeyPropName, false);
		Assume.assumeTrue(
			"No bing key defined. Skipping all tests in SearchEngine_BingTest." +
			"To run those tests, obtain a Bing key from Microsoft Azure and setup a config property "+bingTestKeyPropName+" with that value.",
			key != null);

		return key;
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();

		// Don't run the tests unless a Bing key has been defined.
		//
		this.bingTestAPIKey = assumeTestBingKeyIsDefined();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected Integer engineMaxHitsPerPage() {
		return 50;
	}
	

	@Override
	protected SearchEngine makeSearchEngine() throws IOException, SearchEngineException {
		return new BingSearchEngine(bingTestAPIKey);
	}

	@Test
	public void test__BingSearchEngine__Constructor() throws Exception {
		// To create a BingSearchEngine, you must first get a Bing Web Search API key
		// from Microsof Azure:
		//
		//    https://www.microsoft.com/en-us/bing/apis/bing-web-search-api
		//
		// This test assumes that such a key has been retrieved from a props
		// file.
		BingSearchEngine searchEngine = new BingSearchEngine(bingTestAPIKey);
	}


		@Test
	public void test__getHitDirectURL__HappyPath() throws MalformedURLException, IOException, URISyntaxException, SearchEngineException {
		String bingURL = "https://www.bing.com/cr?IG=4F40DA1845D24932A9D0F66DC4FC14EA&CID=2B66391AD87368C00D4B30F1D9426911&rd=1&h=8If1ewpZIAj-EF57E375KJmLrrKvbvJmfeNbDiXXQwE&v=1&r=https%3a%2f%2fwww.vesselfinder.com%2fvessels%2fARCTIC-IMO-9315173-MMSI-636014506&p=DevEx,5098.1";
		String gotDirectURL = new BingSearchEngine().getHitDirectURL(bingURL);
		String expDirectURL = "https://www.vesselfinder.com/vessels/ARCTIC-IMO-9315173-MMSI-636014506";
		AssertString.assertStringEquals(expDirectURL, gotDirectURL);
	}

	@Test
	public void test__makeBingQueryString__MultipleORedTerms() throws Exception {
		String[] terms = new String[]{"machine learning", "pattern recognition"};
		SearchEngine.Query query = new SearchEngine.Query(terms);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "machine learning, pattern recognition";
		AssertString.assertStringEquals(expQueryString, gotQueryString);
	}

	@Test
	public void test__makeBingQueryString__MultipleORedTermsWithSite() throws Exception {
		String[] terms = new String[]{"machine learning", "pattern recognition"};
		String site = "somesite.com";
		SearchEngine.Query query = new SearchEngine.Query(terms).setSite(site);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "+site:somesite.com machine learning, pattern recognition";
		AssertString.assertStringEquals(expQueryString, gotQueryString);
	}
	
	
	@Test
	public void test__makeBingQueryString__SingleTerm() throws Exception {
		String[] terms = new String[]{"machine learning"};
		SearchEngine.Query query = new SearchEngine.Query(terms);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "machine learning";
		AssertString.assertStringEquals(expQueryString, gotQueryString);
	}

	@Test
	public void test__makeBingQueryString__FuzzyQuery() throws Exception {
		String fuzzyQuery = "this is a fuzzy query";
		SearchEngine.Query query = new SearchEngine.Query(fuzzyQuery);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "this is a fuzzy query";
		AssertString.assertStringEquals(expQueryString, gotQueryString);
	}
	
	@Test
	public void test__makeBingQueryString__FuzzyQuery_NEWSType() throws Exception {
		String fuzzyQuery = "this is a fuzzy query";
		SearchEngine.Query query = new SearchEngine.Query(fuzzyQuery).setType(SearchEngine.Type.NEWS);
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "this is a fuzzy query AND +(\"news\")";
		AssertString.assertStringEquals(expQueryString, gotQueryString);
	}	
	
	@Test
	public void test__makeBingQueryString__Inuktut() throws Exception {
		String queryString = "ᐅᑉᐱᕐᓂᕐᒥᒃ";
		SearchEngine.Query query = 
				new SearchEngine.Query(queryString).setLang("iu");
		
		BingSearchEngine searchEngine = new BingSearchEngine();
		String gotQueryString = searchEngine.makeBingQueryString(query);
		String expQueryString = "ᐅᑉᐱᕐᓂᕐᒥᒃ AND -(\"the\")";
		AssertString.assertStringEquals(expQueryString, gotQueryString);
	}

	@Test
	public void test__BingSearchEngine__Clone() throws Exception {
		BingSearchEngine searchEngine = new BingSearchEngine();
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValueAsString(searchEngine);
	}
	
	/////////////////////////////////
	// TEST HELPERS	
	/////////////////////////////////
	
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
