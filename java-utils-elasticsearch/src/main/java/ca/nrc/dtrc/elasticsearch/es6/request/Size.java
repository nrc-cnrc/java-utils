package ca.nrc.dtrc.elasticsearch.es6.request;

import org.json.JSONObject;

public class Size extends RequestBodyElement {

    int size = 10;

    public Size(int _size) {
        super();
        size = _size;
    }

    @Override
    public JSONObject jsonObject() {
        return new JSONObject().put("size", size);
    }
}
