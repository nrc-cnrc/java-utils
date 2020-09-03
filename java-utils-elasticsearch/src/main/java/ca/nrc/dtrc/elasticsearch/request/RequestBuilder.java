package ca.nrc.dtrc.elasticsearch.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class RequestBuilder<T extends RequestBodyElement> {
    private static final String NO_VALUE_YET = "NO_VALUE_YET";

    Map<String,Object> map = new HashMap<String,Object>();
    Stack<String> fieldsStack = new Stack<String>();
    T instance = null;

    public RequestBuilder(T _instance) {
        instance = _instance;
    }

    public RequestBuilder addObject(String fldName) {
        return addObject(fldName, new HashMap<String,Object>());
    }

    public RequestBuilder addObject(String fldName, Object fldValue) {
        Map<String,Object> field = map;
        for (String aFldName: fieldsStack) {
            field = (Map<String, Object>) field.get(aFldName);
        }
        field.put(fldName, fldValue);
        fieldsStack.push(fldName);

        return this;
    }

    public RequestBuilder closeObject() {
        fieldsStack.pop();
        return this;
    }

    public T build() {
        instance.init(map);
        return instance;
    }
}
