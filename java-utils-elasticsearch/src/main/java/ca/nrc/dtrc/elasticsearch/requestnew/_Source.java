package ca.nrc.dtrc.elasticsearch.requestnew;

import ca.nrc.dtrc.elasticsearch.requestnew.RequestBodyElement;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

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
