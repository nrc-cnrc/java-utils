package ca.nrc.dtrc.elasticsearch.es6.request;

import org.json.JSONObject;

public class RequestElementGeneric extends RequestBodyElement {
	JSONObject _jsonObject = null;

	public RequestElementGeneric(JSONObject obj) {
		this._jsonObject = obj;
	}

	@Override
	public JSONObject jsonObject() {
		return _jsonObject;
	}
}
