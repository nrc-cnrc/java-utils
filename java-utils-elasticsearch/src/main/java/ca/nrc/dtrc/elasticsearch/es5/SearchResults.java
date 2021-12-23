package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.json.PrettyPrinter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.*;

public class SearchResults<T extends Document> implements Iterable<Hit<T>> {
	
	final static Logger logger = Logger.getLogger(SearchResults.class);
	
	private T docPrototype = null;

	private String indexName = "UNKNOWN";

	private String scrollID = null;

	private JSONObject aggregations = new JSONObject();
	
	protected HitFilter filter = new HitFilter();
	
	private Long totalHits = new Long(0);
	private URL searchURL = null;

	private ResponseMapper respMapper = new ResponseMapper(indexName);

	public SearchResults(String jsonResponse, T docPrototype,
		StreamlinedClient _esClient) throws ElasticSearchException {
		init__SearchResults(jsonResponse, docPrototype, _esClient, (String)null);
	}

	public SearchResults(String jsonResponse, T docPrototype,
		StreamlinedClient _esClient, String _indexName)
		throws ElasticSearchException {
		init__SearchResults(jsonResponse, docPrototype, _esClient, _indexName);
	}


	public void init__SearchResults (String jsonResponse, T docPrototype,
		StreamlinedClient _esClient, String _indexName)
		throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.es5.SearchResults.init__SearchResults");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Constructing results of type "+docPrototype.getClass().getName()+" from jsonResponse="+jsonResponse);
		}
		if (_indexName != null) {
			indexName = _indexName;
		}
		esClient = _esClient;
	}

	public Long getTotalHits() {return totalHits;}
		public void setTotalHits(Long _totalHits) {this.totalHits = _totalHits;}
		
	private  List<Hit<T>> scoredHitsBatch = new ArrayList<>();	
		@JsonIgnore
		public List<Hit<T>> getScoredHitsBatch() {return scoredHitsBatch;}
		
	private int batchCursor = 0;
		
	private Double maxScore = 0.0;
		public Double getMaxScore() {return maxScore;}
		public void setMaxScore(Double _maxScore) {
			this.maxScore = _maxScore;
		}
	
	private List<Hit<T>> topHits = new ArrayList<>();
		public List<Hit<T>> getTopScoredDocuments() {
			return topHits;
		}
		public void addHit(T doc, Double score, JSONObject snippets) {
			scoredHitsBatch.add(new Hit<T>(doc,  score, snippets));
		}
		
	// Client that was used to retrieve the results.
	// The SearchResults class needs it to be able to scroll through
	// the list of hits one batch at a time.
	@JsonIgnore
	StreamlinedClient esClient = null;

	public SearchResults() throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.es5.SearchResults");
		tLogger.trace("Empty constructor");
		init_Searchresults((String)null, (T)null, (StreamlinedClient)null,
			(URL)null);
	}

	public SearchResults(List<Hit<T>> firstResultsBatch, String _scrollID,
		Long _totalHits, T _docPrototype, StreamlinedClient _esClient) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.es5.SearchResults");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Constructing results from _scrollID="+_scrollID+" and \n   firstResultsBatch="+PrettyPrinter.print(firstResultsBatch));
		}

		init_Searchresults(firstResultsBatch, _scrollID, _totalHits,
			(JSONObject)null, _docPrototype, _esClient, (URL)null);
	}	
	
	public SearchResults(String jsonResponse, T _docPrototype,
		StreamlinedClient _esClient, URL url) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.es5.SearchResults");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Constructing results from jsonResponse="+jsonResponse);
		}
		init_Searchresults(jsonResponse, _docPrototype, _esClient, url);
	}

	private Triple<Pair<Long, String>, List<Hit<T>>, JSONObject>
		parseJsonSearchResponse(String jsonSearchResponse, T docPrototype) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.es5.SearchResults.parseJsonSearchResponse");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("invoked with docPrototype="+docPrototype.getClass().getName()+", jsonSearchResponse="+jsonSearchResponse);
		}
		List<Hit<T>> scoredDocuments = new ArrayList<>();
		JSONObject aggregations = new JSONObject();
		String scrollID = null;
		ObjectMapper mapper = new ObjectMapper();
		JSONObject jsonRespNode;
		Long totalHits;
		try {
			jsonRespNode = new JSONObject(jsonSearchResponse);
			if (jsonRespNode.has("_scroll_id")) {
				scrollID = jsonRespNode.getString("_scroll_id");
			}
			if (jsonRespNode.has("aggregations")) {
				aggregations = jsonRespNode.getJSONObject("aggregations");
			}

			JSONObject hitsCollectionNode = jsonRespNode.getJSONObject("hits");
			totalHits = hitsCollectionNode.getLong("total");
			JSONArray hitsArrNode = hitsCollectionNode.getJSONArray("hits");
			for (int ii=0; ii < hitsArrNode.length(); ii++) {
				T hitObject = null;
				JSONObject hitJson = hitsArrNode.getJSONObject(ii);
				try {
					hitObject = respMapper.response2doc(hitJson, docPrototype, "");
				} catch (Exception e) {
					throw new BadDocProtoException(e);
				}
				Double hitScore = new Double(0.0);
				if (hitJson.has("_score") && !hitJson.isNull("_score")) {
					hitScore = hitJson.getDouble("_score");
				}

				JSONObject highglights = new JSONObject();
				if (hitJson.has("highlight")) {
					highglights = hitJson.getJSONObject("highlight");
				}
				scoredDocuments.add(new Hit<T>(hitObject, hitScore, highglights));
			}
		} catch (RuntimeException e) {
			throw new ElasticSearchException(e);
		}

		Triple<Pair<Long, String>, List<Hit<T>>, JSONObject> results =
			Triple.of(
				Pair.of(totalHits, scrollID),
				scoredDocuments,
				aggregations
			);

		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Returning results="+ PrettyPrinter.print(results));
		}


		return results;
	}

	public void init_Searchresults(String jsonResponse, T _docPrototype,
		StreamlinedClient _esClient, URL _searchURL)
		throws ElasticSearchException{

		Long _totalHits = new Long(0);
		String scrollID = null;
		List<Hit<T>> firstBatch = new ArrayList<Hit<T>>();
		JSONObject _aggregations = new JSONObject();
		if (jsonResponse != null) {
			Triple<Pair<Long, String>, List<Hit<T>>, JSONObject>
			parsedResults = parseJsonSearchResponse(jsonResponse, _docPrototype);
			_totalHits = parsedResults.getLeft().getLeft();
			scrollID = parsedResults.getLeft().getRight();
			firstBatch = parsedResults.getMiddle();
			_aggregations = parsedResults.getRight();
		}

		init_Searchresults(firstBatch, scrollID, _totalHits, _aggregations,
		_docPrototype, _esClient, (URL)null);
	}

	public void init_Searchresults(List<Hit<T>> firstResultsBatch,
		String _scrollID, Long _totalHits, JSONObject _aggregations,
 		T _docPrototype, StreamlinedClient _esClient, URL _searchURL) {
		this.scoredHitsBatch = firstResultsBatch;
		this.scrollID = _scrollID;
		this.docPrototype = _docPrototype;
		this.esClient = _esClient;
		this.totalHits = _totalHits;
		this.searchURL = _searchURL;
		if (_aggregations != null) {
			this.aggregations = _aggregations;
		}
	}
	
	@Override
	public Iterator<Hit<T>> iterator() {
		ScoredHitsIterator<T> iter = null;
		try {
			iter = new EmptyScoredHitsIterator<T>(docPrototype);
		} catch (ElasticSearchException | SearchResultsException e) {
			throw new RuntimeException(e);
		}

		try {
			iter = new ScoredHitsIterator<T>(scoredHitsBatch, scrollID, docPrototype, esClient, filter);
		} catch (ElasticSearchException | SearchResultsException e) {
			logger.error(e);
			if (errorPolicy() == ErrorHandlingPolicy.STRICT) {
				throw new RuntimeException(e);
			}
		}
		return iter;
	}

	public ErrorHandlingPolicy errorPolicy() {
		ErrorHandlingPolicy policy = null;
		if (esClient != null) {
			policy = esClient.getErrorPolicy();
		}
		return policy;
	}

	public DocIDIterator<T> docIDIterator() {
		return new DocIDIterator<T>(iterator());
	}

	public DocIterator<T> docIterator() {
		return new DocIterator<T>(iterator());
	}

	public UnscoredHitsIterator<T> unscoredHitsIterator() {
		UnscoredHitsIterator<T> iter = null;
		List<T> unscoredHitsBatch = new ArrayList<T>();
		for (Hit<T> scoredHit: scoredHitsBatch) {
			unscoredHitsBatch.add(scoredHit.getDocument());
		}
		
		try {
			iter = new UnscoredHitsIterator<T>(unscoredHitsBatch, scrollID, docPrototype, esClient);
		} catch (ElasticSearchException e) {
			logger.error(e);
		}
		
		return iter;
	}
	
	
	public List<T> getDocs(int nMax) {
		List<T> docs = new ArrayList<T>();
		Iterator<Hit<T>> iter = iterator();
		int count = 0;
		while (iter.hasNext()) {
			docs.add(iter.next().getDocument());
			count++;
			if (count > nMax) break;
		}
		
		return docs;
	}
	public SearchResults setFilter(HitFilter _filter) {
		this.filter = _filter;
		return this;
	}

	public Object aggrResult(String aggrName, Class clazz) throws ElasticSearchException {
		Object value = null;
		JSONObject aggField = aggregations.getJSONObject(aggrName);
		if (clazz == Integer.class) {
			value = aggField.getInt("value");
		} else if (clazz == Long.class) {
			value = aggField.getLong("value");
		} else if (clazz == Float.class || clazz == Double.class) {
			value = aggField.getDouble("value");
		} else if (clazz == String.class) {
			value = aggField.getString("value");
		} else if (clazz == JSONObject.class) {
			value = aggField.getJSONObject("value");
		} else if (clazz == JSONArray.class) {
			value = aggField.getJSONArray("value");
		} else {
			throw new ElasticSearchException("Unsupported type for aggregation field "+aggrName+": "+clazz);
		}

		return value;
	}
}
