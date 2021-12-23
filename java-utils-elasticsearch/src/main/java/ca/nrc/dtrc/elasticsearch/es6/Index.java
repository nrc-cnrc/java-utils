package ca.nrc.dtrc.elasticsearch.es6;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;

public class Index extends StreamlinedClient {

	// Stores the types of the various fields for a given document type
	private static Map<String,Map<String,String>> fieldTypesCache = null;	

	private static  Map<String,Boolean> indexExistsCache =
		new HashMap<String,Boolean>();

	public Index() throws ElasticSearchException {
		super();
	}

	public Index(String _indexName) throws ElasticSearchException {
		super(_indexName);
	}

	public boolean exists() throws ElasticSearchException {
		Boolean exists = uncacheIndexExists(indexName);
		if (exists == null) {
			URL url = null;
			try {
				url = esUrlBuilder().noDocKeyword().build();
			} catch (ElasticSearchException e) {
				throw new RuntimeException(e);
			}

			int status = head(url);
			exists = (200 == status);
			cacheIndexExists(indexName, exists);
		}

		return exists;
	}

	private static synchronized Boolean uncacheIndexExists(String indexName) {
		Boolean exists = indexExistsCache.get(indexName);
		return exists;
	}

	public static synchronized void cacheIndexExists(String indexName, Boolean exists) {
		indexExistsCache.put(indexName, exists);
	}

	public boolean isEmpty() throws ElasticSearchException {
		Boolean empty = null;

		URL url = null;
		try {
			url = esUrlBuilder().forEndPoint("_mappings").build();
		} catch (ElasticSearchException e) {
			throw new RuntimeException(e);
		}

		String jsonResp = null;
		try {
			jsonResp = get(url);
		} catch (NoSuchIndexException e1) {
			empty = true;
		}

		if (empty == null) {
			// Index exists. Does it include some types?
			JSONObject mappings = new JSONObject(jsonResp)
				.getJSONObject(indexName)
				.getJSONObject("mappings");

			empty = (0 == mappings.keySet().size());
		}

		return empty;
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
				throw new IndexException("Tried to change settings of existing index "+ getIndexName()+" eventhough force=false");
			}
			try {
				clearIndex();
			} catch (IOException | InterruptedException e) {
				throw new ElasticSearchException(e);
			}
		} else {
			createIndex(indexName);
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

			url = esUrlBuilder()
				.forEndPoint("_mapping")
				.epComesFirs()
				.build();
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

		cacheIndexExists(indexName, true);

		return;
	}
	
	public void deleteIndex() throws ElasticSearchException {
		URL url =
			esUrlBuilder()
			.noDocKeyword()
			.build();
		
		try {
			delete(url);	
			clearFieldTypesCache();
		} catch (NoSuchIndexException e) {
			// OK... we tried to delete an index that did not exist
			// All other exception types must be passed along 
		}
		cacheIndexExists(indexName, (Boolean)null);
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
				.forEndPoint("_mapping").build();
			String jsonResponse = get(url);
			JSONObject rootNode = null;
			rootNode = new JSONObject(jsonResponse);

			JSONObject mappings =
				rootNode.getJSONObject(indexName).getJSONObject("mappings");

			JSONObject attr4type = null;
			if (mappings.has(type)) {
				attr4type = mappings.getJSONObject(type);
			} else {
				attr4type = mappings.getJSONObject("_doc");
			}

			JSONObject fieldsProps = attr4type.getJSONObject("properties");

			for (String aFldName: fieldsProps.keySet()) {
			    JSONObject aFldProps = fieldsProps.getJSONObject(aFldName);
			    if (aFldName.equals("additionalFields")) {
			    	fieldTypes = collectAdditionalFields(aFldProps, fieldTypes);
			    } else {
				    String aFldType = null;
				    if (aFldProps.has("type")) {
				    	aFldType = aFldProps.getString("type");
				    } else {
				    	aFldType = "_EMBEDDED_STRUCTURE";
				    }
				    fieldTypes.put(aFldName, aFldType);
			    }
			}

			cacheFieldTypes(fieldTypes, type);
		}

//		 Type for 'id' may not have been set in ESFactory, if all
//		 the documents that were put into the type had a null id

		fieldTypes.put("id", "text");

		return fieldTypes;
	}
	
	private Map<String, String> collectAdditionalFields(
		JSONObject dynFieldsMapping, Map<String, String> fieldTypes) {

		if (dynFieldsMapping.has("properties")) {
			JSONObject props = dynFieldsMapping.getJSONObject("properties");
			if (props != null) {
				for (String aFldName : props.keySet()) {
					JSONObject aFldProps = props.getJSONObject(aFldName);
					String aFldType = null;
					if (aFldProps.has("type")) {
						aFldType = aFldProps.getString("type");
					} else {
						aFldType = "_EMBEDDED_STRUCTURE";
					}
					fieldTypes.put("additionalFields." + aFldName, aFldType);
				}
			}
		}
		
		return fieldTypes;
	}
}
