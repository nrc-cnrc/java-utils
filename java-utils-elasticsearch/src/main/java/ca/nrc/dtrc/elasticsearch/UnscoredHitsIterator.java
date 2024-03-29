package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import ca.nrc.datastructure.CloseableIterator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UnscoredHitsIterator<T extends Document> implements CloseableIterator<T> {
	private T docPrototype = null;

	private String scrollID = null;
	
	private Long totalHits = new Long(0);
		public Long getTotalHits() {return totalHits;}
		public void setTotalHits(Long _totalHits) {this.totalHits = _totalHits;}
		
	private  List<T> documentsBatch = new ArrayList<T>();		
		public List<T> getFirstDocumentsBatch() {return documentsBatch;}
		
	private int batchCursor = 0;	
	
	// ESFactory that was used to retrieve the results.
	// The SearchResults class needs it to be able to scroll through
	// the list of hits one batch at a time.
	@JsonIgnore
	ESFactory esFactory = null;
	
	public UnscoredHitsIterator(List<T> firstResultsBatch, String _scrollID,
 		T _docPrototype, ESFactory _esFactory) throws ElasticSearchException {
		this.documentsBatch = firstResultsBatch;
		this.scrollID = _scrollID;
		this.docPrototype = _docPrototype;
		this.esFactory = _esFactory;
	}
	
	@Override
	public boolean hasNext() {
		
		Boolean answer = null;
		
		if (documentsBatch == null) {
			// Null batch --> false;
			answer = false;
		}
		
		if (answer == null && documentsBatch != null && documentsBatch.size() == 0) {
			// Non null but empty batch --> false;
			answer = false;
		}
		
		if (answer == null) {
			// Current batch is not null and it is not empty
			// Has the cursor reached the end of the batch?
			// if so, retrieve next batch, and check again
			if (batchCursor == documentsBatch.size()) {
				try {
					retrieveNewBatch();
					answer = hasNext();
				} catch (ElasticSearchException e) {
					answer = false;
					e.printStackTrace();
				}
			} else {
				answer = true;
			}
		}
		
		return (boolean) answer;
	}
	
	private void retrieveNewBatch() throws ElasticSearchException {
		documentsBatch = esFactory.searchAPI().scroll(scrollID, docPrototype);
		batchCursor = 0;
	}
	
	@Override
	public T next() {
		T  nextItem = null;
		if (hasNext()) {
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


	@Override
	public void close() throws Exception {
		// Nothing to close
	}
}
