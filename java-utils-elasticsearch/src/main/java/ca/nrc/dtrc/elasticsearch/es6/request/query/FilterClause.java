package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.request.RequestBodyElement;
import org.json.JSONObject;

public class FilterClause extends RequestBodyElement {

	JSONObject terms = new JSONObject();

	public FilterClause() {
		super();
		init__FilterClause();
	}

	protected void init__FilterClause() {
	}

	public FilterClause addTerm(String fldName, String fldConstraint) {
		if (fldConstraint != null) {
			// For some reason, filter only works with lowercased constraints,
			// eventhough other clauses like "match" can work with capitalized words
			fldConstraint = fldConstraint.toLowerCase();
		}
		terms.put(fldName, fldConstraint);
		return this;
	}

	@Override
	public JSONObject jsonObject() {
		JSONObject jObj =
			new JSONObject()
			.put("term", terms)
			;
		return jObj;
	}

}
