package ca.nrc.dtrc.elasticsearch;

import org.json.JSONObject;

import java.net.URL;
import java.util.List;

/**
 * This implementation of SearchResults uses the 'scroll' approach to paginate
 * search results.
 */
public class SearchResults_Scroll<T extends Document> extends SearchResults<T>{

	public SearchResults_Scroll(String jsonResponse, ESFactory _esFactory) throws ElasticSearchException {
		super(jsonResponse, (T)null, _esFactory);
		init__SearchResults_Scroll(jsonResponse);
	}


	public SearchResults_Scroll(String jsonResponse, T docPrototype,
 		ESFactory _esFactory) throws ElasticSearchException {
		super(jsonResponse, docPrototype, _esFactory);
		init__SearchResults_Scroll(jsonResponse);
	}

	public SearchResults_Scroll(
		String jsonResponse, T docPrototype, ESFactory _esFactory, String _indexName) throws ElasticSearchException {
		super(jsonResponse, docPrototype, _esFactory, _indexName);
		init__SearchResults_Scroll(jsonResponse);
	}

	public SearchResults_Scroll() throws ElasticSearchException {
		super();
		init__SearchResults_Scroll((String)null);
	}

	public SearchResults_Scroll(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
		init__SearchResults_Scroll((String)null);
	}

	public SearchResults_Scroll(List<Hit<T>> firstResultsBatch,
		String _scrollID, Long _totalHits, T _docPrototype, ESFactory _esFactory) throws ElasticSearchException {
		super(firstResultsBatch, _scrollID, _totalHits, _docPrototype, _esFactory);
		init__SearchResults_Scroll(_scrollID);
	}

	public SearchResults_Scroll(String jsonResponse, T _docPrototype,
		ESFactory _esFactory, URL url) throws ElasticSearchException {
		super(jsonResponse, _docPrototype, _esFactory, url);
		init__SearchResults_Scroll(jsonResponse);
	}

	private void init__SearchResults_Scroll(String responseOrScrollID) {
		scrollID = parseScrollID(responseOrScrollID);
	}

	private String parseScrollID(String responseOrScrollID) {
		String scrollID = responseOrScrollID;
		if (responseOrScrollID != null && responseOrScrollID.startsWith("{")) {
			JSONObject response = new JSONObject(responseOrScrollID);
			scrollID = response.getString("_scroll_id");
		}
		return scrollID;
	}

	@Override
	protected ScoredHitsIterator<T> hitsIterator() throws ElasticSearchException, SearchResultsException {
		ScoredHitsIterator<T> iter =
			new ScoredHitsIterator_Scroll<T>(
				scoredHitsBatch, scrollID, docPrototype,
				esFactory, filter);
		return iter;
	}

}
