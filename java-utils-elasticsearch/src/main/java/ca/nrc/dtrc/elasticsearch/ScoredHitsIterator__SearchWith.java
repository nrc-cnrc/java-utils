package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.request.JsonString;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ScoredHitsIterator__SearchWith<T extends Document> extends ScoredHitsIterator<T> {

	private JSONArray searchAfterValue = null;
	private JSONObject request = null;
	private boolean noMoreHitsAvailable = false;

	public ScoredHitsIterator__SearchWith(
		JsonString _request, List<Hit<T>> firstResultsBatch, T _docPrototype,
		ESFactory _esFactory, HitFilter _filter) throws ElasticSearchException, SearchResultsException {
		super(firstResultsBatch, _docPrototype, _esFactory, _filter);
		init__ScoredHitsIterator__SearchWith(_request, firstResultsBatch);
		return;
	}

	private void init__ScoredHitsIterator__SearchWith(
		JsonString jsonRequest, List<Hit<T>> firstResultsBatch) {
		if (jsonRequest != null) {
			this.request = new JSONObject(jsonRequest.toString());
		}
		if (firstResultsBatch != null && !firstResultsBatch.isEmpty()) {
			Hit<T> lastHit = firstResultsBatch.get(firstResultsBatch.size()-1);
			searchAfterValue = lastHit.sortValues;
		}
		return;
	}

	@Override
	protected List<Hit<T>> nextHitsPage() throws ElasticSearchException {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ScoredHitsIterator_SearchAfter.nextHitsPage");
		logger.trace("searchAfterValue=" + searchAfterValue);

		List<Hit<T>> hits = new ArrayList<Hit<T>>();
		if (!noMoreHitsAvailable) {
			if (request != null) {
				URL url = esFactory.urlBuilder().forEndPoint("_search").build();

				request.put("search_after", searchAfterValue);
				String jsonResponse = null;
				jsonResponse = esFactory.transport().post(url, request.toString());

				Pair<Pair<Long, String>, List<Hit<T>>> parsedResults = null;
				try {
					parsedResults = esFactory.respMapper.parseJsonSearchResponse(jsonResponse, docPrototype);
				} catch (ElasticSearchException e) {
					logger.error("searchAfterValue=" + searchAfterValue + ": parseJsonSearchResponse raised exception!");
					throw e;
				}

				hits = parsedResults.getRight();
			}
			if (logger.isTraceEnabled()) {
				String mess = "Return total of " + hits.size() + " hits: [";
				for (Hit<T> aHit : hits) {
					mess += aHit.getDocument().getId() + ", ";
				}
				mess += "]";
				logger.trace(mess);
			}
		}

		if (hits == null || hits.isEmpty()) {
			noMoreHitsAvailable = true;
		}

		return hits;
	}

	@Override
	protected void filterDocumentsBatch() throws SearchResultsException {
		if (documentsBatch != null && !documentsBatch.isEmpty()) {
			Hit<T> lastHit = documentsBatch.get(documentsBatch.size()-1);
			searchAfterValue = lastHit.sortValues;
		}
		return;
	}


//	@Override
//	public Hit<T> next() {
//		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ScoredHitsIterator.next");
//		Hit<T> nextHit = super.next();
//		searchAfterValue = nextHit.sortValues;
//		return nextHit;
//	}

}
