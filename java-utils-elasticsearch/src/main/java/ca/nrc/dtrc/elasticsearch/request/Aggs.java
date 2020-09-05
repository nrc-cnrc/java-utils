package ca.nrc.dtrc.elasticsearch.request;

import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Aggs extends RequestBodyElement {
    List<Triple<String,String,String>> aggregations =
        new ArrayList<Triple<String,String,String>>();

    public Aggs aggregate(String aggName, String aggFct, String aggField) {
        aggregations.add(Triple.of(aggName, aggFct, aggField));
        return this;
    }

    @Override
    public Map<String,Object> getValue() {
        for (Triple<String,String,String> anAgg: aggregations) {
            setValue(
                anAgg.getRight(),
                anAgg.getLeft(),
                anAgg.getMiddle(),
                "field");
        }

        return _valueMap;
    }
}
