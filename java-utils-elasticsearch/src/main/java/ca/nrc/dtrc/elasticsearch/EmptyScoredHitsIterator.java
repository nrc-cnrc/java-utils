package ca.nrc.dtrc.elasticsearch;

import java.util.List;

public class EmptyScoredHitsIterator<T extends Document> extends ScoredHitsIterator<T> {

	public EmptyScoredHitsIterator() throws ElasticSearchException, SearchResultsException {
		super();
	}

	public EmptyScoredHitsIterator(T _docPrototype) throws ElasticSearchException, SearchResultsException {
		super(_docPrototype);
	}

	@Override
	public boolean hasNext() {
		return false;
	}
}