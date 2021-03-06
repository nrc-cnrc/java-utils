package ca.nrc.data.harvesting;

import ca.nrc.data.harvesting.SearchEngine.Query;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SearchEngineMultiQueryTest {

	private String bingTestKey;

	@Before
	public void setUp() throws Exception {
		bingTestKey = SearchEngine_BingTest.assumeTestBingKeyIsDefined();
		return;
	}

	///////////////////////
	// DOCUMENTATION TESTS
	///////////////////////
	
	@Test(timeout = 10000)
	public void test__SearchEngineMultiQuery__Synopsis() throws Exception {
		// Some search engines (Bing in particular) have trouble processing queries 
		// that have a long list of ORed terms.
		//
		// This class is designed to address that by issuing separate queries for
		// each term, and merging the results
		//
			String[] terms = new String[]{"hello", "world"};
			Query query = new Query(terms);
			SearchResults results = new SearchEngineMultiQuery(bingTestKey).search(query);
	}

	///////////////////////
	// VERIFICATION TESTS
	///////////////////////
	
	@Test
	public void test__search__HappyPath() throws Exception {
		String[] terms = new String[] {"hello", "world"};
		Query query = new Query(terms).setMaxHits(50);
		SearchResults results =
				new SearchEngineMultiQuery(this.bingTestKey)
					.setCheckHitSummary(true)
					.search(query);
		
		int maxBadHits = 1;
		SearchEngineTest.assertResultsFitTheQuery(results, query, maxBadHits);
		
		Long expMinRetrieved = new Long(50);
		Long expMaxRetrieved = new Long(50);
		Long expMinTotalEstimate = new Long(Math.round(1e6));
		Long expMaxTotalEstimate = null;		
		SearchEngineTest.assertNumberHitsOK(results, expMinRetrieved, expMaxRetrieved, 
				expMinTotalEstimate, expMaxTotalEstimate);
	}

	@Test @Ignore
	public void test__search__TermThatProduceLessThanMaxHits() throws Exception {
		String [] terms = new String[] {"ᐅᖃᖅᑐᖅ"};
		Query query = new Query(terms).setMaxHits(50).setLang("iu");
		
		SearchResults results = 
					new SearchEngineMultiQuery(bingTestKey)
						.setCheckHitSummary(true)
						.search(query);
		
		int maxBadHits = 5;
		SearchEngineTest.assertResultsFitTheQuery(results, query, maxBadHits);
		
		Long expMinRetrieved = new Long(5);
		Long expMaxRetrieved = new Long(32);
		Long expMinTotalEstimate = new Long(5);
		Long expMaxTotalEstimate = new Long(30);
		
		SearchEngineTest.assertNumberHitsOK(results, expMinRetrieved, expMaxRetrieved, 
				expMinTotalEstimate, expMaxTotalEstimate);
	}
}
