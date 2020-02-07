package ca.nrc.data.harvesting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;

/**
 * Some search engines (Bing in particular) have trouble processing queries 
 * that have a long list of ORed terms.
 *
 * This class is designed to address that by issuing separate queries for
 * each term, and merging the results
 * 
 * @author desilets
 *
 */

public class SearchEngineMultiQuery  {
	
	SearchEngine engineProto = null;
	
	Map<String,SearchResults> termResults = new HashMap<String,SearchResults>();
	Set<String> foundURLs = new HashSet<String>();
	long estTotalHits = 0;
	
	public SearchEngineMultiQuery() throws IOException, SearchEngineException {
		initializeSearchEngineMultiQuery();
	}

	public SearchEngineMultiQuery(SearchEngine engineProto) throws IOException, SearchEngineException {
		initializeSearchEngineMultiQuery(engineProto);
	}

	private void initializeSearchEngineMultiQuery() throws IOException, SearchEngineException {
		initializeSearchEngineMultiQuery(null);
	}

	private void initializeSearchEngineMultiQuery(SearchEngine proto) throws IOException, SearchEngineException {
		if (proto == null) {
			proto = new BingSearchEngine();
		}
		engineProto = proto;
		termResults = new HashMap<String,SearchResults>();
		foundURLs = new HashSet<String>();
		estTotalHits = 0;
	}
	

	protected SearchResults search(Query query) throws SearchEngineException, IOException {
		
		initializeSearchEngineMultiQuery();
		
		
		// Create one worker per term
		int numWorkers = query.terms.size();
		SearchEngineWorker[] workers = new SearchEngineWorker[numWorkers];
		for (int ii=0; ii < numWorkers; ii++) {
			String aTerm = query.terms.get(ii);
			SearchEngineWorker aWorker = 
					new SearchEngineWorker(aTerm, query, "thr-"+ii+"-"+aTerm, engineProto);
			workers[ii] = aWorker;
			aWorker.start();
		}
		
		// Monitor the workers until they are all done
		while (true) {
			int stillRunning = 0;
			for (int ii=0; ii < numWorkers; ii++) {
				SearchEngineWorker currWorker = workers[ii];
				if (currWorker != null) {
					if (currWorker.stillWorking()) {
						stillRunning++;
					} else {
						// This worker just finished running. Integrate its
						// results in the total
						termResults.put(currWorker.query.terms.get(0), currWorker.results);
					}
				}				
			}
			// All workers have finished
			if (stillRunning == 0) break;
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// Nothing to do if the sleep is interrupted
			}
		}
		
		SearchResults mergedResults = mergeTermResults(query.maxHits);
		
		
		return mergedResults;
	}

	private SearchResults mergeTermResults(Integer maxHits) {
		SearchResults results = new SearchResults();
		
		Set<String> urlsSoFar = new HashSet<String>();
		
		List<List<Hit>> allHits = new ArrayList<List<Hit>>();
		
		boolean keepGoing = true;
		while (keepGoing) {
			int termsWithRemainingHits = 0;
			for (String aTerm: termResults.keySet()) {
				List<Hit> remainingHits = termResults.get(aTerm).retrievedHits;
				if (remainingHits.size() > 0) {
					termsWithRemainingHits++;
					Hit aHit = remainingHits.remove(0);
					results.retrievedHits.add(aHit);
				}
				if (results.retrievedHits.size() == maxHits) {
					keepGoing = false;
					break;
				}
			}
			if (termsWithRemainingHits == 0 
					|| results.retrievedHits.size() == maxHits) {
				keepGoing = false;
			}
		}
		
		results.estTotalHits = 0;
		for (String aTerm: termResults.keySet()) {
			results.estTotalHits += termResults.get(aTerm).estTotalHits;
		}
		
		return results;
	}
}
