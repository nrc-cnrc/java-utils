package ca.nrc.dtrc.elasticsearch.request;

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
