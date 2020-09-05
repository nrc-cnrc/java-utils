package ca.nrc.dtrc.elasticsearch.request;

import ca.nrc.testing.AssertJson;
import ca.nrc.testing.AssertObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Create a "query" body element using a generic RequestBodyElement.
        //
        RequestBodyElement queryElt =
            new RequestBodyElement("query")
                .openAttr("bool")
                    .openAttr("must")
                        .openAttr("exists")
                            .openAttr("field")
                                .setOpenedAttr("userName")
                            .closeAttr()
                        .closeAttr()
                    .closeAttr()
                .closeAttr();

        // Create a "aggs" body element using a generic RequestBodyElement.
        //
        RequestBodyElement aggsElt =
            new RequestBodyElement("query")
                .openAttr("sum")
                    .openAttr("field")
                        .setOpenedAttr("age")

            // Bring the "cursor" back to the top level of the element.
            .closeAll();

        // After you have created a body element, you can obtain its JsonString
        // as follows:
        //
        JsonString queryJson = queryElt.jsonString();

        // You can also merge several body elements into a single
        // Map<String,Object> that can then be serialized and fed to
        // Elastic Search
        //
        Map<String,Object> requestMap = RequestBodyElement.merge(queryElt, aggsElt);

        // You can also obtain the
        // "Generic" RequestBodyElement provide the flexibility of constructing
        // any part of any ES request body, but they can be a bit awkward to use.
        //
        // So most of the time, you will want to used a "typed" subclass that is
        // designed specifically to represent a particular type of ES request
        // body element
        //
        // For example, to build a "sort" element, you could do this:
        //
        Sort sort = new Sort()
            .sortBy("age", Sort.Order.desc)
            .sortBy("revenue", Sort.Order.asc);

        // The above is equivalent to doing this:
        //
        List<Map<String,String>> sortCriteria =
            new ArrayList<Map<String,String>>();
        Map<String,String> criterion = new HashMap<String,String>();
        criterion.put("age", "desc");
        sortCriteria.add(criterion);
        criterion = new HashMap<String,String>();
        criterion.put("revenue", "asc");
        sortCriteria.add(criterion);
        sort = new Sort();
        sort.setOpenedAttr(sortCriteria);
    }

    ///////////////////////////////////////////
    // VERIFICATION TESTS
    ///////////////////////////////////////////

    @Test
    public void test__RequestBodyElement__HappyPath() throws Exception {
        RequestBodyElement queryElt =
            new RequestBodyElement("query")
                .openAttr("bool")
                    .openAttr("must")
                        .openAttr("exists")
                            .openAttr("field")
                                .setOpenedAttr("userName");

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
        assertJsonEquals(expJson, queryElt);
    }

    @Test
    public void test__merge__HappyPath() throws Exception {
        RequestBodyElement queryElt =
            new RequestBodyElement("query")
                .openAttr("bool")
                    .openAttr("must")
                        .openAttr("exists")
                            .openAttr("field")
                            .setOpenedAttr("userName");

        RequestBodyElement aggsElt =
            new RequestBodyElement("aggs")
                .openAttr("totalAge")
                    .openAttr("sum")
                        .openAttr("field")
                        .setOpenedAttr("age");

        Map<String,Object> merged = RequestBodyElement.merge(queryElt, aggsElt);
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
        assertJsonEquals(expJson, merged);
    }
    
    /////////////////////////////////////
    // HELPER METHODS
    /////////////////////////////////////

    private void assertJsonEquals(String expJson, RequestBodyElement gotBodyElt)
        throws Exception {
        assertJsonEquals(expJson, gotBodyElt.jsonString());
    }
    private void assertJsonEquals(String expJson, JsonString gotJsonString)
        throws Exception {
        AssertJson.assertJsonStringsAreEquivalent(
            "JsonString was not as expected string was not as expected",
            expJson, gotJsonString.toString());
    }

    private void assertJsonEquals(String expJson, Map<String,Object> gotMap)
            throws Exception {

        Map<String,Object> expMap = new HashMap<String,Object>();
        expMap = new ObjectMapper().readValue(expJson, expMap.getClass());
        AssertObject.assertDeepEquals(
            "Map was not as expected string was not as expected",
            expMap, gotMap);
    }

}
