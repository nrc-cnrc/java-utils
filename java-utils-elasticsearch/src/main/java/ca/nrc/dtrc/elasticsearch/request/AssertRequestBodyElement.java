package ca.nrc.dtrc.elasticsearch.request;

import ca.nrc.testing.AssertJson;
import ca.nrc.testing.Asserter;

public class AssertRequestBodyElement extends Asserter {
	public AssertRequestBodyElement(RequestBodyElement elt) {
		super(elt);
	}

	public void jsonEquals(String expJson) throws Exception {
		AssertJson.assertJsonStringsAreEquivalent(
		baseMessage,
		expJson, element().jsonString().toString());
	}

	protected RequestBodyElement element() {
		return (RequestBodyElement) gotObject;
	}
}
