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
	
	SearchResultsCollector resultsCollector = null;
	
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
		foundURLs = new HashSet<String>();
		estTotalHits = 0;
	}
	

	public SearchResults search(Query query) throws SearchEngineException, IOException {
		
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineMultQuery.search");

		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Invoked with query="+PrettyPrinter.print(query));
		}
		
		
		initializeSearchEngineMultiQuery();

		// Create one worker per term then wait for them to
		// finish.
		createAndStartWorkers(query);
				
		SearchResults mergedResults = mergeTermResults(query.maxHits);
		
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Returning mergedResults=\n"+PrettyPrinter.print(mergedResults));
		}
		
		return mergedResults;
	}

	/**
	 * Creates one worker for each term in the query.
	 * @param query
	 * @throws SearchEngineException 
	 */
	
	private void createAndStartWorkers(Query query) throws SearchEngineException {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineMultQuery.createAndStartWorkers");
		tLogger.trace("invoked");
		
		int numWorkers = query.terms.size()+1;
		workers = new SearchEngineWorker[numWorkers];

				
		// First worker should search for all the terms at once in a 
		// 'fuzzy search' manner.
		tLogger.trace("Creating first worker which searches for all terms at once");
		{
			String queryStr = String.join(" ", query.terms);
			workers[0] = 
					new SearchEngineWorker(queryStr, query, "thr-0-allterms", engineProto);
		}
		
		// Remaining workers each search for a single term.
		//		
		tLogger.trace("Creating several more workers, one per search term");
		for (int ii=1; ii < numWorkers; ii++) {
			String aTerm = query.terms.get(ii-1);
			SearchEngineWorker aWorker = 
					new SearchEngineWorker(aTerm, query, "thr-"+ii+"-"+aTerm, engineProto);
			workers[ii] = aWorker;
		}
		
		resultsCollector = new SearchResultsCollector();
		for (SearchEngineWorker aWorker: workers) {
			aWorker.setCollector(resultsCollector);
			aWorker.start();
		}
		
		tLogger.trace("Started a total of "+workers.length+" search workers");
		waitForFirstBatchesOfHits();
		
		tLogger.trace("All workers have produced their first batch of hits.");
	}
	
	/**
	 * Monitor the term workers until they all have produced a first batch of results.
	 */

	private void waitForFirstBatchesOfHits() {
		int numWorkers = workers.length;
		while (true) {
			int stillRunning = 0;
			for (int ii=0; ii < numWorkers; ii++) {
				SearchEngineWorker currWorker = workers[ii];
				if (currWorker != null) {
					if (currWorker.stillWorking()) {
						stillRunning++;
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
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineMultQuery.mergeTermResults");

		tLogger.trace("invoked with maxHits="+maxHits);
		
		SearchResults results = new SearchResults();
		
		List<Hit> mergedHits = addHitsFromSingleTermWorkers(maxHits);
		tLogger.trace("After adding hits from the various workers that search for a single term, #mergedHits="+mergedHits.size());
		
		results.retrievedHits = mergedHits;
				
		results.estTotalHits = 0;
		if (mergedHits.size() < maxHits) {
			results.estTotalHits = maxHits;
		} else {
			for (SearchEngineWorker aWorker: workers) {
				results.estTotalHits += resultsCollector.getResultsForWorker(aWorker).estTotalHits;
			}
		}
		
		return results;
	}

	private List<Hit> hitsFromAlltermWorker(int maxHits) {
		List<Hit> hits = new ArrayList<Hit>();
		boolean hitAdded = true;
		while (hitAdded) {
			hitAdded = addNextHitFromWorker(workers[0], hits, maxHits);
		}
		
		return hits;
	}

	private List<Hit> addHitsFromSingleTermWorkers(Integer maxHits) {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineMultQuery.addHitsFromSingleTermWorkers");
		
		tLogger.trace("Adding hits for search for each of the workers");
		
		List<Hit> mergedHits = new ArrayList<Hit>();		

		boolean keepGoing = true;
		// Do a round-robbing loop through each of the workers, pulling
		// one hit from each worker at a time.
		//
		// This allows mixing of the hits from different terms instead of
		// having all the hits for the same term be consecutive.
		//
		while (keepGoing) {
			int termsWithRemainingHits = 0;
			for (SearchEngineWorker aWorker: workers) {
				
				boolean foundHits = addNextHitFromWorker(aWorker, mergedHits, maxHits);
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

	private boolean addNextHitFromWorker(SearchEngineWorker worker, List<Hit> mergedHits, Integer maxHits) {
		boolean hitsFound = false;
		List<Hit> remainingHits = resultsCollector.getResultsForWorker(worker).retrievedHits;
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
