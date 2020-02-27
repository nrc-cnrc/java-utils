package ca.nrc.data.harvesting;

import java.util.HashMap;
import java.util.Map;

/** 
 * 
 * Collect search results found by different SearchEngineWorkers
 * 
 * @author desilets
 *
 */
public class SearchResultsCollector {
	
	private Map<SearchEngineWorker, SearchResults> workerResults = new HashMap<SearchEngineWorker, SearchResults>();

	public synchronized void addResultsForWorker(SearchEngineWorker worker, SearchResults results) {
		workerResults.put(worker, results);
	}

	public synchronized SearchResults getResultsForWorker(SearchEngineWorker worker) {
		SearchResults results = null;
		if (workerResults.containsKey(worker)) {
			results = workerResults.get(worker);
		}
		return results;
	}

	public synchronized boolean workerProducedResults(SearchEngineWorker worker) {
		boolean answer = false;
		if (workerResults.containsKey(worker)) {
			answer = true;
		}
		return answer;
	}
}
