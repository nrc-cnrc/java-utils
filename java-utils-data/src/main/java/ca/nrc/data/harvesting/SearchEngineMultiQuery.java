package ca.nrc.data.harvesting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.nrc.data.harvesting.SearchEngine.Hit;
import ca.nrc.data.harvesting.SearchEngine.Query;
import ca.nrc.data.harvesting.SearchEngine.SearchEngineException;
import ca.nrc.json.PrettyPrinter;

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
	
	SearchEngineWorker[] workers = new SearchEngineWorker[0];
	
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
	

	public SearchResults search(Query query) throws SearchEngineException, IOException {
		
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineMultQuery.search");

		tLogger.trace("Invoked with query="+PrettyPrinter.print(query));
		
		initializeSearchEngineMultiQuery();
		
		// Create one worker per term then wait for them to
		// finish.
		createAndStartWorkers(query);
		waitForWorkersToComplete();
		
		SearchResults mergedResults = mergeTermResults(query.maxHits);
		
		return mergedResults;
	}

	/**
	 * Creates one worker for each term in the query.
	 * @param query
	 * @throws SearchEngineException 
	 */
	
	private void createAndStartWorkers(Query query) throws SearchEngineException {
		int numWorkers = query.terms.size()+1;
		workers = new SearchEngineWorker[numWorkers];

				
		// First worker should search for all the terms at once in a 
		// 'fuzzy search' manner.
		{
			String queryStr = String.join(" ", query.terms);
			workers[0] = 
					new SearchEngineWorker(queryStr, query, "thr-0-allterms", engineProto);
		}
		
		// Remaining workers each search for a single term.
		//		
		for (int ii=1; ii < numWorkers; ii++) {
			String aTerm = query.terms.get(ii-1);
			SearchEngineWorker aWorker = 
					new SearchEngineWorker(aTerm, query, "thr-"+ii+"-"+aTerm, engineProto);
			workers[ii] = aWorker;
		}
		
		for (SearchEngineWorker aWorker: workers) {
			aWorker.start();
		}
	}
	
	/**
	 * Monitor the term workers until they are done.
	 */
	private void waitForWorkersToComplete() {
		int numWorkers = workers.length;
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
	}
	

	private SearchResults mergeTermResults(Integer maxHits) {
		SearchResults results = new SearchResults();
		
		Set<String> urlsSoFar = new HashSet<String>();
		
		List<List<Hit>> allHits = new ArrayList<List<Hit>>();
		
		
		List<Hit> mergedHits = hitsFromAlltermWorker(maxHits);
		mergedHits = addHitsFromSingleTermWorkers(mergedHits, maxHits);
		
		results.retrievedHits = mergedHits;
				
		results.estTotalHits = 0;
		for (String aTerm: termResults.keySet()) {
			results.estTotalHits += termResults.get(aTerm).estTotalHits;
		}
		
		return results;
	}

	private List<Hit> hitsFromAlltermWorker(int maxHits) {
		List<Hit> hits = new ArrayList<Hit>();
		boolean hitAdded = true;
		String queryStr = String.join(" ", workers[0].query.terms);
		while (hitAdded) {
			hitAdded = addNextHitFromWorker(queryStr, hits, maxHits);
		}
		
		return hits;
	}

	private List<Hit> addHitsFromSingleTermWorkers(List<Hit> mergedHits, Integer maxHits) {
		boolean keepGoing = true;
		// Do a round-robbing loop through each of the workers, pulling
		// one hit from each worker at a time.
		//
		// This allows mixing of the hits from different terms instead of
		// having all the hits for the same term be consecutive.
		//
		while (keepGoing) {
			int termsWithRemainingHits = 0;
			for (String aTerm: termResults.keySet()) {
				
				boolean foundHits = addNextHitFromWorker(aTerm, mergedHits, maxHits);
				if (foundHits) {
					termsWithRemainingHits++;
				}
				if (mergedHits.size() == maxHits) {
					keepGoing = false;
					break;
				}
			}
			if (termsWithRemainingHits == 0 
					|| mergedHits.size() == maxHits) {
				keepGoing = false;
			}
		}
		
		return mergedHits;
	}

	private boolean addNextHitFromWorker(String aTerm, List<Hit> mergedHits, Integer maxHits) {
		boolean hitsFound = false;
		List<Hit> remainingHits = termResults.get(aTerm).retrievedHits;
		if (remainingHits.size() > 0) {
			Hit aHit = remainingHits.remove(0);
			String aHitUrl = aHit.url.toString();
			if (!foundURLs.contains(aHitUrl)) {
				foundURLs.add(aHitUrl);
				mergedHits.add(aHit);
				hitsFound = true;
			}
		}
		
		return hitsFound;
	}
}
