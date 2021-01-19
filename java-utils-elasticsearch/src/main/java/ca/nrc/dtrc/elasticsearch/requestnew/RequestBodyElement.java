package ca.nrc.dtrc.elasticsearch.requestnew;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class RequestBodyElement {
	public RequestBodyElement() {
	}

	public RequestBodyElement(String name) {
	}

	public static Map<String,Object> merge(RequestBodyElement elt1, RequestBodyElement elt2) {
		return new HashMap<String,Object>();
	}

	public static RequestElementGeneric mergeElements(RequestBodyElement... elements) {
		JSONObject mergedFields = new JSONObject();
		for (RequestBodyElement elt: elements) {
			JSONObject jObj = elt.jsonObject();
			for (String fldName : jObj.keySet()) {
				mergedFields.put(fldName, jObj.get(fldName));
			}
		}

		RequestElementGeneric mergedElement = new RequestElementGeneric(mergedFields);
		return mergedElement;
	}

	public RequestBodyElement openAttr(String attrName) {
		return this;
	}

	public RequestBodyElement setOpenedAttr(Object attrValue) {
		return this;
	}

	public RequestBodyElement closeAttr() {
		return this;
	}

	public RequestBodyElement closeAll() {
		return this;
	}

	public abstract JSONObject jsonObject();

	public JsonString jsonString() {
		String jString = jsonObject().toString();
		return new JsonString(jString);
	}

	public  boolean containsKey(String key) {
		boolean answer = jsonObject().keySet().contains(key);
		return answer;
	}
}
