package ca.nrc.dtrc.elasticsearch.es5;

import java.util.HashMap;
import java.util.Map;

public class TypeDef {
	
	public Map<String,FieldDef> fields = new HashMap<String,FieldDef>();

	public FieldDef getFieldDef(String fldName) {
		
		if (!fields.containsKey(fldName)) {
			fields.put(fldName, new FieldDef());
		} 
		FieldDef fDef = fields.get(fldName);
		
		return fDef;
	}

	public Object toMap() {
		Map<String,Object> tMap = new HashMap<String,Object>();
		Map<String,Object> fieldsMap = new HashMap<String,Object>();
		tMap.put("properties", fieldsMap);
		{
			for (String fldName: fields.keySet()) {
				FieldDef fDef = getFieldDef(fldName);
				fieldsMap.put(fldName, fDef.toMap());
			}
		}
		
		return tMap;
	}

}
