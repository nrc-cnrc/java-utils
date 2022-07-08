package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.NoSuchIndexException;
import ca.nrc.dtrc.elasticsearch.index.IndexDef;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import ca.nrc.json.PrettyPrinter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.*;

public class IndexAPI_v7mi extends IndexAPI {
	protected static Map<String,Set<String>> typeIndices4baseIndex =
		new HashMap<String,Set<String>>();

	public IndexAPI_v7mi(ESFactory _fullAPI) throws ElasticSearchException {
		super(_fullAPI);
	}

	ES7miFactory esFactory() {
		return (ES7miFactory)esFactory;
	}

	/** Base name that will be used to compose the index name for all doc types
	 * in this 'index'.*/
	protected String indexBaseName() {
		return esFactory().indexName;
	}

	@Override
	protected URL url4singletypeMappings(String docTypeName) throws ElasticSearchException {
		URL url = esFactory().urlBuilder(docTypeName)
			.forEndPoint("_mapping").build();
		return url;
	}

	@Override
	protected URL url4indexDef() throws ElasticSearchException {
		String type = "idefs";
		URL url = url4indexDef(type);
		return url;
	}

	protected URL url4indexDef(String type) throws ElasticSearchException {
		URL url = esFactory().urlBuilder(type).includeDocURLKeyword(false).build();
		return url;
	}

	@Override
	public URL url4deleteByQuery(String docType) throws ElasticSearchException {
		URL url = esFactory().urlBuilder(docType).forEndPoint("_delete_by_query")
			.includeDocURLKeyword(true)
			.includeTypeInUrl(false)
			.build();
		return url;
	}

	@Override
	public URL url4bulk(String docTypeName) throws ElasticSearchException {
		URL url = esFactory().urlBuilder(docTypeName)
		.forEndPoint("_bulk")
		.includeDocURLKeyword(true)
		.build();
		return url;
	}

	@Override
	protected JSONObject extractFieldsProps(JSONObject jsonObj, String docTypeName) throws ElasticSearchException {
		JSONObject props = new JSONObject();
		String typeIndex = esFactory().index4type(docTypeName);
		JSONObject mappings = jsonObj.getJSONObject(typeIndex)
			.getJSONObject("mappings");
		if (mappings.has("properties")) {
			props = mappings.getJSONObject("properties");
		}
		return props;
	}

	public void ensureTypeIndexIsDefined(String type) throws ElasticSearchException {
		Set<String> typesSet = types();
		if (!typesSet.contains(type)) {
			IndexDef iDef = definition();
			putIndexDefintion(iDef, type);
		}
	}

	@Override
	protected void putIndexDefintion(IndexDef iDef) throws ElasticSearchException {
		putIndexDefintion(iDef, "idefs");
	}

	protected void putIndexDefintion(IndexDef iDef, String type) throws ElasticSearchException {
		String jsonString;
		String json;
		URL url;

		// Set the index mappings
		{
			JSONObject mappings = formatMappings(iDef);

			url = url4indexDef(type);
			json = transport().put(url, mappings.toString());
		}

		// Set the index settings
		{
			try {
				jsonString = new ObjectMapper().writeValueAsString(iDef.settingsAsTree());
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}

			url = urlBuilder().forEndPoint("_settings").build();
			json = transport().put(url, jsonString);
		}

		try {
			Thread.sleep(2*1000);
		} catch (InterruptedException e) {
			// Nevermind
		}
	}

	@Override
	protected String bulkLinePrefix(String currDocTypeName, String id) throws ElasticSearchException {
		String typeIndex = esFactory().index4type(currDocTypeName);
		String prefix =
			"{\"index\": {\"_index\": \"" + typeIndex + "\", \"_type\" : \"_doc\", \"_id\": \"" + id + "\"}}";
		return prefix;
	}

	private JSONObject formatMappings(IndexDef iDef) {
		JSONObject allProps = new JSONObject();
		JSONObject mappings = new JSONObject()
			.put("mappings", new JSONObject()
			.put("properties", allProps)
		);
		JSONObject mappingsByType = iDef.jsonMappings();
		for (String aType: mappingsByType.keySet()) {
			JSONObject aTypeProps = mappingsByType
			.getJSONObject(aType).getJSONObject("properties");
			for (String aProp: aTypeProps.keySet()) {
				allProps.put(aProp, aTypeProps.getJSONObject(aProp));
			}
		}
		return mappings;
	}

	@Override
	public boolean exists() throws ElasticSearchException {
		Logger logger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es7mi.IndexAPI_v7mi.exists");
		logger.trace("invoked for index="+esFactory().indexName);
		PrettyPrinter pprinter = new PrettyPrinter();
		Boolean answer = IndexAPI.uncacheIndexExists(esFactory().indexName);
		logger.trace("Uncached answer="+answer);
		if (answer == null) {
			Set<String> typeSet = types();
			if (logger.isTraceEnabled()) {
				logger.trace("typeSet="+ pprinter.pprint(typeSet));
			}
			answer = !typeSet.isEmpty();
			IndexAPI.cacheIndexExists(esFactory().indexName, answer);
		}
		logger.trace("Returning answer="+answer);
		return answer;
	}

	public boolean isEmpty() throws ElasticSearchException {
		boolean answer = true;
		Set<JSONObject> indicesInfo = fetchTypeIndicesFromServer();
		for (JSONObject indexInfo: indicesInfo) {
			float docsCount = indexInfo.getBigInteger("docs.count").floatValue();
			if (docsCount > 0) {
				answer = false;
				break;
			}
		}
		return answer;
	}


	@Override
	public void delete() throws ElasticSearchException {
		Logger logger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es7mi.IndexAPI_v7mi.exists");
		Set<String> types = types();
		types.add("idefs");
		for (String aType: types) {
			URL url = esFactory().urlBuilder(aType)
				.build();
			try {
				transport().delete(url);
			} catch (NoSuchIndexException e) {
				// OK... we tried to delete an index that did not exist
				// All other exception types must be passed along
			}
			clearFieldTypesCache();
			cacheTypeNames(null);
			cacheIndexExists(indexName(), false);
		}
	}

	public void clear(String docTypeName, Boolean failIfIndexNotFound)
		throws ElasticSearchException {
		Set<String> typesToClear = null;
		if (docTypeName == null) {
			typesToClear = types();
		} else {
			typesToClear = new HashSet<String>();
			typesToClear.add(docTypeName);
		}
		for (String type: typesToClear) {
			clearType(type, failIfIndexNotFound);
		}
	}

	private void clearType(String docTypeName, Boolean failIfIndexNotFound) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es7mi.IndexAPI_v7mi.clearType");
		URL url = url4deleteByQuery(docTypeName);
		JSONObject json = new JSONObject()
			.put("query", new JSONObject()
				.put("match_all", new JSONObject())
			);
		if (docTypeName != null) {
			json = new JSONObject()
				.put("query", new JSONObject()
					.put("bool", new JSONObject()
						.put("must", new JSONObject()
							.put("match", new JSONObject()
								.put("type", docTypeName)
							)
						)
					)
				);
		}
		String jsonInput = json.toString();
		String jsonResp = "{}";

		tLogger.trace("url=" + url + ", jsonInput=" + jsonInput);
		try {
			jsonResp = transport().post(url, jsonInput);
		} catch (Exception exc) {
			if (failIfIndexNotFound != null && failIfIndexNotFound) throw exc;
		}

		sleep();

	}


	public synchronized Set<String> types() throws ElasticSearchException {
		Set<String> typeSet = uncacheTypeNames();
		if (typeSet == null) {
			typeSet = new HashSet<String>();
			Set<JSONObject> indicesInfo = fetchTypeIndicesFromServer();
			for (JSONObject indexInfo : indicesInfo) {
				String name = indexInfo.getString("index");
				if (!name.startsWith(indexName()+"__")) {
					continue;
				}
				String type = name.substring(indexName().length() + 2);
				typeSet.add(type);
			}
			cacheTypeNames(typeSet);
		}
		return typeSet;
	}

	private synchronized Set<JSONObject> fetchTypeIndicesFromServer() throws ElasticSearchException {
		Set<JSONObject> indicesInfo = new HashSet<JSONObject>();
		try {
			URL url = urlBuilder().forCat("indices").build();
			String jsonResp = transport().get(url);
			JSONArray jsonArray = new JSONArray(jsonResp);
			for (Object index: jsonArray) {
				JSONObject indexInfo =(JSONObject)index;
				String name = indexInfo.getString("index");
				if (name.startsWith(this.esFactory().indexName+"__")) {
					indicesInfo.add(indexInfo);
				}
			}
			return indicesInfo;
		} catch (ElasticSearchException e) {
			throw new ElasticSearchException(e);
		}
	}

	public synchronized void cacheTypeNames(Set<String> typeIndices) {
		typeIndices4baseIndex.put(indexBaseName(), typeIndices);
		return;
	}

	public synchronized Set<String> uncacheTypeNames() {
		Set<String> types =  null;
		String baseName = indexBaseName();
		if (typeIndices4baseIndex.containsKey(baseName)) {
			types = typeIndices4baseIndex.get(baseName);
		}
		return types;
	}

	public void registerType(String type) throws ElasticSearchException {
		Set<String> currentTypes = types();
		currentTypes.add(type);
		cacheTypeNames(currentTypes);
	}
}
