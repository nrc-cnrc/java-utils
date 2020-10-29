package ca.nrc.dtrc.elasticsearch.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Size extends RequestBodyElement {

    int size = 10;

    public Size(int _size) {
        super();
        size = _size;
    }

    @Override
    public Map<String,Object> getValue() {
        setOpenedAttr(size);
        return _valueMap;
    }
}
