package ca.nrc.dtrc.elasticsearch.request;

import org.json.JSONObject;

public class Query extends RequestElementGeneric {

	public Query(JSONObject obj) {
		super(obj);
	}

	public JSONObject jsonObject() {
		JSONObject obj = new JSONObject()
			.put("query", _jsonObject)
		;
		return obj;
	}
}
