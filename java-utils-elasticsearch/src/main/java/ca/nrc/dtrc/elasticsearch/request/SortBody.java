package ca.nrc.dtrc.elasticsearch.request;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SortBody extends BodyElement {

    public static enum SortOrder {incr, decr};

    List<String[]> criteria =
        new ArrayList<String[]>();

    public SortBody sortBy(String fldName, SortOrder order) {
        criteria.add(new String[] {fldName, order.toString()});
        return this;
    }

    @Override
    public Map<String,Object> getMap() {
        map = new HashMap<String,Object>();
        map.put("fields", criteria);
        return map;
    }
}
