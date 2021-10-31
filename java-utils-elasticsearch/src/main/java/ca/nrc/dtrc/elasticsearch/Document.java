package ca.nrc.dtrc.elasticsearch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;

import javax.xml.bind.DatatypeConverter;

import ca.nrc.debug.Debug;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.introspection.Introspection;
import ca.nrc.json.PrettyPrinter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class Document {
	
	private static final int MAX_ID_LENGTH = 512;

	private static ObjectMapper mapper = new ObjectMapper();
	
	// This makes it possible for a document collection to 
	// contain documents that are in different languages
	public boolean _detect_language = true;
	
	public String lang = "en";
		
	public String keyFieldName() {return "id";};

	//
	// This complicated get/set Id stuff is here to ensure
	// that the ID never exceeds the maximum number of bytes
	// allowed by ElasticSearch.
	//
	@JsonIgnore
	public String getRawId() { return null; }
	public String id = null;
		public String getId() {
			if (this.id == null) {
				String rawId = getRawId();
				this.setId(rawId);
				if (rawId != null && !rawId.equals(id)) {
					System.out.println(
							"WARNING: raw ID of document was truncated.\n"+
							"  Original : '"+rawId+"'\n  Truncated : '"+id+"'"+
							"Type of document was: "+this.getClass()
						);
				}
			}
			return this.id;
		}
		public Document setId(String _id) {

		this.id = null;
			if (_id != null) {
				this.id = truncateID(_id);
			}
			return this;
		}
		
	private String shortDescription = null;
		public void setShortDescription(String _shortDescription) {
			this.shortDescription = _shortDescription; 
		}
		public String getShortDescription() {return this.shortDescription; }
	
		
	// Note: longDescription is synonymous with content 		
	private String content = null;
		public Document setContent(String _content) {this.content = _content; return this;}
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
		initialize(null);
	}
		
	public Document(String _id) {
		initialize(_id);
	}
	
	private void initialize(String _id) {
		this.setId(_id);
	}

	@JsonIgnore
	public String toJson() {
		return toJson(new String[0]);
	}

	@JsonIgnore
	public String toJson(String... ignoreFields) {
		Set<String> ignoreFieldsSet = new HashSet<String>();
		Collections.addAll(ignoreFieldsSet, ignoreFields);
		String content = PrettyPrinter.print(this, ignoreFieldsSet);
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

	public static <T extends Document> T mapESResponse(
		String jsonResp, Class<T> docClass, StreamlinedClient.BadRecordPolicy badRecordsPolicy,
		String contextMess, String indexName) throws ElasticSearchException {
		T proto = (T)prototype4class(docClass);
		return mapESResponse(jsonResp, proto, badRecordsPolicy, contextMess, indexName);
	}


	public static <T extends Document> T mapESResponse(
		String jsonResp, T docProto, StreamlinedClient.BadRecordPolicy badRecordsPolicy,
		String contextMess, String indexName) throws ElasticSearchException {

		T doc = null;
		Class<? extends Document> docClass = docProto.getClass();
		ObjectNode respNode = null;
		try {
			respNode = mapper.readValue(jsonResp, ObjectNode.class);
		} catch (IOException e) {
			throw new ElasticSearchException(
				contextMess + "\n" +
				"Could not map ES response to ObjectNode (index="+indexName+").\n" +
				"jsonResp=" + jsonResp);
		}
		JsonNode sourceNode = respNode.get("_source");;
		try {
			if (sourceNode != null) {
				doc = (T) mapper.treeToValue(sourceNode, docClass);
			}
		} catch (JsonProcessingException exc) {
			contextMess +=
				"\nCould not read _source field to instance of " + docClass + "\n"+
				"_source = " + sourceNode;
			ElasticSearchException excToRaise =
				new ElasticSearchException(exc, contextMess, sourceNode, indexName);
			if (isCorruptedRecord(sourceNode)) {
				excToRaise =
					new CorruptedESRecordException(exc, contextMess, sourceNode,
						indexName);
			}
			if (excToRaise instanceof CorruptedESRecordException) {
				// We log ALL CorruptedESRecordExceptions
				Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.Document.isCorruptedRecord");
				logger.setLevel(Level.ERROR);
				logger.error(contextMess+Debug.printCallStack(exc));
			}

			if (badRecordsPolicy != StreamlinedClient.BadRecordPolicy.LOG_EXCEPTION ||
				!(excToRaise instanceof CorruptedESRecordException)) {
				// We do not raise the exception if this is a corrupted record AND
				// we are using policy
				// BadRecordPolicy.LOG_EXCEPTION
				//   --> log the exception without raising it.
				throw excToRaise;
			}
		}
		return doc;
	}

	public static Document prototype4class(Class<? extends Document> docClass) throws ElasticSearchException {
		Document proto = null;
		try {
			proto =
				docClass.getConstructor().newInstance(new Object[0]);
		} catch (Exception e) {
			throw new ElasticSearchException(
				"Could not create prototype for document class "+docClass,
				e);
		}
		return proto;
	}

	@JsonIgnore
	private static boolean isCorruptedRecord(JsonNode jsonData) {
		Boolean corrupted = null;
		if (jsonData.has("_scroll") || jsonData.has("scroll")) {
			corrupted = true;
		}

		if (corrupted == null) {
			corrupted = false;
		}
		return corrupted;
	}
}
