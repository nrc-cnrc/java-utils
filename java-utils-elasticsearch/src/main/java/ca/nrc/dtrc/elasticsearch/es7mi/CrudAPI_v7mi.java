package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.Document;
import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.NoSuchIndexException;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.net.URL;
import java.util.Map;

public class CrudAPI_v7mi extends CrudAPI {

	public CrudAPI_v7mi(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}

	private ES7miFactory esFactory() {
		return (ES7miFactory) esFactory;
	}

	@Override
	protected URL url4putDocument(String type, String id) throws ElasticSearchException {
		URL url =
		esFactory().urlBuilder(type)
		.forDocType(type)
		.includeTypeInUrl(false)
		.forDocID(id)
		.refresh(esFactory.updatesWaitForRefresh)
		.build();
		return url;
	}

	@Override
	protected URL url4updateDocument(String esDocType, String docID) throws ElasticSearchException {
		URL url =
		esFactory().urlBuilder(esDocType)
		.forDocType(esDocType)
		.forDocID(docID)
		.forEndPoint("_update")
		.includeTypeInUrl(false)
		.refresh(esFactory.updatesWaitForRefresh)
		.build();
		return url;
	}

	@Override
	protected URL url4doc(String esDocType, String docID) throws ElasticSearchException {
		URL url =
		esFactory().urlBuilder(esDocType).forDocType(esDocType).forDocID(docID)
		.includeTypeInUrl(false).build();
		return url;
	}

	public <T extends Document> T getDocumentWithID(
		String docID, Class<T> docClass, String esDocType, Boolean failIfNoSuchIndex)
		throws ElasticSearchException  {

		if (failIfNoSuchIndex == null) {
			failIfNoSuchIndex = true;
		}

		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.CrudAPI.getDocumentWithID");

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
			// There was no index for that specific type. Check if the index itself
			// exists (i.e. check if there is an index for SOME type that exists).
			if (failIfNoSuchIndex && !esFactory().indexAPI().exists()) {
				throw e;
			}
		}
		return doc;
	}

	@Override
	public void updateDocument(String esDocType, String docID,
		Map<String, Object> partialDoc) throws ElasticSearchException  {
		esFactory().indexAPI().ensureTypeIndexIsDefined(esDocType);
		super.updateDocument(esDocType, docID, partialDoc);
	}

	@Override
	public String putDocument(Document doc) throws ElasticSearchException {
		esFactory().indexAPI().ensureTypeIndexIsDefined(doc.type);
		String jsonResp = super.putDocument(doc);
		esFactory().indexAPI().registerType(doc.type);
		return jsonResp;
	}
}
