package ca.nrc.dtrc.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.nrc.datastructure.Pair;

public class SearchResults<T extends Document> implements Iterable<Pair<T,Double>> {
	
	final static Logger logger = Logger.getLogger(SearchResults.class);
	
	private T docPrototype = null;

	private String scrollID = null;
	
	private Long totalHits = new Long(0);
		public Long getTotalHits() {return totalHits;}
		public void setTotalHits(Long _totalHits) {this.totalHits = _totalHits;}
		
	private  List<Pair<T,Double>> scoredHitsBatch = new ArrayList<Pair<T,Double>>();	
		@JsonIgnore
		public List<Pair<T,Double>> getScoredHitsBatch() {return scoredHitsBatch;}
		
	private int batchCursor = 0;
		
	private Double maxScore = 0.0;
		public Double getMaxScore() {return maxScore;}
		public void setMaxScore(Double _maxScore) {
			this.maxScore = _maxScore;
		}
	
	private List<Pair<T,Double>> topHits = new ArrayList<Pair<T,Double>>();
		public List<Pair<T,Double>> getTopScoredDocuments() {
			return topHits;
		}
		public void addHit(T doc, Double score) {
			scoredHitsBatch.add(Pair.of(doc,  score));
		}
		
	// Client that was used to retrieve the results.
	// The SearchResults class needs it to be able to scroll through
	// the list of hits one batch at a time.
	@JsonIgnore
	StreamlinedClient esClient = null;

	public SearchResults() {
		
	}

	public SearchResults(List<Pair<T,Double>> firstResultsBatch, String _scrollID, Long _totalHits, T _docPrototype, StreamlinedClient _esClient) throws ElasticSearchException {
		initialize(firstResultsBatch, _scrollID, _totalHits, _docPrototype, _esClient);
	}	
	
	public SearchResults(String jsonResponse, T _docPrototype, StreamlinedClient _esClient) throws ElasticSearchException {
		Pair<Pair<Long, String>, List<Pair<T, Double>>> parsedResults = parseJsonSearchResponse(jsonResponse, _docPrototype);
		Long _totalHits = parsedResults.getFirst().getFirst();
		List<Pair<T,Double>> firstBatch = parsedResults.getSecond();
		String scrollID = parsedResults.getFirst().getSecond();

		initialize(firstBatch, scrollID, _totalHits, _docPrototype, _esClient);
	}
	
	public void initialize(List<Pair<T,Double>> firstResultsBatch, String _scrollID, Long _totalHits, T _docPrototype, StreamlinedClient _esClient) {
		this.scoredHitsBatch = firstResultsBatch;
		this.scrollID = _scrollID;
		this.docPrototype = _docPrototype;
		this.esClient = _esClient;
		this.totalHits = _totalHits;		
	}
	
	private Pair<Pair<Long,String>,List<Pair<T, Double>>> parseJsonSearchResponse(String jsonSearchResponse, T docPrototype) throws ElasticSearchException {
		List<Pair<T, Double>> scoredDocuments = new ArrayList<Pair<T,Double>>();
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
			for (int ii=0; ii < hitsArrNode.size(); ii++) {
				String hitJson = hitsArrNode.get(ii).get("_source").toString();
				T hitObject = (T) mapper.readValue(hitJson, docPrototype.getClass());
				Double hitScore = hitsArrNode.get(ii).get("_score").asDouble();
				
				scoredDocuments.add(Pair.of(hitObject, hitScore));
			}
		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}			
		
		return Pair.of(Pair.of(totalHits, scrollID), scoredDocuments);
	}	
	
	@Override
	public Iterator<Pair<T, Double>> iterator() {
		ScoredHitsIterator<T> iter = null;
		try {
			iter = new ScoredHitsIterator<T>(scoredHitsBatch, scrollID, docPrototype, esClient);
		} catch (ElasticSearchException e) {
			logger.error(e);
		}
		return iter;
	}
	
	public UnscoredHitsIterator<T> unscoredHitsIterator() {
		UnscoredHitsIterator<T> iter = null;
		List<T> unscoredHitsBatch = new ArrayList<T>();
		for (Pair<T,Double> scoredHit: scoredHitsBatch) {
			unscoredHitsBatch.add(scoredHit.getFirst());
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
		Iterator<Pair<T,Double>> iter = iterator();
		int count = 0;
		while (iter.hasNext()) {
			docs.add(iter.next().getFirst());
			count++;
			if (count > nMax) break;
		}
		
		return docs;
	}

}
