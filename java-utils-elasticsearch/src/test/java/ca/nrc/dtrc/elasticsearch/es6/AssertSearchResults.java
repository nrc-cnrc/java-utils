package ca.nrc.dtrc.elasticsearch.es6;

import ca.nrc.testing.AssertNumber;
import ca.nrc.testing.AssertSet;
import ca.nrc.testing.Asserter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

	public AssertSearchResults hitsIDsAre(String... expIDs) throws Exception {
		Set<String> gotIDs = idsSet();
		AssertSet.assertEquals(
			baseMessage+"\nSet of hit IDs was not as expected",
			expIDs, gotIDs
		);
		return this;
	}

	private Set<String> idsSet() {
		Set<String> idsSet = new HashSet<String>();
		Iterator<Hit> iter = results().iterator();
		while (iter.hasNext()) {
			Hit hit = (Hit)iter.next();
			idsSet.add(hit.document.getId());
		}

		return idsSet;

	}

}
