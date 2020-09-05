package ca.nrc.dtrc.elasticsearch.request;

import ca.nrc.datastructure.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Sort sortBy(String fldName, Order order) {
        criteria.add(Pair.of(fldName, order));
        return this;
    }

    @Override
    public Map<String,Object> getValue() {
        List<Map<String,String>> criteriaLst =
                new ArrayList<Map<String,String>>();
        for (Pair<String,Order> crit: criteria) {
            Map<String,String> critMap = new HashMap<String,String>();
            critMap.put(crit.getFirst(), crit.getSecond().toString());
            criteriaLst.add(critMap);
        }
        this._valueMap.put(getName(), criteriaLst);
        return _valueMap;
    }
}
