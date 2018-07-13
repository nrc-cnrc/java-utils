package ca.nrc.dtrc.elasticsearch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import com.fasterxml.jackson.databind.node.ObjectNode;

/*
 * This class of Document does not require the fields to be defined at 
 * compile time.
 * 
 * Instead, all fields are defined at run time in a hashmap.
 * 
 * The class does however include compile time attributes that correspond
 * to those fields that are expected to always be present in the document:
 * 
 * - ID (aka key)
 * - Document creation date
 * - possibly others
 * 
 * The method for manipulating those garantee that their values will always
 * be in synch with the values provided in the run-time defined values.
 */

public class Document_DynTyped extends Document {
	
	// The run-time defined fields of the document
	private Map<String,Object> fields = null;
		public void setFields(Map<String,Object> _fields) {
			this.fields = _fields;
		}
		public Map<String,Object> getFields() {
			return this.fields;
		}
	
	static Set<String> fieldsFilter = null;
	
	// Methods for manipulating the key field
	
	// Notice how we repeat the key's value as a compile-tme attribute
	// eventhough it is already 
	public String key = null; 
		public String getId() { return this.key;};
		public void setKey(String _key) throws DocumentException {
			this.key = _key;
//			this.setField(this.getKeyFieldName(), key);
		}
		// Name of the runtime field that corresponds to this.key.
		@Override
		public String keyFieldName() {
			return idFieldName;
		}		
		
		
		
	private String idFieldName = null;
		public void setIdFieldName(String _idFieldName) {this.idFieldName = _idFieldName;}
		public String getIdFieldName() {return this.idFieldName; }


	public Document_DynTyped() {initialize(null, null, null);};
	
	public Document_DynTyped(String _idFieldName) throws ElasticSearchException {
		if (_idFieldName == null) throw new ElasticSearchException("You must provide a non-null name for the ID field");		
		initialize(_idFieldName, null, null);
	}
		
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
		this.id = _idValue;
		if (fieldsFilter == null) {
			fieldsFilter = new HashSet<String>();
			fieldsFilter.add("fields");
		}
		
		this.idFieldName = _idFieldName;		

		if (_fields == null) {
			_fields = new HashMap<String,Object>();
		}
		this.fields = _fields;

		this.fields.put(_idFieldName, _idValue);
		
		this.key = (String)_fields.get(idFieldName);
	}


	
	public void setField(String fldName, Object fldValue) throws DocumentException {
		if (fldName.equals(idFieldName) && !(fldValue instanceof String)) {
			throw new DocumentException("Cannot set ID field '"+fldName+"' to a non-string value");
		}
		
		fields.put(fldName, fldValue);
	}
	
	@Override
	public Object getField(String fldName, boolean failIfNotFound, Object defaultVal) throws DocumentException {		
		Object value = defaultVal;
		
		// First check if there is a dynamic field by that name
		if (fields.containsKey(fldName)) {
			value = fields.get(fldName);
		} else {
			// No dynamic field by that name. Is there a 
			// static one?
			try {
				value = super.getField(fldName, true, null);
			} catch (DocumentException exc){
				// No static field by that name either.
				// Should we raise an exception?
				if (failIfNotFound) {
					throw new DocumentException(exc);
				}
			}
		}
		
		return value;
	}
	

	
	@Override
	public String toString() {
		String toS = null;
		
		Map<String,Object> dynFields = getFields();
		if (dynFields != null) {
			if (dynFields.containsKey(null)) {
				// Map with null keys cause problem in super.toString();
				dynFields.remove(null);
			}			
			for (String dynFldName: dynFields.keySet()) {
				if (dynFldName.equals("fields")) continue;
				Object dynVal = dynFields.get(dynFldName);
				String dynValStr = null;
				if (dynVal != null) dynValStr = dynVal.toString();
				toS += "\n----------\n"+dynFldName+": "+dynValStr;
			}
			
		}		
		if (toS == null) toS = "";
		
		toS = super.toString(fieldsFilter) + toS;
		
		return toS;
	}
}
