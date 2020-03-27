package ca.nrc.data.harvesting;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ca.nrc.data.harvesting.SearchEngine;
import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;
import ca.nrc.data.harvesting.SearchEngine.Type;
import ca.nrc.json.PrettyPrinter;
import ca.nrc.testing.AssertHelpers;
import ca.nrc.testing.AssertNumber;

public abstract class SearchEngineTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	protected abstract SearchEngine makeSearchEngine() throws IOException, SearchEngineException;

	/*************************
	 * DOCUMENTATION TESTS
	 * @throws IOException 
	 *************************/
	
	@SuppressWarnings("unused")
	@Test
	public void test__SearchEngine__Synopsis() throws SearchEngine.SearchEngineException, IOException {
		
		// Create the search engine.
		// Note: SearchEngine is an abstract class, so tests use a
		//    use a factory method to generaate a concrete subclass of it.
		SearchEngine engine = makeSearchEngine();
		
		// 
		// The "default" way to search, is to search the whole web for
		// a given FUZZY QUERY.
		//
		// Different search engine implementations (ex: Bing, Google) will interpret
		// this fuzzy query in their own way, but typicaly, they look for documents
		// that contain:
		// - as many of the query words as possible
		// - in as close a proximity as possibl
		// - respecting the order of the words in the query as much as possible
		//
		SearchEngine.Query query = new SearchEngine.Query("how many ways to skin a cat?");
		List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 
		
		// You can then peruse the list of hits as follows.
		for (SearchEngine.Hit aHit: results) {
			URL url = aHit.url; // URL of the hit
			String title = aHit.title; 
			// This corresponds to the couple of lines that the search 
			// engine displays for each hit.
			String summary = aHit.summary;
		}
		
		
		// 
		// A more controlled way to search is to specify a list of TERMS.
		// Terms can be single words, or phrases.
		//
		// The system will return documents that contain at least one of the 
		// terms as-is.
		//
		// For example, the query below will find documents that contain at least 
		// one of the exact phrases "machine learning" or "pattern recognition"
		// 
		query = new SearchEngine.Query(new String[] {"machine learning", "pattern recognition"});

		// 
		// You can change the default settings of the search by 
		// changing settings of the Query object.
		//
		// For example, this query will search up to 20 hits
		// and restrict results to be News pages
		//
		query = new SearchEngine.Query("donald trump")
						.setMaxHits(20)
						.setType(SearchEngine.Type.NEWS);	
		
		// And this query will search up to 20 hits
		// and restrict results to be News AND Blog pages
		//
		query = new SearchEngine.Query("donald trump")
						.setMaxHits(20)
						.setTypes(new SearchEngine.Type[] {SearchEngine.Type.NEWS, SearchEngine.Type.BLOG});	
		
		// This one will search for pages on site: 'www.nrc-cnrc.gc.ca'
		query = new SearchEngine.Query("donald trump")
				.setMaxHits(20)
				.setSite("www.nrc-cnrc.gc.ca");	
		
		// When displaying search results in a web UI, it is typical to request one
		// "page" of hits at a time. You can do this as follows.
		//
		// Say you want to have 10 hits per page...
		//
		int hitsPerPage = 10;
		query = new SearchEngine.Query("donald trump")
					.setHitsPerPage(hitsPerPage);
		
		// Get the first page of hits...
		results = engine.search(query.setHitsPageNum(0)).retrievedHits; 
		
		// Get the second page, and so on...
		results = engine.search(query.setHitsPageNum(1)).retrievedHits; 
	}

	/*************************
	 * VERIFICATION TESTS
	 *************************/
	
	@Test
	public void test__search__HappyPath() throws Exception {
		SearchEngine engine = makeSearchEngine();
		
		SearchEngine.Query query = new SearchEngine.Query("learn");
		List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 
		Assert.assertTrue("Found more hits than requested", results.size() <= query.maxHits);
		assertResultsFitTheQuery(results, query);
	}


	@Test
	public void test__search__MultipleTerms() throws Exception {
		SearchEngine engine = makeSearchEngine();
		
		String[] terms = new String[] {"machine learning", "pattern recognition"};
		SearchEngine.Query query = new SearchEngine.Query(terms).setMaxHits(10);
		List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 
		Assert.assertEquals("max Hits", query.maxHits, new Integer(results.size()));
		assertResultsFitTheQuery(results, query, 3);
	}

	@Test
	public void test__search__OverrideDefaultMaxHits__ShouldReturnCorrectNumberOfHits() throws Exception {

		SearchEngine engine = makeSearchEngine();
		
		SearchEngine.Query query = new SearchEngine.Query("wikipedia");
		
		
		// Set the max number of hits to twice the default value;
		int origMaxHits =query.maxHits;
		query.setMaxHits(query.maxHits*2 + 5);
		List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 		
		
		Assert.assertTrue("max Hits", results.size() > origMaxHits);
		
		// Note: We allow at most one of the hits to not match the query
		//   (this is something that can happen, where we can't tell
		//   from the title, url and summary of the hit that it actually
		//   fits the query)
		int maxNonMatching = 3;
		assertResultsFitTheQuery(results, query, maxNonMatching);
	}
	
	@Test
	public void test__search__maxHitsGreaterThanWhatEngineShowsOnFirstPageOfHits() throws Exception {
		
		Integer maxHitsPerPage = this.engineMaxHitsPerPage();
		if (maxHitsPerPage != null) {
			// This search engine requires that you download one page of hits at
			// a time, where the max hits per page is maxHitsPerPage.
			//
			// Make sure that things work OK when we ask more hits than what
			// can fit on the first page.
			//
			
			// Set the max number of hits to twice the default value;			
			int numHits = 2*maxHitsPerPage + 3;
			
			SearchEngine engine = makeSearchEngine();
			SearchEngine.Query query = 
					new SearchEngine.Query("wikipedia")
					.setMaxHits(numHits)
					;		
			
			List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 
			Assert.assertEquals("Number of hits was not as expected",
					numHits, results.size());
			
		}
	}
		
	
	protected abstract Integer engineMaxHitsPerPage();

	@Test
	public void test__search__TypeNEWS() throws Exception {
		SearchEngine engine = makeSearchEngine();
		
		SearchEngine.Type hitType = SearchEngine.Type.NEWS;
		SearchEngine.Query query = 
				new SearchEngine.Query("machine learning").setType(hitType);
		List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 
		assertResultsFitTheQuery(results, query);
	}
	
	@Test
	public void test__search__Site() throws Exception {
		SearchEngine engine = makeSearchEngine();
		
		String site = "nrc-cnrc.gc.ca";
		SearchEngine.Query query = 
				new SearchEngine.Query("machine learning").setSite(site);
		List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 
		assertResultsFitTheQuery(results, query, 3);
	}	

	
	@Test
	public void test__search__RestrictToSite() throws Exception {
		SearchEngine engine = makeSearchEngine();
		
		String site = "nrc-cnrc.gc.ca";
		SearchEngine.Query query = new SearchEngine.Query("machine learning");
		query.setSite(site);
		List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 
		assertResultsFitTheQuery(results, query, 3);
	}
	
	@Test
	public void test__search__PageByPage() throws Exception {
		SearchEngine engine = makeSearchEngine();
		
		SearchEngine.Query query = new SearchEngine.Query("learn");
		
		List<SearchEngine.Hit> gotPage1 = engine.search(query.setHitsPageNum(0)).retrievedHits; 
		List<SearchEngine.Hit> gotPage2 = engine.search(query.setHitsPageNum(1)).retrievedHits; 
		AssertHelpers.assertDeepNotEqual("First page of hits should have been different from the second one.", gotPage1, gotPage2);
	}

	@Test(timeout=10000)
	public void test__search__InuktutLanguage() throws Exception {
		// Most search engines (well, at least Bing) tend to return 
		// non-Inuktut pages, even when you ask specifically for that language
		//
		// So for this test, activate the post-processing language filter
		//
		SearchEngine engine = makeSearchEngine().setCheckHitLanguage(true);
		
		// Search for the word 'nunavut'
		SearchEngine.Query query = new SearchEngine.Query("ᓄᓇᕗ");
		List<SearchEngine.Hit> results = engine.search(query).retrievedHits; 
		assertResultsFitTheQuery(results, query, 3);
	}
	
	/*************************
	 * TEST HELPER METHODS
	 * @throws PageHarvesterException 
	 *************************/	

	public static void assertResultsFitTheQuery(List<SearchEngine.Hit> results, SearchEngine.Query query) throws PageHarvesterException {
		assertResultsFitTheQuery(results, query, 0);
	}

	public static void assertResultsFitTheQuery(SearchResults results, SearchEngine.Query query, int maxNoFit) throws PageHarvesterException {
		assertResultsFitTheQuery(results.retrievedHits, query, maxNoFit);
	}
	
	public static void assertResultsFitTheQuery(List<SearchEngine.Hit> results, SearchEngine.Query query, int maxNoFit) throws PageHarvesterException {
		
		Map<Hit, String> hitValidity = new HashMap<Hit,String>();
		for (SearchEngine.Hit hit: results) {
			hitValidity.put(hit, "OK");
		}
		String resultsJson = PrettyPrinter.print(results);
		
		// Checking max number of hits
		int expNumHits = query.maxHits;
		int gotNumHits = results.size();
		
		AssertNumber.isLessOrEqualTo("Too many results were produced", 
				new Long(gotNumHits), new Long(expNumHits));
		
		// Checking if hits are from the correct site
		String expSite = query.getSite();
		if (expSite != null) {
			for (SearchEngine.Hit hit: results) {
				String gotHost = hit.url.getHost();
				if (!gotHost.contains(expSite)) {
					hitValidity.put(hit, "Is on wrong web site");
				}
			}
		}

		// Checking if hits match the query words and have the correct type
		for (SearchEngine.Hit hit: results) {
			if (! hitValidity.get(hit).equals("OK")) {continue;}
			
			if (hit.url.toString().matches("[\\s\\S]*\\.pdf$")) {
				hitValidity.put(hit, "Skipped");
				continue;
			}
			
			if (!hitMatchesQueryKeywords(query, hit)) {
				hitValidity.put(hit, "Did not match the keywords");
				continue;
			}
			
			if (!hitHasCorrectType(query, hit)) {
				hitValidity.put(hit, "Content does not have the correct type");
				continue;
			}
		}

		int numBadHits = 0;
		int totalHits = 0;
		List<String> badHits = new ArrayList<String>();
		for (Hit aHit: hitValidity.keySet()) {
			String validity = hitValidity.get(aHit);
			URL url = aHit.url;
			if (validity.equals("Skipped")) {continue;}
			totalHits++;
			if (! validity.equals("OK")) {
				numBadHits++;
				badHits.add("\n   Problem: "+hitValidity.get(aHit)+"\n   Summary: "+aHit.summary);;
			}
		}
		
		if (numBadHits > maxNoFit) {
			String[] badHitsArray = badHits.toArray(new String[numBadHits]);
			String badHitsDesc = String.join("\n\n", badHitsArray);
			fail(
				"There were too many hits ("+numBadHits+" > "+maxNoFit+" out of "+totalHits+") that did not fit the query.\n"+ 
				"Query was:\n============\n" + PrettyPrinter.print(query) + "============\n" +
				"Here are the non-matching hits:\n\n" + badHitsDesc
				);
		}
	}
	
	public static Boolean hitMatchesQueryKeywords(SearchEngine.Query query, SearchEngine.Hit hit) throws PageHarvesterException {
		String wholeContent = hit.toString();
		Boolean matches = null;
		if (wholeContent.contains("moved")) {
			// Don't check keywords if the page was moved to a different location
			matches = true;
		} else {
			if (query.fuzzyQuery != null) {
				if (wholeContent != null) {
					matches = hitMatchesContent_FuzzyQuery(query.fuzzyQuery, wholeContent);
					if (! matches) {
						try {
							String actualContent = getHitActualContent(hit);
							if (actualContent != null) {
								matches = hitMatchesContent_FuzzyQuery(query.fuzzyQuery, actualContent);
							}
						} catch (PageHarvesterException e) {
							// If summary of hit does not match and we are not able
							// to fetch the page's full content, assume no match
							matches = false;
						}
					}
				}
			} else {
				matches = hitMatchesContent_TermsList(query.terms, wholeContent);
				if (! matches) {
					try {
						String actualContent = getHitActualContent(hit);
						matches = hitMatchesContent_TermsList(query.terms, actualContent);
					} catch (PageHarvesterException e) {
						// If summary of hit does not match and we are not able
						// to fetch the page's full content, assume no match
						matches = false;
					}
				}
			}
		}
		return matches;
	}
	
	private static String getHitActualContent(SearchEngine.Hit hit) throws PageHarvesterException {
		String content = new PageHarvester_HtmlCleaner().harvestSinglePage(hit.url);
		
		return content;
	}

	public static Boolean hitMatchesContent_TermsList(List<String> terms, String wholeContent) {
		Boolean foundTerm = false;
		for (String aTerm: terms) { 
			if (wholeContent != null && wholeContent.contains(aTerm.toLowerCase())) {
				foundTerm = true;
				break;
			}
		}
		return foundTerm;
	}

	public static Boolean hitMatchesContent_FuzzyQuery(String fuzzyQuery, String wholeContent) {
		Boolean foundWord = false;
		String[] words = fuzzyQuery.split("\\s+");
		for (String aWord: words) {
			if (wholeContent.contains(aWord.toLowerCase())) {
				foundWord = true;
				break;
			}
		}

			return foundWord;
	}
	
	private static boolean hitHasCorrectType(Query query, SearchEngine.Hit hit) {
		boolean hasCorrectType = true;
		
		String wholeContent = hit.toString();
		SearchEngine.Type[] types = query.types;
		
		boolean foundType = false;
		for (SearchEngine.Type aType: types) {
			if (aType == SearchEngine.Type.ANY) {
				foundType = true;
				break;
			} else {
				String aTypeAsString = aType.toString().toLowerCase();
				if (wholeContent.contains(aTypeAsString)) {
					foundType= true;
					break;
				}
			}
		}
		
		if (foundType) hasCorrectType = true;
		
		return hasCorrectType;
	}
	
	public static void assertSufficientHitsFound(long expMin, SearchResults results) {
		Assert.assertTrue("Total estimated hits should have been at least "+expMin
				+", but it was only"+results.estTotalHits, 
				results.estTotalHits >= expMin);
	}

	public static void assertNumberHitsOK(SearchResults results, Long expMinRetrieved, Long expMaxRetrieved,
			Long expMinTotalEstimate, Long expMaxTotalEstimate) {
		
		int gotRetrieved = results.retrievedHits.size();
		if (expMinRetrieved != null) {
			AssertNumber.isGreaterOrEqualTo("Number of retrieved hits was too low.", 
					new Long(gotRetrieved), expMinRetrieved);
		}
		if (expMaxRetrieved != null) {
			AssertNumber.isLessOrEqualTo("Number of retrieved hits was too high.", 
					new Long(gotRetrieved), expMaxRetrieved);
		}
		
		Long gotEstimate = new Long(results.estTotalHits);
		if (expMinTotalEstimate != null) {
			AssertNumber.isGreaterOrEqualTo("Estimate for total #hits was too low.", 
					gotEstimate, expMinTotalEstimate);
		}
		if (expMaxTotalEstimate != null) {
			AssertNumber.isLessOrEqualTo("Estimate for total #hits was too high.", 
					gotEstimate, expMaxTotalEstimate);
		}
	}
}
