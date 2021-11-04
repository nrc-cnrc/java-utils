package ca.nrc.dtrc.elasticsearch;

import java.util.List;

public class EmptyScoredHitsIterator<T extends Document> extends ScoredHitsIterator<T> {

	public EmptyScoredHitsIterator() throws ElasticSearchException, SearchResultsException {
		super();
	}

	public EmptyScoredHitsIterator(List<Hit<T>> firstResultsBatch, String _scrollID, T _docPrototype, StreamlinedClient _esClient, HitFilter _filter) throws ElasticSearchException, SearchResultsException {
		super(firstResultsBatch, _scrollID, _docPrototype, _esClient, _filter);
	}

	@Override
	public boolean hasNext() {
		return false;
	}
}