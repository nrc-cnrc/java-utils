package ca.nrc.dtrc.elasticsearch.es6.request;

import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Aggs extends RequestBodyElement {
    List<Triple<String,String,String>> aggregations =
        new ArrayList<Triple<String,String,String>>();

    public Aggs aggregate(String aggName, String aggFct, String aggField) {
        aggregations.add(Triple.of(aggName, aggFct, aggField));
        return this;
    }

    @Override
    public JSONObject jsonObject() {
        JSONObject aggs = new JSONObject();
        for (Triple<String,String,String> anAgg: aggregations) {
            aggs
                .put(anAgg.getLeft(), new JSONObject()
                    .put(anAgg.getMiddle(), new JSONObject()
                        .put("field", anAgg.getRight())))
                ;
        }


        JSONObject jObj = new JSONObject().put("aggs", aggs);

        return jObj;
    }
}
