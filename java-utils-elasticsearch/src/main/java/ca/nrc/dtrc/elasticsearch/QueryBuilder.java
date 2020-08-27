package ca.nrc.dtrc.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryBuilder {

    private static final String NO_VALUE_YET = "NO_VALUE_YET";

    Map<String,Object> queryMap = new HashMap<String,Object>();
    List<String> fieldsStack = new ArrayList<String>();

    public QueryBuilder addObject(String fldName) {
        return addObject(fldName, new HashMap<String,Object>());
    }

    public QueryBuilder addObject(String fldName, Object fldValue) {
        Map<String,Object> field = queryMap;
        for (String aFldName: fieldsStack) {
            field = (Map<String, Object>) field.get(aFldName);
        }
        field.put(fldName, fldValue);
        fieldsStack.add(fldName);

        return this;
    }

    public Map<String,Object> buildMap() {
        return queryMap;
    }
}
