package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.request.RequestBodyElement;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class BooleanClause extends RequestBodyElement {

	String clauseName = null;
	boolean hasSingleElement = false;
	protected List<JSONObject> elements = new ArrayList<JSONObject>();

	public BooleanClause () {
		init__BooleanClause(null);
	}

	public BooleanClause(JSONObject element) {
		init__BooleanClause(element);
	}

	private void init__BooleanClause(JSONObject elt) {
		add(elt);
	}

	public BooleanClause add(JSONObject json) {
		if (json != null) {
			if (!json.has(clauseName)) {
				// This is an actual element to be added to the clause
				elements.add(json);
			} else {
				// This is the JSON of another clause whose elements need
				// to be added to this one
				JSONArray elts2add = json.getJSONArray(clauseName);
				for (int ii=0; ii < elts2add.length(); ii++) {
					elements.add((JSONObject) elts2add.get(ii));
				}
			}
		}
		return this;
	}

	@Override
	public JSONObject jsonObject() {
		JSONObject json = new JSONObject();
		if (hasSingleElement) {
			json.put(clauseName, elements.get(0));
		} else {
			JSONArray jsonElements = new JSONArray();
			for (JSONObject elt : elements) {
				jsonElements.put(elt);
			}
			json.put(clauseName, jsonElements);
		}

		return  json;
	}

	public Object mainBody() {
		return jsonObject().get(clauseName);
	}
}
