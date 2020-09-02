package ca.nrc.dtrc.elasticsearch.request;

import java.util.HashMap;

public class HighlightBody extends BodyElement {

    public void hihglightField(String fldName) {
        setValue(new HashMap<String,Object>(), "fields");
    }
}
