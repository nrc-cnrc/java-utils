package ca.nrc.data.harvesting;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.nrc.data.harvesting.SearchEngine.Query;

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
		SearchResults results = new SearchEngineMultiQuery().search(query);
		SearchEngineTest.assertResultsFitTheQuery(results, query, 3);
		SearchEngineTest.assertSufficientHitsFound(Math.round(1e6), results);
	}
}
