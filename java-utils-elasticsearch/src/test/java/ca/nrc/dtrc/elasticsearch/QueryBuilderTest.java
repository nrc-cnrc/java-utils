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
//         Use this class to build complex ES queries like this:
//
//                {
//                    "query": {
//                    "bool": {
//                        "must_not": {
//                            "exists": {
//                                "field": "username"
//                            }
//                        }
//                    }
//                }
        Map<String,Object> queryMap =
            new QueryBuilder()
                .addObject("query")
                    .addObject("bool")
                        .addObject("must_not")
                            .addObject("exists")
                                .addObject("field", "userName")
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
                .addObject("must_not")
                .addObject("exists")
                .addObject("field", "userName")
                .buildMap();

        String expJson =
            "{\"query\": {\"bool\": {\"must_not\": {\"exists\": {\"field\": \"userName\"}}}}}";
        assertJsonEquals(expJson, queryMap);
    }

    /////////////////////////////////////
    // HELPER METHODS
    /////////////////////////////////////

    private void assertJsonEquals(String expJson, Map<String, Object> gotQueryMap) throws IOException {
        AssertObject.assertEqualsJsonCompare("JSON string was not as expected", expJson, gotQueryMap);
    }
}
