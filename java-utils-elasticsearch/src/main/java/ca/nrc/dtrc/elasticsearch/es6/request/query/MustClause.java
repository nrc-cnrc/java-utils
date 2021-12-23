package ca.nrc.dtrc.elasticsearch.es6.request.query;

import org.json.JSONObject;

public class MustClause extends BooleanClause {
	public MustClause() {
		super(null);
		init__MustClause();
	}

	public MustClause(JSONObject _element) {
		super(_element);
		init__MustClause();
	}

	protected void init__MustClause() {
		clauseName = "must";
	}
}
