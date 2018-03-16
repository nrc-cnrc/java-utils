package ca.nrc.dtrc.elasticsearch;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Document {
	
	// This makes it possible for a document collection to 
	// contain documents that are in different languages
	public boolean _detect_language = true;
	
	public String lang = "en";
		
	@JsonIgnore
	public abstract String getKeyFieldName();

	@JsonIgnore	
	public abstract String getKey();
	
	public Object getFieldValueByName(String fldName) throws DocumentException {
		Object value = null;
		try {
			Field fld = this.getClass().getDeclaredField(fldName);
			fld.setAccessible(true);
			value = (Object)fld.get(this);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException exc) {
			throw new DocumentException(exc);
		}
		
		return value;
	}
	
	public String toString() {
		String toStr = "";
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.convertValue(this, Map.class);
		for (String field: map.keySet()) {
			toStr += "\n-------\n"+field+": "+map.get(field);
		}
		
		return toStr;
	}

}
