package ca.nrc.dtrc.elasticsearch.es6.request.query;

import org.json.JSONObject;

public class MatchClause extends BooleanClause {

	public MatchClause() {
		super();
		init__MatchClause();
	}

	public MatchClause(JSONObject _element) {
		super(_element);
		init__MatchClause();
	}

	protected void init__MatchClause() {
		clauseName = "match";
	}

	public MatchClause addField(String fldName, String fldValue) {
		add(new JSONObject().put(fldName, fldValue));
		return this;
	}
}
