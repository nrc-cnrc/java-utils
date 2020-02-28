package ca.nrc.data.harvesting;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.nrc.data.harvesting.SearchEngine.Hit;

/** 
 * 
 * Collect search results found by different SearchEngineWorkers
 * 
 * @author desilets
 *
 */
public class SearchResultsCollector {
	
	public List<Hit> hits = new ArrayList<Hit>();
	Set<URL> alreadySeen = new HashSet<URL>();

	public void addHit(Hit hit) {
		if (hit != SearchEngineWorker.NO_MORE_HITS && 
				hit != SearchEngineWorker.WAIT_FOR_MORE &&
				hit != null) {
			URL url = hit.url;
			if (!alreadySeen.contains(url)) {
				hits.add(hit);
				alreadySeen.add(url);
			}
		}
	}
}
