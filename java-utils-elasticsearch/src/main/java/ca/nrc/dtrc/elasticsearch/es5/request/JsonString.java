package ca.nrc.dtrc.elasticsearch.es5.request;

/**
 * Class that wraps a string that is a JSON serialisation of some object
 * or data structure.
 *
 * Its only purpose is to distinguish between a regular string and a JSON
 * string in method signatures. For example:
 *
 *   search(JsonString jsonQuery, etc...)
 *      AND
 *   search(String freeformQuery, etc...)
 */
public class JsonString  {

    private String jsonString;

    public JsonString(String _jsonString) {
        this.jsonString = _jsonString;
    }

    public String toString() {
        return jsonString;
    }
 }
