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


	public Document_DynTyped() {};
		
	public Document_DynTyped(String _idFieldName, String _idValue) throws ElasticSearchException {
		if (_idFieldName == null) throw new ElasticSearchException("You must provide a non-null name for the ID field");
		if (_idValue == null) throw new ElasticSearchException("The value for the ID field cannot be null");
		initialize(_idFieldName, _idValue, null);
	}

	public Document_DynTyped(String _idFieldName, String _idValue, Map<String,Object> _fields) throws ElasticSearchException {
		if (_idFieldName == null) throw new ElasticSearchException("You must provide a non-null name for the ID field");
		if (_idValue == null) throw new ElasticSearchException("The value for the ID field cannot be null");
		initialize(_idFieldName, _idValue, _fields);
	}
	
	public Document_DynTyped(String _idFieldName, Map<String,Object> _fields) throws ElasticSearchException  {
		if (_idFieldName == null) throw new ElasticSearchException("You must provide a non-null name for the ID field");
		if (_fields == null || !_fields.containsKey(_idFieldName)) throw new ElasticSearchException("The map of fields must contain a value for the ID field '"+_idFieldName+"'");
		initialize(_idFieldName, (String)_fields.get(_idFieldName), _fields);
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
