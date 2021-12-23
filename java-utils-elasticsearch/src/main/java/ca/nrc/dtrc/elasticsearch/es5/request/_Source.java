package ca.nrc.dtrc.elasticsearch.es5.request;

import org.json.JSONArray;
import org.json.JSONObject;

public class _Source extends RequestBodyElement {

    String[] fields = null;

    public _Source(String... _fields) {
        super();
        fields = _fields;
    }

    @Override
    public JSONObject jsonObject() {
        JSONObject jObj = new JSONObject();
        JSONArray jFieldsArr = new JSONArray();
        for (String aField: fields) {
            jFieldsArr.put(aField);
        }
        return new JSONObject().put("_source", jFieldsArr);
    }
}
