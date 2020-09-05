package ca.nrc.dtrc.elasticsearch.request;

import java.util.*;

public class Highlight extends RequestBodyElement {

    List<String> fieldsToHighlight = new ArrayList<String>();

    public Highlight hihglightField(String fldName) {
        fieldsToHighlight.add(fldName);
        return this;
    }

    @Override
    public Map<String,Object> getValue() {
        Map<String,Map<String,String>> fieldsMap =
            new HashMap<String,Map<String,String>>();
        for (String aFldName: fieldsToHighlight) {
            fieldsMap.put(aFldName, Collections.emptyMap());
        }
        setValue(fieldsMap, "fields");
        return _valueMap;
    }
}
