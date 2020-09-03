package ca.nrc.data.harvesting;

import ca.nrc.data.harvesting.SearchEngine.Query;
import org.junit.Test;

public class SearchEngineMultiQueryTest {

	///////////////////////
	// DOCUMENTATION TESTS
	///////////////////////
	
	@Test
	public void test__SearchEngineMultiQuery__Synopsis() throws Exception {
		// Some search engines (Bing in particular) have trouble processing queries 
		// that have a long list of ORed terms.
		//
		// This class is designed to address that by issuing separate queries for
		// each term, and merging the results
		//
		String[] terms = new String[] {"hello", "world"};
		Query query = new Query(terms);
		SearchResults results = new SearchEngineMultiQuery().search(query);
	}

	///////////////////////
	// VERIFICATION TESTS
	///////////////////////
	
	@Test
	public void test__search__HappyPath() throws Exception {
		String[] terms = new String[] {"hello", "world"};
		Query query = new Query(terms).setMaxHits(50);
		SearchResults results = 
				new SearchEngineMultiQuery()
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
	
	@Test
	public void test__search__InuktitutWords() throws Exception {
		//
		// Inkutut is one language where we have a lot of problems when
		// ORing a bunch of words.
		//
		// So we make sure to test that it works with the MultiQuery 
		// engine
		//
		String [] terms = new String[] {
				"ᓄᓇᕗ", "ᓄᓇᕗᒻᒥ", "ᓄᓇᕘᒥ", "ᓄᓇᕘᑉ", "ᓄᓇᕗᒻᒥᐅᑦ", "ᓄᓇᕗᑦ"};
		Query query = new Query(terms).setMaxHits(50).setLang("iu");
		SearchResults results = 
				new SearchEngineMultiQuery()
					.setCheckHitSummary(true)
					.search(query);
		
		int maxBadHits = 4;
		SearchEngineTest.assertResultsFitTheQuery(results, query, maxBadHits);
		
		Long expMinRetrieved = new Long(50);
		Long expMaxRetrieved = new Long(50);
		Long expMinTotalEstimate = new Long(1000);
		Long expMaxTotalEstimate = null;		
		SearchEngineTest.assertNumberHitsOK(results, expMinRetrieved, expMaxRetrieved, 
				expMinTotalEstimate, expMaxTotalEstimate);
		
		SearchEngineTest.assertSufficientHitsFound(Math.round(100), results);
	}
	
	@Test
	public void test__search__TermThatProduceLessThanMaxHits() throws Exception {
		String [] terms = new String[] {"ᐅᖃᖅᑐᖅ"};
		Query query = new Query(terms).setMaxHits(50).setLang("iu");
		
		SearchResults results = 
					new SearchEngineMultiQuery()
						.setCheckHitSummary(true)
						.search(query);
		
		int maxBadHits = 5;
		SearchEngineTest.assertResultsFitTheQuery(results, query, maxBadHits);
		
		Long expMinRetrieved = new Long(5);
		Long expMaxRetrieved = new Long(30);
		Long expMinTotalEstimate = new Long(5);
		Long expMaxTotalEstimate = new Long(30);
		
		SearchEngineTest.assertNumberHitsOK(results, expMinRetrieved, expMaxRetrieved, 
				expMinTotalEstimate, expMaxTotalEstimate);
	}
}
