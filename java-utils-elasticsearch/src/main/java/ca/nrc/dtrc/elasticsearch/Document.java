package ca.nrc.dtrc.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.introspection.Introspection;
import ca.nrc.json.PrettyPrinter;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

public class Document {
	
	private static final int MAX_ID_LENGTH = 512;

	private static ObjectMapper mapper = new ObjectMapper();
	
	// This makes it possible for a document collection to 
	// contain documents that are in different languages
	public Boolean _detect_language = true;
	
	public String lang = "en";
		
	public String type = null;

	public Document setType(String _type) {
			this.type = _type;
			return this;
				}

	private String idWithoutType = null;

		public Document setIdWithoutType(String _id) {
			this.idWithoutType = _id;
			return this;
		}
		public String getIdWithoutType() {
			return this.idWithoutType;
		}

	private String shortDescription = null;
		public void setShortDescription(String _shortDescription) {
			this.shortDescription = _shortDescription; 
		}
		public String getShortDescription() {return this.shortDescription; }
	
		
	// Note: longDescription is synonymous with content 		
	private String content = null;
		public Document setContent(String _content) {
			this.content = _content;
			return this;
		}
		public String getContent() {return this.content;}
		
		// Note: longDescription is an alias for content.
		public void setLongDescription(String _longDescription) {this.content = _longDescription;}
		public String getLongDescription() {return this.content;}
			
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
		public Document setAdditionalField(String _fldName, Object _fldValue) {
			this.additionalFields.put(_fldName, _fldValue);
			return this;
		}
		@JsonIgnore
		public Object getAdditionalField(String _fldName) {
			Object value = this.additionalFields.get(_fldName);
			return value;
		}

	public Document() {
		initialize((String)null, (String)null);
	}

	public Document(String _id, String _type) {
		initialize(_id, _type);
	}

	private void initialize(String _id, String _type) {
		this.type = _type;
		this.idWithoutType = _id;
	}

	public static String docID(String _type, String _rawID) throws ElasticSearchException {
		if (_type == null) {
			throw new NullDocTypeException();
		}
		if (_rawID == null) {
			throw new ElasticSearchException("Null rawID");
		}
		String _id = _type+":"+_rawID;
		return _id;
	}

	public static String docID(String _type, Document doc) throws ElasticSearchException, ElasticSearchException {
		return docID(_type, doc.getIdWithoutType());
	}

	public static Document prototype(Class<? extends Document> docClass) throws ElasticSearchException {
		Document proto = null;
		Constructor<?> ctor = null;
		try {
			ctor = docClass.getConstructor();
			proto = (Document) ctor.newInstance();
			if (proto.type == null) {
				throw new ElasticSearchException("Prototype of document class had a null type: "+docClass);
			}
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ElasticSearchException("Could not create instance of class "+docClass, e);
		}
		return proto;
	}

	public static String determineType(Class<? extends Document> docClass)
		throws ElasticSearchException {
		return determineType(null, null, docClass);
	}

	public static String determineType(Document docPrototype)
		throws ElasticSearchException {
		return determineType(null, docPrototype, null);
	}

	public static String determineType(String explicitType, Document prototype)
		throws ElasticSearchException {
		return determineType(explicitType, prototype, null);
	}

	public static String determineType(
		String explicitType, Class<? extends Document> docClass)
	throws ElasticSearchException {
		return determineType(explicitType, null, docClass);
	}

	public static String determineType(String explicitType, Document prototype,
		Class<? extends Document> docClass) throws ElasticSearchException {

		// First, try using the explicit type provided
		String type = explicitType;

		// Next, try to get the type from the prototype
		if (type == null && prototype != null) {
			type = prototype.type;
		}

		// Next, try to get the type from the class
		if (type == null && docClass != null) {
			type = Document.prototype(docClass).type;
		}

		if (type == null) {
			throw new NullDocTypeException("Could not determine the doc type");
		}

		return type;
	}


	public String getId() {
		String _id = type+":"+ getIdWithoutType();
		return _id;
	}

	public Document setId(String _id) {
		Pair<String,String> idParts = parseID(_id);
		if (idParts.getLeft() != null) {
			type = idParts.getLeft();
		}
		idWithoutType = idParts.getRight();
		return this;
	}

	public static String removeType(String _id) {
		String withoutType = parseID(_id).getRight();
		return withoutType;
	}

	public static Pair<String,String> parseID(String _id) {
		String _type = null;
		String _idNoType = null;
		if (_id != null) {
			String[] parts = _id.split(":");
			if (parts.length > 1) {
				_type = parts[0];
				_idNoType = parts[1];
			} else {
				_idNoType = _id;
			}
		}
		return Pair.of(_type, _idNoType);
	}

	public static String changeIDType(String _id, String newType) {
		Pair<String,String> parsed = parseID(_id);
		String newID = newType+":"+parsed.getRight();
		return newID;
	}

	@JsonIgnore
	public String toJson() throws ElasticSearchException {
		return toJson(new String[0]);
	}

	@JsonIgnore
	public String toJson(String... ignoreFields) throws ElasticSearchException {
		if (type == null) {
			throw new NullDocTypeException();
		}
		Set<String> ignoreFieldsSet = new HashSet<String>();
		Collections.addAll(ignoreFieldsSet, ignoreFields);
		ignoreFieldsSet.add("longDescription");
		String content = PrettyPrinter.print(this, ignoreFieldsSet);
		return content;
	}

	
	public Object getField(String fldName, boolean failIfNotFound, Object defaultVal)
		throws DocumentException {
		Object value = defaultVal;
		if (fldName.startsWith("additionalFields.")) {
			// This is a dynamically set, runtie field
			fldName = fldName.substring(17);
			value = additionalFields.get(fldName);
		} else {
			// This is a member attribute field
			try {
				value = Introspection.getFieldValue(this, fldName, failIfNotFound);
			} catch (Exception exc) {
				throw new DocumentException(exc);
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
		String toS = null;
		try {
			toS = toString(null);
		} catch (ElasticSearchException e) {
			throw new RuntimeException(e);
		}
		return toS;
	}
	
	public String toString(Set<String> fieldsFilter) throws ElasticSearchException {
		String toStr = "";
		ObjectMapper mapper = new ObjectMapper();
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(mapper.writeValueAsString(this));
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		for (String field: jsonObject.keySet()) {
			if (fieldsFilter != null && fieldsFilter.contains(field)) continue;
			toStr += "\n-------\n"+field+": "+jsonObject.get(field);
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

	public static Document makeDocumentPrototype(String className) throws DocumentException {
		Class clazz;
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new DocumentException("Could not generate prototype for document class: "+className, e);
		}
		return makeDocumentPrototype(clazz);
	}
	
	public static Document makeDocumentPrototype(Class clazz) throws DocumentException {
		Document doc;
		try {
			Constructor<?> ctor = clazz.getConstructor();
			doc = (Document) ctor.newInstance();			
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new DocumentException("Could not generate prototype for document class: "+clazz.getName(), e);
		}
		
		return doc;
	}
	public String truncateID(String origID) {
		String truncated = origID;
		try {
			int rawLenght = truncated.getBytes("UTF-8").length;
			if (truncated.length() > MAX_ID_LENGTH) {
				// ID is too long for ElasticSearch
				// Truncate it and append a unique MD5 hashcode
				//
			    MessageDigest md;
				md = MessageDigest.getInstance("MD5");
			    md.update(truncated.getBytes());
			    byte[] digest = md.digest();
			    String hashCode = DatatypeConverter
			      .printHexBinary(digest).toUpperCase();
				
			    truncated = truncated.substring(0, MAX_ID_LENGTH - (hashCode.length()+1));
			    truncated += " "+hashCode;
			}	
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return truncated;
	}
}
