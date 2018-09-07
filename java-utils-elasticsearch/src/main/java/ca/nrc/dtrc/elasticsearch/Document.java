package ca.nrc.dtrc.elasticsearch;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.json.PrettyPrinter;

public class Document {
	
	// This makes it possible for a document collection to 
	// contain documents that are in different languages
	public boolean _detect_language = true;
	
	public String lang = "en";
		
	public String keyFieldName() {return "id";};

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
		
	private Map<String,Object> additionalFields = new HashMap<String,Object>();
		public void setAdditionalFields(Map<String,Object> _fields) {
			this.additionalFields = _fields;
		}
		public Map<String,Object> getAdditionalFields() {
			return this.additionalFields;
		}
		@JsonIgnore
		public void setAnAdditionalField(String _fldName, Object _fldValue) {
			this.additionalFields.put(_fldName, _fldValue);
		}
		@JsonIgnore
		public Object getAnAdditionalField(String _fldName) {
			Object value = this.additionalFields.get(_fldName);
			return value;
		}
		
	public Document() {
		initialize(null);
	}
		
	public Document(String _id) {
		initialize(_id);
	}
	
	private void initialize(String _id) {
		this.setId(_id);
	}
	
	@JsonIgnore
	public String getContent() {
		String content = PrettyPrinter.print(this);
		return content;
	}

	
	public Object getField(String fldName, boolean failIfNotFound, Object defaultVal) throws DocumentException {
		Object value = defaultVal;
		if (fldName.startsWith("additionalFields.")) {
			// This is a dynamically set, runtie field
			fldName = fldName.substring(17);
			value = additionalFields.get(fldName);
		} else {
			// This is a member attribute field
			try {
				Field fld = this.getClass().getDeclaredField(fldName);
				fld.setAccessible(true);
				value = (Object)fld.get(this);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException exc) {
				if (failIfNotFound) throw new DocumentException(exc);
			}
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
	
	@JsonIgnore
	public LocalDate getCreationLocalDate() {
		LocalDate date = null;
		String dateStr = getCreationDate();
		if (dateStr != null) {
			date = LocalDate.parse(dateStr);
		}
		
		return date;
	}

}
