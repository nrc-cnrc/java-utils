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

import ca.nrc.ict.Collections.Pair;

public class SearchResults<T extends Document> implements Iterable<Pair<T,Double>> {
	
	final static Logger logger = Logger.getLogger(SearchResults.class);
	
	private T docPrototype = null;

	private String scrollID = null;
	
	private Long totalHits = new Long(0);
		public Long getTotalHits() {return totalHits;}
		public void setTotalHits(Long _totalHits) {this.totalHits = _totalHits;}
		
	private  List<Pair<T,Double>> documentsBatch = new ArrayList<Pair<T,Double>>();		
		public List<Pair<T,Double>> getFirstDocumentsBatch() {return documentsBatch;}
		
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
			documentsBatch.add(Pair.of(doc,  score));
		}
		
	// Client that was used to retrieve the results.
	// The SearchResults class needs it to be able to scroll through
	// the list of hits one batch at a time.
	@JsonIgnore
	StreamlinedClient esClient = null;

	public SearchResults() {
		
	}

	public SearchResults(List<Pair<T,Double>> firstResultsBatch, String _scrollID, T _docPrototype, StreamlinedClient _esClient) throws ElasticSearchException {
		this.documentsBatch = firstResultsBatch;
		this.scrollID = _scrollID;
		this.docPrototype = _docPrototype;
		this.esClient = _esClient;
	}	
	
	@Override
	public Iterator<Pair<T, Double>> iterator() {
		SearchResultsIterator<T> iter = null;
		try {
			iter = new SearchResultsIterator<T>(documentsBatch, scrollID, docPrototype, esClient);
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
