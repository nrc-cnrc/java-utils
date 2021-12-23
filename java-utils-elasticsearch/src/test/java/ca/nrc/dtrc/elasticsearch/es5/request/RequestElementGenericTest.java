package ca.nrc.dtrc.elasticsearch.es5.request;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class RequestElementGenericTest {
	@Test
	public void test__RequestElementGeneric__HappyPath() throws Exception {
		JSONObject obj = new JSONObject()
			.put("fld1", new JSONArray()
				.put("hello")
				.put("world"))
			.put("fld2", "hi")
			;

		RequestElementGeneric elt = new RequestElementGeneric(obj);
		String expJson =
			"{\"fld1\":[\"hello\",\"world\"], \"fld2\":\"hi\"}";

		new AssertRequestBodyElement(elt)
			.jsonEquals(expJson);
	}
}
