package ca.nrc.dtrc.elasticsearch;

import java.util.HashMap;
import java.util.Map;

//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import com.fasterxml.jackson.databind.node.ObjectNode;

/*
 * This class of Document does not require the fields to be defined at 
 * compile time.
 */

public class Document_DynTyped extends Document {
	
	private Map<String,Object> fields = null;
		public void setFields(Map<String,Object> _fields) {this.fields = _fields;}
		public Map<String,Object> getFields() {return this.fields;}
		
	private String idFieldName = null;
		public void setIdFieldName(String _idFieldName) {this.idFieldName = _idFieldName;}
		public String getIdFieldName() {return this.idFieldName; }


	public Document_DynTyped(String _idFieldName, String _idValue) {
		initialize(_idFieldName, _idValue, null);
	}

	public Document_DynTyped(String _idFieldName, String _idValue, Map<String,Object> _fields) {
		initialize(_idFieldName, _idValue, _fields);
	}
	
	
	public Document_DynTyped(Map<String, Object> _fields) {
		String _idFieldName = (String)_fields.get("Document_DynTyped.idFieldName");
		String _id = (String)_fields.get(_idFieldName);
		Map<String,Object> fieldsFiltered = new HashMap<String,Object>();
		for (String fldName: _fields.keySet()) {
			if (fldName.equals("Document_DynTyped.idFieldName")) continue;
			fieldsFiltered.put(fldName, _fields.get(fldName));
		}
		initialize(_idFieldName, _id, fieldsFiltered);
	}
	
	private void initialize(String _idFieldName, String _idValue, Map<String,Object> _fields) {
		this.idFieldName = _idFieldName;		

		if (_fields == null) {
			_fields = new HashMap<String,Object>();
		}
		this.fields = _fields;

		this.fields.put(_idFieldName, _idValue);

	}

	public void setField(String fldName, Object fldValue) throws DocumentException {
		if (fldName.equals(idFieldName) && !(fldValue instanceof String)) {
			throw new DocumentException("Cannot set ID field '"+fldName+"' to a non-string value");
		}
		
		fields.put(fldName, fldValue);
	}

	public Object getField(String fldName) throws DocumentException {
		if (!fields.containsKey(fldName)) {
			throw new DocumentException("Document does not have a field with name: "+fldName);
		}
		return fields.get(fldName);
	}
	
	@Override
	public String getKeyFieldName() {
		return idFieldName;
	}

	@Override
	public String getKey() {
		return (String) this.fields.get(idFieldName);
	}

}
