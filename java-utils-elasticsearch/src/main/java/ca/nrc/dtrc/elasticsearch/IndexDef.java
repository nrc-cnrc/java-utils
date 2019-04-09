package ca.nrc.dtrc.elasticsearch;

import java.util.HashMap;
import java.util.Map;

public class IndexDef {
	
	public Map<String,TypeDef> types = new HashMap<String,TypeDef>();

	public TypeDef getTypeDef(String typeName) {
		if (!types.containsKey(typeName)) {
			types.put(typeName, new TypeDef());
		}
		TypeDef tDef = types.get(typeName);

		return tDef;
	}

	public Map<String, Object> toMap() {
		Map<String,Object> iMap = new HashMap<String,Object>();
		
		Map<String,Object> mappings = new HashMap<String,Object>();
		iMap.put("mappings", mappings);
		{
			for (String typeName: types.keySet()) {
				TypeDef typeDef = getTypeDef(typeName);
				mappings.put(typeName, typeDef.toMap());
			}
		}
		
		// TODO Auto-generated method stub
		return iMap;
	}

	
}
