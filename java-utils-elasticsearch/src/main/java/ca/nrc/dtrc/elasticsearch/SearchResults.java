package ca.nrc.dtrc.elasticsearch;

import ca.nrc.json.PrettyPrinter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class SearchResults<T extends Document> implements Iterable<Hit<T>> {
	
	final static Logger logger = Logger.getLogger(SearchResults.class);
	
	private T docPrototype = null;

	private String scrollID = null;

	private Map<String,Object> aggregations = new HashMap<String,Object>();
	
	protected HitFilter filter = new HitFilter();
	
	private Long totalHits = new Long(0);
	private URL searchURL = null;

	public SearchResults(String jsonResponse, T docPrototype,
		StreamlinedClient _esClient) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.SearchResults");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Constructing results of type "+docPrototype.getClass().getName()+" from jsonResponse="+jsonResponse);
		}
		init_Searchresults(jsonResponse, (T)null, (StreamlinedClient)null,
			(URL)null);
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
		public void addHit(T doc, Double score, JsonNode snippets) {
			scoredHitsBatch.add(new Hit<T>(doc,  score, snippets));
		}
		
	// Client that was used to retrieve the results.
	// The SearchResults class needs it to be able to scroll through
	// the list of hits one batch at a time.
	@JsonIgnore
	StreamlinedClient esClient = null;

	public SearchResults() throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.SearchResults");
		tLogger.trace("Empty constructor");
		init_Searchresults((String)null, (T)null, (StreamlinedClient)null,
			(URL)null);
	}

	public SearchResults(List<Hit<T>> firstResultsBatch, String _scrollID,
		Long _totalHits, T _docPrototype, StreamlinedClient _esClient) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.SearchResults");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Constructing results from _scrollID="+_scrollID+" and \n   firstResultsBatch="+PrettyPrinter.print(firstResultsBatch));
		}

		init_Searchresults(firstResultsBatch, _scrollID, _totalHits,
			(Map)null, _docPrototype, _esClient, (URL)null);
	}	
	
	public SearchResults(String jsonResponse, T _docPrototype,
		StreamlinedClient _esClient, URL url) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.SearchResults");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Constructing results from jsonResponse="+jsonResponse);
		}
		init_Searchresults(jsonResponse, _docPrototype, _esClient, url);
	}

	private Triple<Pair<Long, String>, List<Hit<T>>, Map<String, Object>>
		parseJsonSearchResponse(String jsonSearchResponse, T docPrototype) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.SearchResults.parseJsonSearchResponse");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("invoked with docPrototype="+docPrototype.getClass().getName()+", jsonSearchResponse="+jsonSearchResponse);
		}
		List<Hit<T>> scoredDocuments = new ArrayList<>();
		Map<String,Object> aggregations = new HashMap<String,Object>();
		String scrollID = null;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonRespNode;
		Long totalHits;
		try {
			jsonRespNode = (ObjectNode) mapper.readTree(jsonSearchResponse);
			if (jsonRespNode.has("_scroll_id")) {
				scrollID = jsonRespNode.get("_scroll_id").asText();
			}
			if (jsonRespNode.has("aggregations")) {
				JsonNode aggrNode = jsonRespNode.get("aggregations");
				String aggrJson = mapper.writeValueAsString(aggrNode);
				aggregations = mapper.readValue(aggrJson, aggregations.getClass());
			}

			ObjectNode hitsCollectionNode = (ObjectNode) jsonRespNode.get("hits");
			totalHits = hitsCollectionNode.get("total").asLong();
			ArrayNode hitsArrNode = (ArrayNode) hitsCollectionNode.get("hits");
			String hitSource = null;
			for (int ii=0; ii < hitsArrNode.size(); ii++) {
				T hitObject = null;
				String hitID = null;
				try {
					hitSource = hitsArrNode.get(ii).get("_source").toString();
					hitID =
						mapper.readValue(
							hitsArrNode.get(ii).get("_id").toString(), String.class);
					if (tLogger.isTraceEnabled()) {
						tLogger.trace("Parsing hit #"+ii+": "+hitSource+" with _id="+hitID);
					}
					hitObject = (T) mapper.readValue(hitSource, docPrototype.getClass());
				} catch (Exception e) {
					throw new BadDocProtoException(e);
				}
				hitObject.id = hitID;
				Double hitScore = hitsArrNode.get(ii).get("_score").asDouble();

				scoredDocuments.add(new Hit<T>(hitObject, hitScore, hitsArrNode.get(ii).get("highlight")));
			}
		} catch (IOException | RuntimeException e) {
			throw new ElasticSearchException(e);
		}

		Triple<Pair<Long, String>, List<Hit<T>>, Map<String, Object>> results =
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
		Map<String,Object> _aggregations = new HashMap<String,Object>();
		if (jsonResponse != null) {
			Triple<Pair<Long, String>, List<Hit<T>>, Map<String, Object>>
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
		String _scrollID, Long _totalHits, Map<String,Object> _aggregations,
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
			iter = new ScoredHitsIterator<T>(scoredHitsBatch, scrollID, docPrototype, esClient, filter);
		} catch (ElasticSearchException | SearchResultsException e) {
			logger.error(e);
		}
		return iter;
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

	public Object aggrResult(String aggrName) {
		Map<String,Object> aggr = (Map<String, Object>) aggregations.get(aggrName);
		Object value = null;
		if (aggr != null) {
			value = aggr.get("value");
		}
		return value;
	}
}
