package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ca.nrc.json.PrettyPrinter;

public abstract class ScoredHitsIterator<T extends Document> implements Iterator<Hit<T>> {

	protected abstract List<Hit<T>> nextHitsPage() throws ElasticSearchException;

	private static final int MAX_CONSEC_UNSUCCESSFUL_BATCHES = 5;

	public SearchAPI.PaginationStrategy paginateWith =
		SearchAPI.PaginationStrategy.SCROLL;

	protected T docPrototype = null;

	private HitFilter filter = new HitFilter();
	
	private Long totalHits = new Long(0);

	public Long getTotalHits() {return totalHits;}
		public void setTotalHits(Long _totalHits) {this.totalHits = _totalHits;}
		
	protected  List<Hit<T>> documentsBatch = new ArrayList<Hit<T>>();
		public List<Hit<T>> getFirstDocumentsBatch() {return documentsBatch;}
		
	private int batchCursor = 0;	
	
	// ESFactory that was used to retrieve the results.
	// The SearchResults class needs it to be able to scroll through
	// the list of hits one batch at a time.
	@JsonIgnore
	ESFactory esFactory = null;

	ObjectMapper mapper = new ObjectMapper();


	public ScoredHitsIterator(ESFactory _esFactory, T docPrototype) throws ElasticSearchException, SearchResultsException {
		init__ScoredHitsIterator(
			(List<Hit<T>>)null, docPrototype,
			_esFactory, (HitFilter)null);
	}

	public ScoredHitsIterator(List<Hit<T>> firstResultsBatch,
		T _docPrototype, ESFactory _esFactory, HitFilter _filter) throws ElasticSearchException, SearchResultsException {
		init__ScoredHitsIterator(
			firstResultsBatch, _docPrototype, _esFactory, _filter);
	}

	private void init__ScoredHitsIterator(
		List<Hit<T>> firstResultsBatch,
		T _docPrototype, ESFactory _esFactory, HitFilter _filter)
		throws ElasticSearchException, SearchResultsException {

		this.docPrototype = _docPrototype;
		this.esFactory = _esFactory;
		this.filter = _filter;
		if (firstResultsBatch != null) {
			this.documentsBatch = firstResultsBatch;
		}
		return;
	}

	public ErrorHandlingPolicy errorPolicy() {
		ErrorHandlingPolicy policy = ErrorHandlingPolicy.STRICT;
		if (esFactory != null) {
			policy = esFactory.getErrorPolicy();
		}
		return policy;
	}
	
	protected void retrieveAndFilterUntilNonEmptyBatch() throws ElasticSearchException, SearchResultsException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ScoredHitsIterator.retrieveAndFilterUntilNonEmptyBatch");
		if (tLogger.isTraceEnabled()) {
			tLogger.trace(
				"\n  esClient.getErrorPolicy(): " + esFactory.getErrorPolicy());
		}

		int loopCounter = 0;

		// Keep polling the server for a new batch of hits until either:
		// - We get one that contains at least one hit that passes the filter.
		//    OR
		// - We reach the maximum number of tries.

		boolean keepGoing = true;
		while(keepGoing) {
			loopCounter++;
			traceDocsBatch(tLogger,
				"retrieveAndFilterUntilNonEmptyBatch[top of while loop]:\n  loopCounter="+loopCounter);

			try {
				documentsBatch = nextHitsPage();
				if (documentsBatch != null && documentsBatch.isEmpty()) {
					// If the next page of hits is empty, that means we there
					// are no more hits to be found.
					documentsBatch = null;
					break;
				}
				traceDocsBatch(tLogger,
					"retrieveAndFilterUntilNonEmptyBatch[after polling server for new batch]:\n  loopCounter="+loopCounter);
			} catch (Exception e) {
				if (e.getMessage().contains("scrollId is missing")) {
					tLogger.trace("There are no more hits to be had: EXITING\n  loopCounter="+loopCounter);
					documentsBatch = null;
					break;
				} else {
					throw e;
				}
			}

			filterDocumentsBatch();
			traceDocsBatch(tLogger,
				"retrieveAndFilterUntilNonEmptyBatch[after filtering current batch]:\n  loopCounter="+loopCounter);

			if (documentsBatch.size() > 0) {
				// Last batch retrieved from server contains some hits that passed the filter
				tLogger.trace("At least one hit in the batch passes the filter: EXITING\n  loopCounter="+loopCounter);
				break;
			} else {
				tLogger.trace("None of the hits in the batch passes the filter.\n  loopCounter="+loopCounter);
				if (loopCounter == MAX_CONSEC_UNSUCCESSFUL_BATCHES) {
					tLogger.trace("Reached max number of tries: EXITING");
					break;
				} else {
					tLogger.trace("Trying another batch.\n  loopCounter="+loopCounter);
				}
			}
		}
		batchCursor = 0;

		traceDocsBatch(tLogger, "retrieveAndFilterUntilNonEmptyBatch[upon exit]");
		return;
	}

	private void traceDocsBatch(Logger tLogger, String context) {
		if (tLogger.isTraceEnabled()) {
			String mess = context + "\n"
				+ " batchCursor: " + batchCursor + "\n"
				+ " documentsBatch: " + (documentsBatch == null?"null": documentsBatch.size()+" elements")
				;
			tLogger.trace(mess);
		}
	}


	protected void filterDocumentsBatch() throws SearchResultsException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.filterDocumentsBatch");
		if (documentsBatch != null) {
			List<Hit<T>> filteredHits = new ArrayList<Hit<T>>();
			for (Hit<T> aHit : documentsBatch) {
				try {
					if (filter == null || filter.keep(aHit)) {
						filteredHits.add(aHit);
					} else {
						if (tLogger.isTraceEnabled()) {
							tLogger.trace("** Rejected aHit=" + PrettyPrinter.print(aHit));
						}
					}
				} catch (HitFilterException e) {
					throw new SearchResultsException(e);
				}
			}
			documentsBatch = filteredHits;
		}
	}
	@Override
	public boolean hasNext() {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ScoredHitsIterator.hasNext");
		Boolean answer = null;

		while (answer == null) {
			if (documentsBatch == null) {
				// Null batch --> false;
				answer = false;
			}

			if (answer == null) {
				// Current batch is not null and it is not empty
				// Has the cursor reached the end of the batch?
				// if so, retrieve next batch, and check again
				//
				if (batchCursor == documentsBatch.size()) {
					try {
						retrieveAndFilterUntilNonEmptyBatch();
						continue;
					} catch (ElasticSearchException | SearchResultsException e) {
						documentsBatch = null;
						answer = false;
					}
				}
			}

			if (answer == null && errorPolicy() == ErrorHandlingPolicy.LENIENT &&
				null == documentsBatch.get(batchCursor)) {
				// If using LENIENT error handling, then just skip null batch
				// elements
				//
				batchCursor++;
				continue;
			}

			if (answer == null) {
				// At this point, we know that:
				// - Current batch is not empty
				// - Cursor is not at the end of the batch
				// - Batch element at cursor position is not null
				answer = true;
			}
		}

		if (logger.isEnabledFor(Level.ERROR)) {
			if (answer && null == documentsBatch.get(batchCursor)) {
				logger.error("Iterator claims there is a next element, yet the element at the cursor is null");
			}
		}
		return (boolean) answer;
	}

	
	@Override
	public Hit<T> next() {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ScoredHitsIterator.next");
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

		if (logger.isEnabledFor(Level.ERROR)) {
			String docClass = "UNKNOWN";
			if (docPrototype != null) {
				docClass = docPrototype.getClass().getSimpleName();
			}
			if (nextItem == null) {
				logger.error("*** Next hit is null!!! (doc class="+docClass+")");
			}
			T nextDocument = nextItem.getDocument();
			if (nextDocument == null) {
				logger.error("*** Next document is null!!! (doc class="+docClass+")");
		 	} else if (nextDocument.getId() == null) {
				try {
					logger.error("*** Next document has a null ID!!! (doc class="+docClass+"):\n"+mapper.writeValueAsString(nextDocument));
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return nextItem;
	}
}
