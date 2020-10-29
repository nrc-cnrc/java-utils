package ca.nrc.dtrc.elasticsearch.request;

import java.util.Map;

public class _Source extends RequestBodyElement {

    String[] fields = null;

    public _Source(String... _fields) {
        super();
        fields = _fields;
    }

    @Override
    public Map<String,Object> getValue() {
        setOpenedAttr(fields);
        return _valueMap;
    }
}
