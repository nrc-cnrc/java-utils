package ca.nrc.dtrc.elasticsearch.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents parts of the body of an ElasticSearch request
 */
public class RequestBodyElement {

    protected Map<String,Object> map = new HashMap<String,Object>();

    protected void init(Map<String,Object> _map) {
        this.map = _map;
    }

    public Map<String,Object> getMap() {
        return this.map;
    }

    @JsonIgnore
    public void setValue(Object value, Object... path) {
        Map<String,Object> obj = this.map;
        for (int ii=0; ii < path.length - 1; ii++) {
            obj = (Map<String,Object>) (obj.get(path[ii].toString()));
        }
        obj.put(path[path.length-1].toString(), value);
    }
}
