package ca.nrc.dtrc.elasticsearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TypeDef {
	
	public Map<String,FieldDef> fields = new HashMap<String,FieldDef>();

	@JsonIgnore
	public FieldDef getFieldDef(String fldName) {
		if (!fields.containsKey(fldName)) {
			fields.put(fldName, new FieldDef());
		} 
		FieldDef fDef = fields.get(fldName);
		
		return fDef;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		JSONObject jsonProperties = new JSONObject();
		json.put("properties", jsonProperties);
		for (String fldName: fields.keySet()) {
			FieldDef fDef = getFieldDef(fldName);
			jsonProperties.put(fldName, fDef.toJson());
		}

		return json;
	}

}
