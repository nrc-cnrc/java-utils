package ca.nrc.dtrc.elasticsearch.requestnew;

import ca.nrc.dtrc.elasticsearch.requestnew.RequestBodyElement;
import org.json.JSONObject;

import java.util.Map;

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
