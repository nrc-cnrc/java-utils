package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.List;

public class EmptyScoredHitsIterator<T extends Document> extends ScoredHitsIterator<T> {

	@Override
	protected List<Hit<T>> nextHitsPage() throws ElasticSearchException {
		return new ArrayList<Hit<T>>();
	}

	public EmptyScoredHitsIterator(ESFactory _esFactory, T _docPrototype) throws ElasticSearchException, SearchResultsException {
		super(_esFactory, _docPrototype);
	}

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	protected void retrieveAndFilterUntilNonEmptyBatch() throws ElasticSearchException, SearchResultsException {
		// No need to retrieve hits for an empty iterator
	}
}