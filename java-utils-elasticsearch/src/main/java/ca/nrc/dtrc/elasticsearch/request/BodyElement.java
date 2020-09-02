package ca.nrc.dtrc.elasticsearch.request;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents parts of the body of an ElasticSearch request
 */
public class BodyElement {

    protected Map<String,Object> map = new HashMap<String,Object>();

    protected void init(Map<String,Object> _map) {
        this.map = _map;
    }

    public Map<String,Object> getMap() {
        return this.map;
    }
}
