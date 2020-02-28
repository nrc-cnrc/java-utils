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
		
		resultsCollector = new SearchResultsCollector();
		
		int numWorkers = query.terms.size();
		workers = new SearchEngineWorker[numWorkers];

						
		// Each worker searches for a single term.
		//		
		tLogger.trace("Creating several more workers, one per search term");
		for (int ii=0; ii < numWorkers; ii++) {
			String aTerm = query.terms.get(ii);
			SearchEngineWorker aWorker = 
					new SearchEngineWorker(aTerm, query, "thr-"+ii+"-"+aTerm, engineProto, resultsCollector);
			workers[ii] = aWorker;
			aWorker.start();
		}
		
		tLogger.trace("Started a total of "+workers.length+" search workers");
	}	

	private SearchResults mergeTermResults(Integer maxHits) throws SearchEngineException {		
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineMultQuery.mergeTermResults");

		tLogger.trace("invoked with maxHits="+maxHits);
		
		SearchResults results = new SearchResults();
		
		addHitsFromSingleTermWorkers(maxHits);
		tLogger.trace("After adding hits from the various workers that search for a single term, resultsCollector.hits.size()="+resultsCollector.hits.size());
		
		results.retrievedHits = resultsCollector.hits;
				
		results.estTotalHits = 0;
		if (resultsCollector.hits.size() < maxHits) {
			results.estTotalHits = resultsCollector.hits.size();
		} else {
			for (SearchEngineWorker aWorker: workers) {
				results.estTotalHits += aWorker.getEstTotalHits();
			}
		}
		
		return results;
	}

	private void addHitsFromSingleTermWorkers(Integer maxHits) throws SearchEngineException {
		Logger tLogger = Logger.getLogger("ca.nrc.data.harvesting.SearchEngineMultQuery.addHitsFromSingleTermWorkers");
		
		tLogger.trace("Adding hits for search for each of the workers");
		
		boolean keepGoing = true;
		//
		// Do a round-robbing loop through each of the workers, pulling
		// one hit from each worker at a time.
		//
		// This allows mixing of the hits from different terms instead of
		// having all the hits for the same term be consecutive.
		//
		while (keepGoing) {
			int termsWithRemainingHits = 0;
			for (SearchEngineWorker aWorker: workers) {
				
				Hit addedHit = addNextHitFromWorker(aWorker, maxHits);
				if (addedHit != SearchEngineWorker.NO_MORE_HITS) {
					termsWithRemainingHits++;
				}
				if (resultsCollector.hits.size() == maxHits) {
					keepGoing = false;
					break;
				}
			}
			if (termsWithRemainingHits == 0 
					|| resultsCollector.hits.size() == maxHits) {
				keepGoing = false;
			}
		}	
		
		for (SearchEngineWorker aWorker: workers) {
			aWorker.setStatus(SearchEngineWorker.Status.STOP);
		}
	}

	private Hit addNextHitFromWorker(SearchEngineWorker worker, Integer maxHits) throws SearchEngineException {
		Hit hit = worker.pullHit();
		if (hit != SearchEngineWorker.WAIT_FOR_MORE 
				&& hit != SearchEngineWorker.NO_MORE_HITS) {
			resultsCollector.addHit(hit);
		}
		
		return hit;
	}
}
