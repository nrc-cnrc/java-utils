package ca.nrc.dtrc.elasticsearch.es6.request.query;

import org.json.JSONObject;

public class MustNotClause extends BooleanClause {

	public MustNotClause() {
		super(null);
		init__MustNotClause();
	}

	public MustNotClause(JSONObject _element) {
		super(_element);
		init__MustNotClause();
	}

	protected void init__MustNotClause() {
		clauseName = "must_not";
	}

}
