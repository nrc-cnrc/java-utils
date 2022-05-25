package ca.nrc.dtrc.elasticsearch.index;

import ca.nrc.dtrc.elasticsearch.FieldDef;
import ca.nrc.dtrc.elasticsearch.TypeDef;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class IndexDef {
	
	public String indexName = null;
	
	public Map<String, TypeDef> types = new HashMap<String,TypeDef>();
	public Integer totalFieldsLimit = null;
	
	public IndexDef() {
		init__IndexDef();
	}

	public IndexDef(String _name) {
		this.indexName = _name;
		init__IndexDef();
	}

	private void init__IndexDef() {
		setDefaultMappings();
		return;
	}

	public void setDefaultMappings() {
		// Setup the 'id' and 'idWithoutType' fields in such a way that they can
		// be used for sorting
		getTypeDef("*").getFieldDef("id").type = FieldDef.Types.keyword;
		getTypeDef("*").getFieldDef("idWithoutType").type = FieldDef.Types.keyword;
		getTypeDef("*").getFieldDef("type").type = FieldDef.Types.keyword;
		getTypeDef("*").getFieldDef("lang").type = FieldDef.Types.keyword;
		return;
	}

	public IndexDef setTotalFieldsLimit(Integer limit) {
		totalFieldsLimit = limit;

		return this;
	}

	public TypeDef getTypeDef(String typeName) {
		if (!types.containsKey(typeName)) {
			types.put(typeName, new TypeDef());
		}
		TypeDef tDef = types.get(typeName);

		return tDef;
	}

	public Map<String, Object> settingsAsProps() {
		Map<String,Object> props = new HashMap<String,Object>();
		
		if (totalFieldsLimit != null) {
			props.put("index.mapping.total_fields.limit", totalFieldsLimit);
		} else {
			props.put("index.mapping.total_fields.limit", 1000);
		}
		
		Map<String,Object> isMap = new HashMap<String,Object>();
		isMap.put("settings", props);
		
		return isMap;
	}

	public Map<String, Object> settingsAsTree() {
		Map<String,Object> props = settingsAsProps();
		Map<String,Object> tree = props2tree((Map<String, Object>) props.get("settings"));

		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put("settings", tree);
		
		return settings;
	}
	
	public JSONObject jsonMappings() {
		JSONObject mappings = new JSONObject();
		{
			for (String typeName: types.keySet()) {
				TypeDef typeDef = getTypeDef(typeName);
				mappings.put(typeName, typeDef.toJson());
			}
		}
		
		return mappings;
	}

	public IndexDef loadSettings(Map<String, Object> settings) throws IndexDefException {
		for (String aSetting: settings.keySet()) {
			Object value = settings.get(aSetting);
			if (aSetting.equals("index.mapping.total_fields.limit")) {
				this.totalFieldsLimit = Integer.parseInt((String) value);
			}				
		}
		
		return this;
	}

	public IndexDef loadMappings(Map<String, Object> mappings) {
		return this;
	}

	public IndexDef setFieldMapping(String field, String type) {
		return setFieldMapping(field, type, (Map<String,Object>)null);
	}

	public IndexDef setFieldMapping(String field, String type,
		Map<String,Object> additionalAttributes) {

		return this;
	}

	public static Map<String, Object> tree2props(Map<String, Object> tree) {
		Map<String, Object> props = new HashMap<String, Object>();
		tree2props(tree, "", props);
		
		return props;
	}
	
	static Map<String, Object> tree2props(Object node, String parentPropName, Map<String,Object> props) {
		
		if (! (node instanceof Map<?,?>)) {
			props.put(parentPropName, node);
		} else {
			Map<String,Object> map = (Map<String,Object>) node;
			if (!parentPropName.isEmpty()) {
				parentPropName += ".";
			}
			for (String key: map.keySet()) {
				tree2props(map.get(key), parentPropName+key, props);
			}
		}
		
		return props;
	}
	
	public static Map<String, Object> props2tree(Map<String, Object> props) {
		Map<String,Object> map = new HashMap<String,Object>();
		
		for (String propName: props.keySet()) {
			Object propVal = props.get(propName);
			String[] nodeNames = propName.split("\\.");
			Map<String,Object> currNode = map;
			for (int ii=0; ii < nodeNames.length; ii++) {
				String aNodeName = nodeNames[ii];
				if (ii == nodeNames.length-1) {
					// We have reached the leaf where to put
					// the property value
					currNode.put(aNodeName, propVal);
				} else {
					if (!currNode.containsKey(aNodeName)) {
						currNode.put(aNodeName, new HashMap<String,Object>());
					}
					currNode = (Map<String, Object>) currNode.get(aNodeName);
				}
			}
		}
		
		return map;
	}
}
