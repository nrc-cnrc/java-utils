package ca.nrc.dtrc.elasticsearch.request;

import org.json.JSONObject;

public class Query extends RequestElementGeneric {

	public Query(JSONObject obj) {
		super(obj);
	}

	public JSONObject jsonObject() {
		JSONObject obj = new JSONObject();
		JSONObject querySpecs = _jsonObject;
		if (querySpecs.has("query")) {
			// The details of the query already starts with a "query" field
			// at its root. Make sure we don't repeat it as {query: {query etc...
			querySpecs = querySpecs.getJSONObject("query");
		}
		obj.put("query", querySpecs);

		return obj;
	}
}
