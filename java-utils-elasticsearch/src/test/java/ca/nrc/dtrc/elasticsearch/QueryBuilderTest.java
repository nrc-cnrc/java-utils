package ca.nrc.dtrc.elasticsearch;

import ca.nrc.testing.AssertObject;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class QueryBuilderTest {

    /////////////////////////////////
    // DOCUMENTATION TESTS
    /////////////////////////////////

    @Test
    public void test__QueryBuilder__Synopsis() throws ElasticSearchException {

        Map<String,Object> queryMap =
            new QueryBuilder()
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
                .addObject("aggs")
                    .addObject("totalAge")
                        .addObject("sum")
                            .addObject("field", "age")

                 // Note: If you don't close the last couple objects that were
                 // added to the stack, buildMap() will close
                .buildMap();
    }

    /////////////////////////////////
    // VERIFICATION TESTS
    /////////////////////////////////

    @Test
    public void test__QueryBuilder__HappyPath() throws Exception {

    Map<String,Object> queryMap =
        new QueryBuilder()
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
            .addObject("aggs")
                .addObject("totalAge")
                    .addObject("sum")
                        .addObject("field", "age")

            .buildMap();

        String expJson =
            "{\n"+
            "  \"aggs\": {\n"+
            "    \"totalAge\": {\n"+
            "      \"sum\": {\n"+
            "        \"field\": \"age\"\n"+
            "      }\n"+
            "    }\n"+
            "  },\n"+
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
        assertJsonEquals(expJson, queryMap);
    }

    /////////////////////////////////////
    // HELPER METHODS
    /////////////////////////////////////

    private void assertJsonEquals(String expJson, Map<String, Object> gotQueryMap) throws IOException {
        AssertObject.assertEqualsJsonCompare("JSON string was not as expected", expJson, gotQueryMap);
    }
}
