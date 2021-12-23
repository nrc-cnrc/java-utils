package ca.nrc.dtrc.elasticsearch.es6.request.query;

import org.json.JSONObject;

public class ShouldClause extends BooleanClause {

	public ShouldClause() {
		super();
		init__ShouldClause();
	}

	public ShouldClause(JSONObject _element) {
		super(_element);
		init__ShouldClause();
	}

	protected void init__ShouldClause() {
		clauseName = "should";
	}

}
