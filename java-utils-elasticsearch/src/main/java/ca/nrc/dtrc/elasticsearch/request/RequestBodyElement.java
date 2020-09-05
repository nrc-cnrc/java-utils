package ca.nrc.dtrc.elasticsearch.request;

import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Class that represents parts of the body of an ElasticSearch request
 */
public class RequestBodyElement {

    private String name = null;

    protected Map<String,Object> _valueMap = null;

    protected Stack<String> fieldsStack = new Stack<String>();

    public RequestBodyElement() {
        initRequestBodyElement(null);
    }

    public RequestBodyElement(String _name) {
        initRequestBodyElement(_name);
    }

    protected void initRequestBodyElement(String _name) {
        if (_name != null) {
            this.name = _name;
        } else {
            this.name = this.getClass().getSimpleName().toLowerCase();
        }

        this._valueMap = new HashMap<String,Object>();
        this._valueMap.put(name, new HashMap<String,Object>());
        fieldsStack.push(name);

        return;
    }

    public String getName() {
        return name;
    }

    public Map<String,Object> getValue() {
        return _valueMap;
    }


    public static Map<String,Object> merge(RequestBodyElement... bodyElts) throws ElasticSearchException {
        Map<String,Object> mergedMap = new HashMap<String,Object>();
        mergeIntoMap(mergedMap, bodyElts);

        return mergedMap;
    }

    public static void mergeIntoMap(
        Map<String,Object> mergedMap, RequestBodyElement... bodyElts) {

        for (RequestBodyElement elt: bodyElts) {
            System.out.println(
                    "-- RequestBodyElement.mergeIntoMap: processing elt="+
                            ((elt != null)?elt.getName():"null"));
            Map<String,Object> eltMap = elt.getValue();
            for (Map.Entry<String,Object> eltEntry: eltMap.entrySet()) {
                mergedMap.put(eltEntry.getKey(), eltEntry.getValue());
            }
            System.out.println(
                    "-- RequestBodyElement.mergeIntoMap: DONE processing elt="+
                            ((elt != null)?elt.getName():"null"));
        }

        return;
    }

    protected void set_valueMap(Map<String,Object> _map) {
        this._valueMap = _map;
    }

    @JsonIgnore
    public void setValue(Object value, String... path) {
        Map<String,Object> parent = parentMap(path);
        parent.put(path[path.length-1], value);
    }


    public RequestBodyElement openAttr(String fldName) {
        fieldsStack.push(fldName);
        return this;
    }


    public RequestBodyElement setOpenedAttr(Object value) {
        Map<String,Object> parentMap =
            parentMap(fieldsStack.toArray(new String[0]));

        // Set the attribute's value
        parentMap.put(fieldsStack.lastElement(), value);

        return this;
    }

    /**
     * Locate the parent of the attribute in the nested value()
     */
    protected Map<String,Object> parentMap(String... path) {
        Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.request.RequestBodyElement");
        if (tLogger.isTraceEnabled()) {
            tLogger.trace("invoked with path="+String.join(",", path));
        }
        Map<String,Object> parent = _valueMap;

        String[] pathWithName = path;
        if (pathWithName.length == 0) {
            pathWithName = new String[] {getName()};
        } else {
            if (!path[0].equals(getName())) {
                pathWithName = new String[path.length+1];
                pathWithName[0] = getName();
                for (int ii=1; ii < pathWithName.length; ii++) {
                    pathWithName[ii] = path[ii-1];
                }
            }

            if (tLogger.isTraceEnabled()) {
                tLogger.trace("invoked with pathWithName="+String.join(",", pathWithName));
            }

            int count = 0;
            for (String fldName : pathWithName) {
                tLogger.trace("looking at fldName="+fldName);
                count++;
                if (count == pathWithName.length) {
                    break;
                }
                if (!parent.containsKey(fldName)) {
                    parent.put(fldName, new HashMap<String, Object>());
                }
                parent = (Map<String, Object>) parent.get(fldName);
            }
        }

        return parent;
    }

    public RequestBodyElement closeAttr() {
        fieldsStack.pop();
        return this;
    }


    public RequestBodyElement closeAll() {
        fieldsStack = new Stack<String>();
        return this;
    }

    public JsonString jsonString() throws ElasticSearchException {
        String json = null;
        try {
            Map<String,Object> eltMap = new HashMap<String,Object>();
            String name = getName();
            eltMap.put(name, getValue().get(name));
            json = new ObjectMapper().writeValueAsString(eltMap);
        } catch (JsonProcessingException e) {
            throw new ElasticSearchException(e);
        }
        return new JsonString(json);
    }
}
