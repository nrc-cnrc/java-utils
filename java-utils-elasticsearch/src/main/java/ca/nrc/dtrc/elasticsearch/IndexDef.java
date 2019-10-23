package ca.nrc.dtrc.elasticsearch;

import java.util.HashMap;
import java.util.Map;

public class IndexDef {
	
	public String indexName = null;
	
	public Map<String,TypeDef> types = new HashMap<String,TypeDef>();
	public Integer totalFieldsLimit = null;
	
	public IndexDef(String _name) {
		this.indexName = _name;
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

	public Map<String, Object> indexSettings() {
		Map<String,Object> isMap = new HashMap<String,Object>();
		
		if (totalFieldsLimit != null) {
			isMap.put("index.mapping.total_fields.limit", totalFieldsLimit);
		} else {
			isMap.put("index.mapping.total_fields.limit", 1000);
		}
		
		return isMap;
	}

	
	public Map<String, Object> indexMappings() {
		Map<String,Object> imMap = new HashMap<String,Object>();
		
		Map<String,Object> mappings = new HashMap<String,Object>();
		imMap.put("mappings", mappings);
		{
			for (String typeName: types.keySet()) {
				TypeDef typeDef = getTypeDef(typeName);
				mappings.put(typeName, typeDef.toMap());
			}
		}
		
		// TODO Auto-generated method stub
		return imMap;
	}

	public IndexDef loadSettings(Map<String, Object> settings) {
		Map<String,Object> field = (Map<String, Object>) settings.get(indexName);
		while (true)
		{
			if (!field.containsKey("settings")) break;
			field = (Map<String, Object>) field.get("settings");
			
			if (!field.containsKey("index")) break;			
			field = (Map<String, Object>) field.get("index");
			
			if (!field.containsKey("mapping")) break;			
			field = (Map<String, Object>) field.get("mapping");
			
			if (!field.containsKey("total_fields")) break;
			field = (Map<String, Object>) field.get("total_fields");
			
			if (!field.containsKey("limit")) break;
			this.totalFieldsLimit = (Integer) Integer.parseInt((String) field.get("limit"));
			
			break;
		}
		
		return this;
	}

	public IndexDef loadMappings(Map<String, Object> mappings) {
		
		
		return this;
	}

	
}
