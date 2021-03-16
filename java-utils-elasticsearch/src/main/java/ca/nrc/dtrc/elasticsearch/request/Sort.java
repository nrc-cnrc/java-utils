package ca.nrc.dtrc.elasticsearch.request;

import ca.nrc.datastructure.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sort extends RequestBodyElement {

    public static enum Order {asc, desc};

    List<Pair<String,Order>> criteria =
        new ArrayList<Pair<String,Order>>();

    public Sort() {
        super();
    }

    public Sort(List<Pair<String,Order>> _criteria) {
        super();
        criteria = _criteria;
    }

    public Sort(Pair<String,Order>... _critArray) {
        super();
        List<Pair<String,Order>> critList =
            new ArrayList<Pair<String, Order>>();
        if (_critArray != null) {
            Collections.addAll(criteria, _critArray);
        }
    }

    public Sort sortBy(String fldName, Order order) {
        criteria.add(Pair.of(fldName, order));
        return this;
    }

    @Override
    public JSONObject jsonObject() {
        JSONObject jsonObj = new JSONObject();
        JSONArray criteriaArr = new JSONArray();
        for (Pair<String,Order> crit: criteria) {
            JSONObject critObj =
            new JSONObject().put(crit.getFirst(), crit.getSecond().toString());
            criteriaArr.put(critObj);
        }
        jsonObj.put("sort", criteriaArr);
        return jsonObj;
    }
}
