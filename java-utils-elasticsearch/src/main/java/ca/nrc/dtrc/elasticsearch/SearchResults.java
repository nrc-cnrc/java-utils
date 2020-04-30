package ca.nrc.dtrc.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.nrc.datastructure.Pair;

public class SearchResults<T extends Document> implements Iterable<Hit<T>> {
	
	final static Logger logger = Logger.getLogger(SearchResults.class);
	
	private T docPrototype = null;

	private String scrollID = null;
	
	protected HitFilter filter = new HitFilter();
	
	private Long totalHits = new Long(0);
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

	public SearchResults() {
		
	}

	public SearchResults(List<Hit<T>> firstResultsBatch, String _scrollID, Long _totalHits, T _docPrototype, StreamlinedClient _esClient) throws ElasticSearchException {
		initialize(firstResultsBatch, _scrollID, _totalHits, _docPrototype, _esClient);
	}	
	
	public SearchResults(String jsonResponse, T _docPrototype, StreamlinedClient _esClient) throws ElasticSearchException {
		Pair<Pair<Long, String>, List<Hit<T>>> parsedResults = parseJsonSearchResponse(jsonResponse, _docPrototype);
		Long _totalHits = parsedResults.getFirst().getFirst();
		List<Hit<T>> firstBatch = parsedResults.getSecond();
		String scrollID = parsedResults.getFirst().getSecond();

		initialize(firstBatch, scrollID, _totalHits, _docPrototype, _esClient);
	}
	
	public void initialize(List<Hit<T>> firstResultsBatch, String _scrollID, Long _totalHits, T _docPrototype, StreamlinedClient _esClient) {
		this.scoredHitsBatch = firstResultsBatch;
		this.scrollID = _scrollID;
		this.docPrototype = _docPrototype;
		this.esClient = _esClient;
		this.totalHits = _totalHits;		
	}
	
	private Pair<Pair<Long,String>,List<Hit<T>>> parseJsonSearchResponse(String jsonSearchResponse, T docPrototype) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.SearchResults.parseJsonSearchResponse");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("invoked with docPrototype="+docPrototype+", jsonSearchResponse="+jsonSearchResponse);
		}
		List<Hit<T>> scoredDocuments = new ArrayList<>();
		String scrollID = null;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonRespNode;
		Long totalHits;
		try {
			jsonRespNode = (ObjectNode) mapper.readTree(jsonSearchResponse);	
			scrollID = jsonRespNode.get("_scroll_id").asText();
			ObjectNode hitsCollectionNode = (ObjectNode) jsonRespNode.get("hits");
			totalHits = hitsCollectionNode.get("total").asLong();
			ArrayNode hitsArrNode = (ArrayNode) hitsCollectionNode.get("hits");
			String hitJson = null;
			for (int ii=0; ii < hitsArrNode.size(); ii++) {
				hitJson = hitsArrNode.get(ii).get("_source").toString();
				T hitObject = null;
				try {
					hitObject = (T) mapper.readValue(hitJson, docPrototype.getClass());
				} catch (Exception e) {
					throw new BadDocProtoException(e);
				}
				Double hitScore = hitsArrNode.get(ii).get("_score").asDouble();
				
				scoredDocuments.add(new Hit<T>(hitObject, hitScore, hitsArrNode.get(ii).get("highlight")));
			}
		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}			
		
		return Pair.of(Pair.of(totalHits, scrollID), scoredDocuments);
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

}
