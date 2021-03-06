package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.nrc.json.PrettyPrinter;

public class ScoredHitsIterator<T extends Document> implements Iterator<Hit<T>> {
	
	private static final int MAX_CONSEC_UNSUCCESSFUL_BATCHES = 5;

	private T docPrototype = null;

	private String scrollID = null;
	
	private HitFilter filter = new HitFilter();
	
	private Long totalHits = new Long(0);
		public Long getTotalHits() {return totalHits;}
		public void setTotalHits(Long _totalHits) {this.totalHits = _totalHits;}
		
	private  List<Hit<T>> documentsBatch = new ArrayList<>();		
		public List<Hit<T>> getFirstDocumentsBatch() {return documentsBatch;}
		
	private int batchCursor = 0;	
	
	boolean potentiallyMoreInIndex = true;
	
	// Client that was used to retrieve the results.
	// The SearchResults class needs it to be able to scroll through
	// the list of hits one batch at a time.
	@JsonIgnore
	StreamlinedClient esClient = null;	
	
	public ScoredHitsIterator(List<Hit<T>> firstResultsBatch, String _scrollID, T _docPrototype, StreamlinedClient _esClient, HitFilter _filter) throws ElasticSearchException, SearchResultsException {
		this.scrollID = _scrollID;
		this.docPrototype = _docPrototype;
		this.esClient = _esClient;
		this.filter = _filter;
		this.retrieveAndFilterUntilNonEmptyBatch(firstResultsBatch);
	}
	
	private void retrieveAndFilterUntilNonEmptyBatch() throws ElasticSearchException, SearchResultsException {
		retrieveAndFilterUntilNonEmptyBatch(null);
	}

	
	private void retrieveAndFilterUntilNonEmptyBatch(List<Hit<T>> initialBatch) throws ElasticSearchException, SearchResultsException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ScoredHitsIterator.retrieveAndFilterUntilNonEmptyBatch");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace("scrollID="+scrollID+", initialBatch="+PrettyPrinter.print(initialBatch));
		}
		if (initialBatch != null) { 
			documentsBatch = initialBatch; 
		} else {
			documentsBatch = new ArrayList<Hit<T>>();
		}
		
		if (scrollID == null) {
			// Note: scrollID == null may happen when we are creating 
			//   dummy list of hits for testing purposes.
			documentsBatch = initialBatch;
		} else {
			int unsuccessfulBatchesCountdown = MAX_CONSEC_UNSUCCESSFUL_BATCHES;
			while(true) {
				filterDocumentsBatch();
				
				// Last batch retrieved from ES contains some hits that passed the filter
				if (documentsBatch.size() > 0) break;
				
				// Last batch retrieved from ES did NOT contains any hits that pass the filter
				// Try another batch unless we reached the maximum number of consecutive
				// unsuccessful batches
				unsuccessfulBatchesCountdown--;
				if (unsuccessfulBatchesCountdown == 0) break;
				
				documentsBatch = esClient.scrollScoredHits(scrollID, docPrototype);
				
				// No more hits to be retrieved from ElasticSearch
				if (documentsBatch == null) break;
			}
		}
		batchCursor = 0;

		if (tLogger.isTraceEnabled()) {
			tLogger.trace("Upon exit, batchCursor="+batchCursor+", scrollID="+scrollID+", documentsBatch="+PrettyPrinter.print(documentsBatch));
		}
	}		
		

	private void filterDocumentsBatch() throws SearchResultsException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.filterDocumentsBatch");
		List<Hit<T>> filteredHits = new ArrayList<Hit<T>>();
		for (Hit<T> aHit: documentsBatch) {
			try {
				if (filter == null || filter.keep(aHit)) { 
					filteredHits.add(aHit);							
				} else {
					if (tLogger.isTraceEnabled()) {
						tLogger.trace("** Rejected aHit="+PrettyPrinter.print(aHit));
					}
				}
			} catch (HitFilterException e) { 
				throw new SearchResultsException(e);
			}
		}
		documentsBatch = filteredHits;
	}
	@Override
	public boolean hasNext() {
		
		Boolean answer = null;
		
		if (documentsBatch == null) {
			// Null batch --> false;
			answer = false;
		}
		
		if (answer == null && documentsBatch.size() == 0) {
			// Non null but empty batch --> false;
			answer = false;
		}
		
		if (answer == null) {
			// Current batch is not null and it is not empty
			// Has the cursor reached the end of the batch?
			// if so, retrieve next batch, and check again
			if (batchCursor == documentsBatch.size()) {
				try {
					retrieveAndFilterUntilNonEmptyBatch();
					answer = hasNext();
				} catch (ElasticSearchException | SearchResultsException e) {
					answer = false;
					e.printStackTrace();
				}
			} else {
				answer = true;
			}
		}
		
		return (boolean) answer;
	}

	
	@Override
	public Hit<T> next() {
		Hit<T> nextItem = null;
		if (!hasNext()) {
			String errMess = "There were no more in the list of ElasticSearch hits.";
			if (docPrototype != null) {
				errMess += "\nDoc prototype: "+docPrototype.getClass().getName();
			}
			throw new RuntimeException(errMess);
		} else {
			// Get next item in current batch.
			//
			// Note: we can assume that the current batch will be non-null and that
			// the cursor is not positioned at its end, because this will have been
			// ensured by hasNext()
			//
			nextItem = documentsBatch.get(batchCursor);
			batchCursor++;
		}

		return nextItem;
	}
}
