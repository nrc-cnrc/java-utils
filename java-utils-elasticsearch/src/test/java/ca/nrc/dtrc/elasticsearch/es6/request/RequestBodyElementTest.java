package ca.nrc.dtrc.elasticsearch.es6.request;

import ca.nrc.dtrc.elasticsearch.es6.request.query.MustClause;
import ca.nrc.dtrc.elasticsearch.es6.request.query.Query;
import ca.nrc.dtrc.elasticsearch.es6.request.query.QueryBoolean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;


public class RequestBodyElementTest {

    ///////////////////////////////////////////
    // DOCUMENTATION TESTS
    ///////////////////////////////////////////

    @Test
    public void test__RequestBodyElement__Synopsis() throws Exception {
        // Use RequestBodyElement to create and manipulate parts of an
        // ElasticSearch request body.
        //
        // This is more practical than manipulating a JSON string or a
        // JsonNode.
        //
        // For example, you can compose a generic element, feeding it
        // any kind of nested fields (even ones that are not valid ESFactory fields).
        //
        RequestElementGeneric genericElt =
            new RequestElementGeneric(new JSONObject()
                .put("eltname", new JSONObject()
                    .put("subfield", new JSONObject()
                        .put("sub_subfield", new JSONObject()
                            .put("attr1", "value1")
                            .put("attr2", new JSONArray())
                        )
                    )
                )
            );

        // There are however more  suclasses of element which are
        // designed to facilitate the creation of the most commonly used
        // ESFactory elements.
        //
        // For example:
        //
        Aggs aggsElt =
            new Aggs()
            .aggregate("avg_age", "avg", "age")
            .aggregate("avg_salaray", "avg", "salary");

        // Given a request element, you can obtain its content in the form of
        // either a json String or object
        //
        JSONObject aggsJsonObject = aggsElt.jsonObject();
        JsonString aggsJsonString = aggsElt.jsonString();


        // You can also merge several elements into a single one
        RequestBodyElement mergedElt =
            RequestBodyElement.mergeElements(genericElt, aggsElt);
    }

    ///////////////////////////////////////////
    // VERIFICATION TESTS
    ///////////////////////////////////////////

    @Test
    public void test__merge__HappyPath() throws Exception {
		Query query = new QueryBoolean(
			new MustClause(
				new JSONObject()
					.put("exists", new JSONObject()
						.put("field", "userName")
					)
			)
		);

		Aggs aggs =
			new Aggs()
			.aggregate("totalAge", "sum", "age");

		RequestElementGeneric merged =
			RequestBodyElement.mergeElements(query, aggs);
		String expJson =
			"{\n"+
			"  \"aggs\": {\n"+
			"    \"totalAge\": {"+
			"      \"sum\": {\n"+
			"        \"field\": \"age\"\n"+
			"      }\n"+
			"    }\n"+
			"  },\n"+
			"  \"query\": {\n"+
			"    \"bool\": {\n"+
			"      \"must\": [\n"+
			"        {\"exists\": {\"field\": \"userName\"}}\n"+
			"      ]\n"+
			"      }\n"+
			"    }\n"+
			"  }\n"+
			"}"
			;
		new AssertRequestBodyElement(merged)
			.jsonEquals(expJson);
    }
    
    /////////////////////////////////////
    // HELPER METHODS
    /////////////////////////////////////

}
