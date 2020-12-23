package ca.nrc.dtrc.elasticsearch;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Index extends StreamlinedClient {

	// Stores the types of the various fields for a given document type
	private static Map<String,Map<String,String>> fieldTypesCache = null;	

	private Boolean _exists = null;

	public Index() throws ElasticSearchException {
		super();
	}

	public Index(String _indexName) throws ElasticSearchException {
		super(_indexName);
	}

	public boolean exists() throws ElasticSearchException {
		if (_exists == null) {
			URL url = null;
			try {
				url = esUrlBuilder().build();
			} catch (ElasticSearchException e) {
				throw new RuntimeException(e);
			}

			int status = head(url);
			_exists = (200 == status);

		}

		return _exists;
	}

	@JsonIgnore
	public IndexDef getDefinition() throws ElasticSearchException, IndexDefException {
		
		Map<String,Object> settingProps = null;
		// Get the Index settings
		{
		
			URL url = esUrlBuilder().forEndPoint("_settings").build();
			String json = get(url);
			
			Map<String,Object> esSettings = new HashMap<String,Object>();
			try {
				esSettings = (Map<String, Object>) new ObjectMapper().readValue(json, esSettings.getClass());
				esSettings = (Map<String, Object>) esSettings.get(this.indexName);
				esSettings = (Map<String, Object>) esSettings.get("settings");
			} catch (IOException e) {
				throw new ElasticSearchException(e);
			}		
			settingProps = IndexDef.tree2props(esSettings);
		}
		
		// Get the index mappings
		Map<String,Object> mappings = new HashMap<String,Object>();
		{
			String json = null;
			try {
				json = new ObjectMapper().writeValueAsString(mappings);
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}
			
			URL url = esUrlBuilder().forEndPoint("_mappings").build();
			json = get(url);
			try {
				mappings = (Map<String, Object>) new ObjectMapper().readValue(json, mappings.getClass());
			} catch (IOException e) {
				throw new ElasticSearchException(e);
			}
		}
		
		IndexDef iDef = 
				new IndexDef(indexName)
				.loadSettings(settingProps)
				.loadMappings(mappings)
				;
		
		return iDef;
	}



	public void setDefinition(IndexDef def) throws ElasticSearchException {
		setDefinition(def, null);
		
	}

	public void setDefinition(Map<String,Object> settings, Map<String,Object> mappings, Boolean force) throws ElasticSearchException, IndexDefException {
		if (force == null) {
			force = false;
		}
		
		IndexDef iDef = new IndexDef(indexName);
		iDef.loadSettings(settings);
		iDef.loadMappings(mappings);
		
		setDefinition(iDef, force);
	}
	
	public void setDefinition(IndexDef def, Boolean force) throws ElasticSearchException {
		if (force == null) {
			force = false;
		}
		
		if (indexExists()) {
			if (!force) {
				throw new IndexException("Tried to change settings of existing index "+getIndexName()+" eventhough force=false");
			} 
			deleteIndex();
		}
		
		String jsonString;
		String json;
		URL url;
		
		// Set the index mappings
		{		
			try {
				jsonString = new ObjectMapper().writeValueAsString(def.indexMappings());
			} catch (JsonProcessingException e) {
				throw new IndexException(e);
			}
			
			url = esUrlBuilder().build();
			json = put(url, jsonString);
		}
			
		
		// Set the index settings
		{
			try {
				jsonString = new ObjectMapper().writeValueAsString(def.settingsAsTree());
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}
			
			url = esUrlBuilder().forEndPoint("_settings").build();
			json = put(url, jsonString);
		}
		
		try {
			Thread.sleep(5*1000);
		} catch (InterruptedException e) {
			// Nevermind
		}
		
		return;
	}
	
	public void deleteIndex() throws ElasticSearchException {
		URL url =
			esUrlBuilder()
			.build();
		
		try {
			delete(url);	
			clearFieldTypesCache();
		} catch (NoSuchIndexException e) {
			// OK... we tried to delete an index that did not exist
			// All other exception types must be passed along 
		}
		
	}
	
	private static Map<String,String> uncacheFieldTypes(String docClassName) {
		Map<String,String> fieldTypes = null;
		if (fieldTypesCache != null && fieldTypesCache.containsKey(docClassName)) {
			fieldTypes = fieldTypesCache.get(docClassName);
		}
		return fieldTypes;
	}
	
	public static void cacheFieldTypes(Map<String,String> types, String docClassName) {
		if (fieldTypesCache == null) {
			fieldTypesCache = new HashMap<String,Map<String,String>>();
		}
		fieldTypesCache.put(docClassName, types);
	}

	private static void clearFieldTypesCache(Class<? extends Document> docClass) {
		clearFieldTypesCache(docClass.getName());
	}	
	
	static void clearFieldTypesCache(String docTypeName) {
		if (fieldTypesCache != null) {
			fieldTypesCache.put(docTypeName, null);
		}		
	}
		
	static void clearFieldTypesCache() {
		fieldTypesCache = null;
	}	
	
	private ObjectNode objectNode() {
		return new ObjectMapper().createObjectNode();
	}
	

	public Map<String,String> getFieldTypes(String type) throws ElasticSearchException {
		Map<String,String> fieldTypes = uncacheFieldTypes(type);
		if (fieldTypes == null) {
			fieldTypes = new HashMap<String,String>();
			URL url = esUrlBuilder().forDocType(type)
						.forEndPoint("_mapping")
						.endPointBeforeType(true).build();
			String jsonResponse = get(url);
			ObjectNode oNode = objectNode();
			ObjectMapper mapper = new ObjectMapper();
			try {
				oNode = mapper.readValue(jsonResponse, oNode.getClass());
			} catch (IOException exc) {
				throw new ElasticSearchException(exc);
			}
			
			ObjectNode fieldsProps = (ObjectNode) oNode.get(indexName).get("mappings").get(type).get("properties");
			
			Iterator<Entry<String, JsonNode>> iterator = fieldsProps.fields();
			while (iterator.hasNext()) {
				Entry<String, JsonNode> entry = iterator.next();
			    String aFldName = entry.getKey();
			    JsonNode aFldProps = entry.getValue();			    
			    if (aFldName.equals("additionalFields")) {
			    	fieldTypes = collectAdditionalFields(aFldProps, fieldTypes);
			    } else {
				    String aFldType = null;
				    if (aFldProps.has("type")) {
				    	aFldType = aFldProps.get("type").asText();
				    } else {
				    	aFldType = "_EMBEDDED_STRUCTURE";
				    }
				    fieldTypes.put(aFldName, aFldType);
			    }
			}
			
			cacheFieldTypes(fieldTypes, type);
		}
		
//		 Type for 'id' may not have been set in ES, if all
//		 the documents that were put into the type had a null id
		
		fieldTypes.put("id", "text");
		
		return fieldTypes;
	}
	
	private Map<String, String> collectAdditionalFields(JsonNode dynFieldsMapping, Map<String, String> fieldTypes) {
		ObjectNode props = (ObjectNode) dynFieldsMapping.get("properties");
		if (props != null) {
			Iterator<Entry<String, JsonNode>> iterator = props.fields();
			while (iterator.hasNext()) {
				Entry<String, JsonNode> entry = iterator.next();
			    String aFldName = entry.getKey();
			    JsonNode aFldProps = entry.getValue();			    
			    String aFldType = null;
			    if (aFldProps.has("type")) {
			    	aFldType = aFldProps.get("type").asText();
			    } else {
			    	aFldType = "_EMBEDDED_STRUCTURE";
			    }
			    fieldTypes.put("additionalFields."+aFldName, aFldType);			
			}
		}
		
		return fieldTypes;
	}
}
