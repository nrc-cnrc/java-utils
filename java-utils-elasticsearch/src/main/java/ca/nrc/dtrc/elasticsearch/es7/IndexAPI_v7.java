package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.index.IndexDef;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.net.URL;

public class IndexAPI_v7 extends IndexAPI {
	public IndexAPI_v7(ESFactory _fullAPI) throws ElasticSearchException {
		super(_fullAPI);
	}

	@Override
	protected URL url4singletypeMappings(String docTypeName) throws ElasticSearchException {
		URL url = urlBuilder()
			.forEndPoint("_mapping").build();
		return url;
	}

	@Override
	protected URL url4indexDef() throws ElasticSearchException {
		URL url = urlBuilder().includeDocURLKeyword(false).build();
		return url;
	}

	@Override
	public URL url4deleteByQuery(String docType) throws ElasticSearchException {
		URL url = urlBuilder().forEndPoint("_delete_by_query")
			.includeDocURLKeyword(true)
			.includeTypeInUrl(false)
			.build();
		return url;
	}

	@Override
	public URL url4bulk(String docTypeName) throws ElasticSearchException {
		URL url = urlBuilder()
			.forEndPoint("_bulk")
			.includeDocURLKeyword(true)
			.build();
		return url;
	}


	@Override
	protected JSONObject extractFieldsProps(JSONObject jsonObj, String docTypeName) {
		JSONObject props = new JSONObject();
		JSONObject mappings = jsonObj.getJSONObject(indexName())
			.getJSONObject("mappings");
		if (mappings.has("properties")) {
			props = mappings.getJSONObject("properties");
		}
		return props;
	}

	@Override
	protected void putIndexDefintion(IndexDef iDef) throws ElasticSearchException {
		String jsonString;
		String json;
		URL url;

		// Set the index mappings
		{
			JSONObject mappings = formatMappings(iDef);

			url = url4indexDef();
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
	protected String bulkLinePrefix(String currDocTypeName, String id) {
		String prefix =
			"{\"index\": {\"_index\": \"" + indexName() + "\", \"_type\" : \"_doc\", \"_id\": \"" + id + "\"}}";
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
}
