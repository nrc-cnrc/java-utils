package ca.nrc.dtrc.elasticsearch.request;

import org.json.JSONObject;

import java.util.*;

public class Highlight extends RequestBodyElement {

    List<String> fieldsToHighlight = new ArrayList<String>();

    public Highlight hihglightField(String fldName) {
        fieldsToHighlight.add(fldName);
        return this;
    }

    @Override
    public JSONObject jsonObject() {
        JSONObject fields = new JSONObject();
        for (String field: fieldsToHighlight) {
            fields.put(field, new JSONObject());
        }
        JSONObject jsonObj = new JSONObject()
            .put("highlight", new JSONObject()
                .put("fields", fields));

        return jsonObj;
    }
}
