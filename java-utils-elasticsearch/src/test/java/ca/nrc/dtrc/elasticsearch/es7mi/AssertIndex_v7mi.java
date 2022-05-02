package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.testing.AssertObject;
import ca.nrc.testing.Asserter;

import java.util.Set;

public class AssertIndex_v7mi extends Asserter<IndexAPI_v7mi> {

	public AssertIndex_v7mi(IndexAPI_v7mi _gotObject) {
		super(_gotObject);
	}

	public AssertIndex_v7mi(IndexAPI_v7mi _gotObject, String mess) {
		super(_gotObject, mess);
	}

	protected IndexAPI_v7mi index() {
		return (IndexAPI_v7mi)gotObject;
	}

	public AssertIndex_v7mi typesAre(String[] expTypes) throws Exception {
		Set<String> gotTypes = index().types();
		AssertObject.assertDeepEquals(
			"List of types not as expected",
			expTypes, gotTypes);
		return this;
	}

}
