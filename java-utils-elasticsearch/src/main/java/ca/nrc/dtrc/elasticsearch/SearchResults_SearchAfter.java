package ca.nrc.dtrc.elasticsearch;


import org.json.JSONObject;

import java.net.URL;

/**
 * This implementation of SearchResults uses the 'search after' approach to paginate
 * search results.
 */
public class SearchResults_SearchAfter<T extends Document> extends SearchResults<T>{
	protected JSONObject request;

	public SearchResults_SearchAfter(JSONObject _request, String jsonResponse, T _docPrototype, ESFactory _esFactory, URL url) throws ElasticSearchException {
		super(jsonResponse, _docPrototype, _esFactory, url);
		init__SearchResults_SearchAfter(_request);
	}

	private void init__SearchResults_SearchAfter(JSONObject _request) {
		this.request = _request;
	}

	@Override
	protected ScoredHitsIterator<T> hitsIterator() throws ElasticSearchException, SearchResultsException {
		ScoredHitsIterator<T> iter =
			new ScoredHitsIterator__SearchWith<T>(
				request, scoredHitsBatch, docPrototype,
				esFactory, filter);
		return iter;
	}
}