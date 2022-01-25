package ca.nrc.dtrc.elasticsearch.crud;

import ca.nrc.datastructure.Cloner;
import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/** API for creating/reading/updating/deleting ESFactory documents
 */
public abstract class CrudAPI extends ES_API {

	protected abstract URL url4putDocument(String type, String id) throws ElasticSearchException;
	protected abstract URL url4updateDocument(String type, String id) throws ElasticSearchException;
	protected abstract URL url4doc(String esDocType, String docID) throws ElasticSearchException ;

	public CrudAPI(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}

	public String putDocument(String esDocType, Document doc) throws ElasticSearchException {
		Document docWithType = doc;
		if (!doc.type.equals(esDocType)) {
			try {
				docWithType = Cloner.clone(doc);
			} catch (Cloner.ClonerException e) {
				throw new ElasticSearchException(e);
			}
			docWithType.type = esDocType;
		}
		return putDocument(esDocType, docWithType.getId(), docWithType.toJson());
	}

	public String putDocument(Document doc) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es5.CrudAPI_v5");
		defineIndexIfNotExists();
		if (tLogger.isTraceEnabled()) {
			try {
				tLogger.trace("(Document): putting document with id=" + doc.getId() + ", doc=" +
				new ObjectMapper().writeValueAsString(doc));
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}
		}
		String jsonDoc;
		try {
			jsonDoc = new ObjectMapper().writeValueAsString(doc);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		String docType = Document.determineType(null, doc, null);
		String docID = doc.getIdWithoutType();
		String jsonResponse = putDocument(docType, docID, jsonDoc);

		esFactory.indexAPI().cacheIndexExists(true);

		return jsonResponse;
	}

	public String putDocument(String type, String docID, String jsonDoc) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es5.StreamlinedClient.putDocument");
		URL url = url4putDocument(type, docID);
		tLogger.trace("(String, String, String) putting url=" + url + ", type=" + type + ", docID=" + docID + ", updatesWaitForRefresh=" + esFactory.updatesWaitForRefresh + ", jsonDoc=" + jsonDoc);

		String jsonResponse = esFactory.transport().put(url, jsonDoc);

		esFactory.indexAPI().clearFieldTypesCache(type);

		esFactory.sleep();

		return jsonResponse;
	}


	private void defineIndexIfNotExists() throws ElasticSearchException {
		IndexAPI indexAPI = esFactory.indexAPI();
		if (!indexAPI.exists()) {
			indexAPI.define(true);
		}
	}

	public <T extends Document> T getDocumentWithID(
		String docID, Class<T> docClass) throws ElasticSearchException  {
		return getDocumentWithID(docID, docClass, (String)null, (Boolean)null);
	}

	public <T extends Document> T getDocumentWithID(
		String docID, Class<T> docClass, Boolean failIfNonexistantIndex)
		throws ElasticSearchException {
		return getDocumentWithID(docID, docClass, (String)null, failIfNonexistantIndex);
	}

	public <T extends Document> T getDocumentWithID(
		String docID, Class<T> docClass, String esDocType)
		throws ElasticSearchException {
		return getDocumentWithID(docID, docClass, esDocType, (Boolean)null);
	}

	public <T extends Document> T getDocumentWithID(
		String docID, Class<T> docClass, String esDocType, Boolean failIfNoSuchIndex)
		throws ElasticSearchException  {

		if (failIfNoSuchIndex == null) {
			failIfNoSuchIndex = true;
		}

		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.CrudAPI.getDocumentWithID");

		esDocType = Document.determineType(esDocType, docClass);
		T doc = null;

		URL url = url4doc(esDocType, docID);
		tLogger.trace("url=" + url);

		ObjectMapper mapper = new ObjectMapper();


		try {
			String jsonRespStr = transport().get(url);
			JSONObject jsonResp = new JSONObject(jsonRespStr);
			doc =
			respMapper.response2doc(jsonResp, docClass,
			"Record for document with ID=" + docID + " is corrupted (expected class=" + docClass);
		} catch (NoSuchIndexException e) {
			if (failIfNoSuchIndex) {
				throw e;
			}
		}
		return doc;
	}

	public void deleteDocumentWithID(String id) throws ElasticSearchException  {
		Pair<String, String> parsed = Document.parseID(id);
		deleteDocumentWithID(parsed.getLeft(), parsed.getRight());
	}

	public void deleteDocumentWithID(String docID,
		Class<? extends Document> docClass) throws ElasticSearchException {

		String type = Document.determineType(docClass);
		deleteDocumentWithID(docID, type);
	}

	public void deleteDocumentWithID(String docID, String esDocType) throws ElasticSearchException  {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.crud.CrudAPI.deleteDocumentWithID");
		URL url = url4doc(esDocType, docID);
		transport().delete(url);
		sleep();
	}

	public void updateDocument(Document doc) throws ElasticSearchException  {
		Map<String,Object> docMap =
			new ObjectMapper().convertValue(doc, Map.class);
		updateDocument(doc.getClass(), doc.getId(), docMap);
	}

	public void updateDocument(
		Class<? extends Document> docClass, String docID,
		Map<String, Object> partialDoc) throws ElasticSearchException  {

		String esDocType = Document.determineType(docClass);
		updateDocument(esDocType, docID, partialDoc);
	}

	public void updateDocument(String esDocType, String docID, Map<String, Object> partialDoc) throws ElasticSearchException  {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.es5.StreamlinedClient.updateDocument");
		URL url = url4updateDocument(esDocType, docID);
		String jsonBody = null;
		Map<String, Object> jsonData = new HashMap<String, Object>();
		jsonData.put("doc", partialDoc);
		try {
			jsonBody = new ObjectMapper().writeValueAsString(jsonData);
		} catch (JsonProcessingException exc) {
			throw new ElasticSearchException(exc);
		}

		transport().post(url, jsonBody);
	}
}
