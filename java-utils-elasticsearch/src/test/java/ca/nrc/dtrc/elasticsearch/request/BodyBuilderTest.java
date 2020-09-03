package ca.nrc.dtrc.elasticsearch.request;

import ca.nrc.testing.AssertObject;
import org.junit.Test;

import java.io.IOException;

public class BodyBuilderTest {

    //////////////////////////////////////////
    // DOCUMENTATION TESTS
    //////////////////////////////////////////

    @Test
    public void test__BodyBuilder__Synopsis() {
        //
        // Use a BodyBuilder to construct elements of the body of an
        // ElasticSearch request.
        //
        // For example
        //
        QueryBody query = new QueryBody();
        new BodyBuilder<QueryBody>(query)
            .addObject("query")
                .addObject("bool")
                    .addObject("must")
                        .addObject("exists")
                            .addObject("field", "userName")
                            .closeObject()
                        .closeObject()
                    .closeObject()
                .closeObject()
            .closeObject()

            // After build(), the query object will be populated with all the
            // above defined fields.
            .build();

        AggrBody aggregations = new AggrBody();
        new BodyBuilder<AggrBody>(aggregations)
            .addObject("aggs")
                .addObject("totalAge")
                    .addObject("sum")
                        .addObject("field", "age")
        // After build(), the aggregation object will be populated with all the
        // above defined fields.
        .build()
        ;
    }

    /////////////////////////////////
    // VERIFICATION TESTS
    /////////////////////////////////

    @Test
    public void test__BodyBuilder__HappyPath() throws Exception {
        QueryBody query = new QueryBody();
        new BodyBuilder<QueryBody>(query)
            .addObject("query")
                .addObject("bool")
                    .addObject("must")
                        .addObject("exists")
                            .addObject("field", "userName")
            .build();

        String expJson =
            "{\n"+
            "  \"query\": {\n"+
            "    \"bool\": {\n"+
            "      \"must\": {\n"+
            "        \"exists\": {\n"+
            "          \"field\": \"userName\"\n"+
            "          }\n"+
            "        }\n"+
            "      }\n"+
            "    }\n"+
            "  }\n"+
            "}"
            ;
        assertJsonEquals(expJson, query);
    }

    /////////////////////////////////////
    // HELPER METHODS
    /////////////////////////////////////

    private void assertJsonEquals(String expJson, BodyElement gotBodyElt) throws IOException {

        AssertObject.assertEqualsJsonCompare(
            "JSON string was not as expected", expJson, gotBodyElt.map);
    }
}
