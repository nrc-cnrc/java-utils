package ca.nrc.dtrc.elasticsearch;

import ca.nrc.testing.AssertNumber;
import ca.nrc.testing.Asserter;

public class AssertSearchResults extends Asserter<SearchResults> {
	public AssertSearchResults(SearchResults _gotObject) {
		super(_gotObject);
	}

	public AssertSearchResults(SearchResults _gotObject, String mess) {
		super(_gotObject, mess);
	}

	public SearchResults results() {
		return (SearchResults)gotObject;
	}

	public AssertSearchResults totalHitsEquals(int expTotal) throws Exception {
		Long gotTotal = results().getTotalHits();
		AssertNumber.assertEquals(
			baseMessage+"\nTotal number of hits was not as expected",
			expTotal, gotTotal, 0.0);
		return this;
	}
}
