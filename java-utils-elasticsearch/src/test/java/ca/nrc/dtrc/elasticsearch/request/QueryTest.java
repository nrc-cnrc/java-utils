package ca.nrc.dtrc.elasticsearch.request;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class QueryTest {
	@Test
    public void test__Query__HappyPath() throws Exception {
		JSONObject jObj = new JSONObject()
			.put("bool", new JSONObject()
				.put("must", new JSONObject()
					.put("exists", new JSONObject()
						.put("field", "userName")
					)
				)
			);
		Query query = new Query(jObj);
		String expJson =
			"{\"query\": {\"bool\": {\"must\":{\"exists\":{\"field\":\"userName\"}}}}}";
		new AssertRequestBodyElement(query)
			.jsonEquals(expJson);
	}

}
