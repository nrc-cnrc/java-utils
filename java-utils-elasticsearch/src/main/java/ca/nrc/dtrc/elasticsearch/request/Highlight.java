package ca.nrc.dtrc.elasticsearch.request;

import java.util.HashMap;

public class Highlight extends RequestBodyElement {

    public void hihglightField(String fldName) {
        setValue(new HashMap<String,Object>(), "fields");
    }
}
