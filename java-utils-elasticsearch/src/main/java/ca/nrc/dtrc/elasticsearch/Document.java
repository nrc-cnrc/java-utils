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
		
	public abstract String keyFieldName();

	private String id = null;
		public String getId() {return this.id;}
		public void setId(String _id) {this.id = _id;}
		
	private String shortDescription = null;
		public void setShortDescription(String _shortDescription) {
			this.shortDescription = _shortDescription; 
		}
		public String getShortDescription() {return this.shortDescription; }
	
	private String longDescription = null;
		public void setLongDescription(String _longDescription) {
			this.longDescription = _longDescription;
		}
		public String getLongDescription() {return this.longDescription; }
			
	private String creationDate = null;
		public void setCreationDate(String _date) { this.creationDate = _date; }
		public String getCreationDate() {return this.creationDate; }
		
	private Map<String,Object> additionalFields = null;
		public void setAdditionalFields(Map<String,Object> _fields) {
			this.additionalFields = _fields;
		}
		public Map<String,Object> getAdditionalFields() {
			return this.additionalFields;
		}
	
	public Object getField(String fldName, boolean failIfNotFound, Object defaultVal) throws DocumentException {
		Object value = defaultVal;
		try {
			Field fld = this.getClass().getDeclaredField(fldName);
			fld.setAccessible(true);
			value = (Object)fld.get(this);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException exc) {
			if (failIfNotFound) throw new DocumentException(exc);
		}
		
		return value;
	}
	
	public Object getField(String fldName) throws DocumentException {
		return getField(fldName, true, null);
	}

	public Object getField(String fldName, boolean failIfNotFound) throws DocumentException {
		return getField(fldName, failIfNotFound, null);
	}
	
	
	public String defaultESDocType() {
		return this.getClass().getName();
	}
	
	public String toString() {
		return toString(null);
	}
	
	public String toString(Set<String> fieldsFilter) {
		String toStr = "";
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.convertValue(this, Map.class);
		for (String field: map.keySet()) {
			if (fieldsFilter != null && fieldsFilter.contains(field)) continue;
			toStr += "\n-------\n"+field+": "+map.get(field);
		}
		
		return toStr;
	}
	
	public String fingerprint() throws DocumentException {
		return fingerprint(100);
	}

	public String fingerprint(Integer minLength) throws DocumentException {
		return null;
	}

}
