package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.request.AssertRequestBodyElement;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class MustClauseTest {

	@Test
	public void test__MustClause__SingleElement() throws Exception {
		MustClause gotClause =
			new MustClause(new JSONObject()
				.put("exists", new JSONObject()
					.put("field", "id")
				)
			);
		System.out.println(gotClause.jsonObject().toString());
		String expJson = "{\"must\":[{\"exists\":{\"field\":\"id\"}}]}";
		new AssertRequestBodyElement(gotClause)
			.jsonEquals(expJson);

	}

	@Test
	public void test__MustClause__MultipleElements() throws Exception {
		MustClause gotClause =
			new MustClause(new JSONObject()
				.put("exists", new JSONObject()
					.put("field", "id")
				)
			);
		gotClause.add(new JSONObject()
			.put("match", new JSONObject()
				.put("type", new JSONObject()
					.put("query", "show")
				)
			)
		);
		System.out.println(gotClause.jsonObject().toString());
		String expJson = "{\"must\":[{\"exists\":{\"field\":\"id\"}},{\"match\":{\"type\":{\"query\":\"show\"}}}]}";
		new AssertRequestBodyElement(gotClause)
			.jsonEquals(expJson);

	}

}
