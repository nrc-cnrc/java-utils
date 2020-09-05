package ca.nrc.testing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class AssertJson {
    public static void assertJsonStringsAreEquivalent(
        String mess, String expJson, String gotJson) throws Exception {

        Map<String,Object> expMap = deserialize(expJson);
        Map<String,Object> gotMap = deserialize(gotJson);
        AssertObject.assertDeepEquals(
    mess+"\nThe two json strings yielded different objects.",
            expMap, gotMap);
    }

    private static Map<String, Object> deserialize(String json)
        throws Exception {
        Map<String,Object> map = new HashMap<String,Object>();
        map = new ObjectMapper().readValue(json, map.getClass());
        return map;
    }
}
