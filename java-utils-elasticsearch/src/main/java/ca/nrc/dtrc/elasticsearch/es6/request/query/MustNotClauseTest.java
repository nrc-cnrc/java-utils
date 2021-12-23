package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.request.AssertRequestBodyElement;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class MustNotClauseTest {

	@Test
	public void test__MustNotClause__SingleElement() throws Exception {
		MustNotClause gotClause =
			new MustNotClause(new JSONObject()
				.put("exists", new JSONObject()
					.put("field", "id")
				)
			);
		System.out.println(gotClause.jsonObject().toString());
		String expJson = "{\"must_not\":[{\"exists\":{\"field\":\"id\"}}]}";
		new AssertRequestBodyElement(gotClause)
			.jsonEquals(expJson);

	}

	@Test
	public void test__MustNotClause__MultipleElements() throws Exception {
		MustNotClause gotClause =
			new MustNotClause(new JSONObject()
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
		String expJson = "{\"must_not\":[{\"exists\":{\"field\":\"id\"}},{\"match\":{\"type\":{\"query\":\"show\"}}}]}";
		new AssertRequestBodyElement(gotClause)
			.jsonEquals(expJson);

	}

}
