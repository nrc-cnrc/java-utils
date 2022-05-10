package ca.nrc.dtrc.elasticsearch;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.net.URL;
import java.util.List;

public class ScoredHitsIterator_Scroll<T extends Document> extends ScoredHitsIterator<T> {
	private String scrollID;


	public ScoredHitsIterator_Scroll(List<Hit<T>> firstResultsBatch, String _scrollID, T _docPrototype, ESFactory _esFactory, HitFilter _filter) throws ElasticSearchException, SearchResultsException {
		super(firstResultsBatch, _docPrototype, _esFactory, _filter);
		init__ScoredHitsIterator_Scroll(_scrollID);
	}

	private void init__ScoredHitsIterator_Scroll(String _scrollID) {
		this.scrollID = _scrollID;
	}

	@Override
	protected List<Hit<T>> nextHitsPage() throws ElasticSearchException {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ScoredHitsIterator_Scroll.nextHitsPage");
		URL url = esFactory.urlBuilder().forEndPoint("_search/scroll").build();

		JSONObject postJson = new JSONObject()
			.put("scroll_id", scrollID)
			.put("scroll", "1m")
			;
		String jsonResponse = null;
		jsonResponse = esFactory.transport().post(url, postJson.toString());

		Pair<Pair<Long, String>, List<Hit<T>>> parsedResults = null;
		try {
			parsedResults = esFactory.respMapper.parseJsonSearchResponse(jsonResponse, docPrototype);
		} catch (ElasticSearchException e) {
			logger.error("scrollID="+scrollID+": parseJsonSearchResponse raised exception!");
			throw e;
		}

		List<Hit<T>> hits = parsedResults.getRight();
		logger.trace("Return total of "+hits.size()+" hits");
		return hits;
	}
}
