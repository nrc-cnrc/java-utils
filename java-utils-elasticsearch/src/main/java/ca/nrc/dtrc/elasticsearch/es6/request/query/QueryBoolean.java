package ca.nrc.dtrc.elasticsearch.es6.request.query;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class QueryBoolean extends Query {
	Map<String,BooleanClause> clauses = new HashMap<String,BooleanClause>();

	public QueryBoolean(BooleanClause... _clauses) {
		super();
		init_QueryBoolean(_clauses);

	}

	private void init_QueryBoolean(BooleanClause[] _clauses) {
		for (BooleanClause aClause: _clauses) {
			addClause(aClause);
		}
	}

	public void addClause(BooleanClause clause) {
			if (!clauses.containsKey(clause.clauseName)) {
				clauses.put(clause.clauseName, clause);
			} else {
				clauses.get(clause.clauseName).add(clause.jsonObject());
			}
	}

	@Override
	public JSONObject jsonObject() {
		JSONObject json = new JSONObject()
			.put("query", new JSONObject()
				.put("bool", new JSONObject())
			);

		JSONObject jsonQuery = json.getJSONObject("query").getJSONObject("bool");
		for (String clauseName: clauses.keySet()) {
			jsonQuery.put(clauseName, clauses.get(clauseName).elements);

		}

		return json;
	}

}
