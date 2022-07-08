package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.index.IndexDef;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.net.URL;

public class IndexAPI_v5 extends IndexAPI {

	public IndexAPI_v5(ESFactory _fullAPI) throws ElasticSearchException {
		super(_fullAPI);
	}

	@Override
	protected URL url4singletypeMappings(String docTypeName) throws ElasticSearchException {
		URL url = urlBuilder().forDocType(docTypeName)
		.forEndPoint("_mapping")
		.endPointBeforeType(true).build();
		return url;
	}

	@Override
	protected URL url4indexDef() throws ElasticSearchException {
		return urlBuilder().build();
	}

	@Override
	public URL url4deleteByQuery(String docType) throws ElasticSearchException {
		URL url = urlBuilder().forEndPoint("_delete_by_query").build();
		return url;
	}

	@Override
	public URL url4bulk(String docTypeName) throws ElasticSearchException {
		URL url = urlBuilder()
			.forDocType(docTypeName)
			.forEndPoint("_bulk")
			.build();
		return url;
	}


	@Override
	protected JSONObject extractFieldsProps(JSONObject jsonObj, String docTypeName) {
		JSONObject fieldProps =jsonObj
			.getJSONObject(indexName())
			.getJSONObject("mappings")
			.getJSONObject(docTypeName)
			.getJSONObject("properties");
		return fieldProps;
	}

	@Override
	protected void putIndexDefintion(IndexDef iDef) throws ElasticSearchException {
		Logger logger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es5.IndexAPI_v5.putIndexDefintion");
		String jsonString;
		String json;
		URL url;

		// Set the index mappings
		{
			JSONObject mappings = new JSONObject()
				.put("mappings", iDef.jsonMappings()
			);
			jsonString = mappings.toString();

			url = url4indexDef();
			if (logger.isTraceEnabled()) {
				logger.trace("url="+url+", jsonString="+ jsonString);
			}
			transport().put(url, jsonString);
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
			Thread.sleep(5*1000);
		} catch (InterruptedException e) {
			// Nevermind
		}
	}

	@Override
	protected String bulkLinePrefix(String currDocTypeName, String id) {
		return
			"{\"index\": {\"_index\": \"" + indexName() + "\", \"_type\" : \"" + currDocTypeName + "\", \"_id\": \"" + id + "\"}}";
	}
}
