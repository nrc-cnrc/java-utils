package ca.nrc.dtrc.elasticsearch;

import ca.nrc.testing.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

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

	public void hitIDsEqual(String... expIDs) throws IOException {
		Set<String> gotIDs = hitIDsSet();
		AssertSet.assertEquals(
			baseMessage+"\nIDs of retrieved docs were not as expected",
			expIDs, gotIDs);
	}

	public void containsIDs(String... expIDs) throws IOException {
		Set<String> gotIDs = hitIDsSet();
		AssertSet.assertContainsAll(
			baseMessage+"\nRetrieved docs did not contain the expected IDs",
			hitIDsSet(), expIDs);
	}

	public AssertSearchResults doesNotcontainIDs(String... unexpectedIDs) throws IOException {
		Set<String> gotIDs = hitIDsSet();
		AssertSet.containsNoneOf(
			baseMessage+"\nRetrieved docs contained some unexpected IDs",
			unexpectedIDs, hitIDsSet());
		return this;
	}

	private Set<String> hitIDsSet() {
		Set<String> gotIDs = new HashSet<String>();
		DocIDIterator iter = results().docIDIterator();
		while (iter.hasNext()) {
			gotIDs.add(iter.next());
		}
		return gotIDs;
	}

	public void fieldValueFoundInFirstNHits(
		Pair<String, String> expField, int firstN) throws Exception {
		String fldName = expField.getLeft();
		String fldVal = expField.getRight();
		List<Object> firstNValue =
			fieldValuesForFirstNHits(firstN, fldName);
		AssertCollection.assertContains(
			baseMessage+"\nField value "+fldName+"="+fldVal+
				" not found in the first "+firstN+" hits",
			fldVal, firstNValue, true
		);
	}


	private <T extends Document> List<Object> fieldValuesForFirstNHits(
		int nHits, String fieldName)
		throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, DocumentException {

		List<Object> gotValues = new ArrayList<Object>();
		Iterator<Hit<T>> iter = results().iterator();
		while (iter.hasNext()) {
			T aHit = iter.next().getDocument();
			if (nHits <= 0) break;
			Object fldValue = null;
			if (aHit instanceof Map<?,?>) {
				Map<String,Object> aHitMap = (Map<String, Object>) aHit;
				fldValue = (Object) aHitMap.get(fieldName);
			} else {
				fldValue = aHit.getField(fieldName);
			}
			gotValues.add(fldValue);
			nHits--;
		}

		return gotValues;
	}

}
