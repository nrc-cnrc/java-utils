package ca.nrc.dtrc.elasticsearch;

import ca.nrc.debug.Debug;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ca.nrc.dtrc.elasticsearch.ErrorHandlingPolicy.STRICT;

/** This class is used to map the Elastic Search responses to objects */

public class ResponseMapper {
	/* When the mapper encounters an ESFactory record that does not have the expected
	 * Document structure, it will behave differently depending on the value of
	 * onBadRecord.
	 *
	 *   STRICT: Raise a BadESRecordException.
	 *
	 *   LENIENT: Log the exception and return a null or empty result
	 *   (depending on the context).
	 */
	public ErrorHandlingPolicy onBadRecord = STRICT;

	Logger errorLogger = null;

	private static ObjectMapper mapper = new ObjectMapper();

	private String indexName = "UNKNOWN";

	public ResponseMapper(String _indexName) {
		init__ResponseMapper(_indexName, (ErrorHandlingPolicy) null);
	}

	public ResponseMapper(String _indexName, ErrorHandlingPolicy _onBadRecord) {
		init__ResponseMapper(_indexName, _onBadRecord);
	}

	private void init__ResponseMapper(String _indexname,
		ErrorHandlingPolicy _onBadRecord) {
		if (_onBadRecord != null) {
			onBadRecord = _onBadRecord;
		}
		if (_indexname != null) {
			indexName = _indexname;
		}
		errorLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ResponseMapper");
		errorLogger.setLevel(Level.ERROR);
	}

	public <T extends Document> T response2doc(
		JSONObject jsonResp, Class<T> docClass, String contextMess)
		throws ElasticSearchException {
		T proto = (T)Document.prototype(docClass);
		return response2doc(jsonResp, proto, contextMess);
	}

	public <T extends Document> T response2doc(
		JSONObject jsonResp, T docProto, String contextMess)
		throws ElasticSearchException {

		T doc = null;
		JSONObject jsonDoc = null;
		// Leave the doc at null if the response says found=false
		if (!jsonResp.has("found") || jsonResp.getBoolean("found")) {
			if (jsonResp.has("_source")) {
				jsonDoc = jsonResp.getJSONObject("_source");
			} else {
				jsonDoc = jsonResp;
			}
			Class<? extends Document> docClass = docProto.getClass();
			String sourceJson = jsonDoc.toString();
			try {
				String _id = null;
				if (jsonResp.has("_id")) {
					_id = jsonResp.getString("_id");
				}
				doc = parseJsonDocSource(jsonDoc, docClass);
				if (_id != null) {
					doc.setId(_id);
				}
				if (doc.getId() == null) {
					throw new IOException("Document had null ID:\n"+mapper.writeValueAsString(doc));
				}
			} catch (IOException exc) {
				contextMess +=
					"\nCould not read _source field to instance of " + docClass + "\n" +
					"_source = " + sourceJson;
				BadESRecordException badRecExc =
					new BadESRecordException(exc, contextMess, sourceJson, indexName);

				logBadRecordException(contextMess, badRecExc);

				if (this.onBadRecord == ErrorHandlingPolicy.STRICT) {
					throw badRecExc;
				}
			}
		}

		return doc;
	}

	private <T extends Document> T parseJsonDocSource(
		JSONObject sourceJson, Class<? extends Document> docClass) throws JsonProcessingException {
		T doc = null;
		if (sourceJson != null) {
			if (sourceJson.has("doc")) {
				sourceJson = sourceJson.getJSONObject("doc");
			}
			doc = (T) mapper.readValue(sourceJson.toString(), docClass);
		}
		return doc;
	}

	private void logBadRecordException(String contextMess, BadESRecordException exc) {
		errorLogger.error(contextMess+ Debug.printCallStack(exc));
	}

	public <T extends Document> Pair<Pair<Long, String>, List<Hit<T>>> parseJsonSearchResponse(String jsonSearchResponse, T docPrototype) throws ElasticSearchException {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.es5.StreamlinedClient.parseJsonSearchResponse");
		List<Hit<T>> scoredDocuments = new ArrayList<>();
		String scrollID = null;
		ObjectMapper mapper = new ObjectMapper();
		JSONObject jsonRespObj;
		Long totalHits;
		try {
			jsonRespObj = new JSONObject(jsonSearchResponse);
			scrollID = jsonRespObj.getString("_scroll_id");
			JSONObject hitsCollectionNode = jsonRespObj.getJSONObject("hits");
			totalHits = getTotalHits(hitsCollectionNode);
			JSONArray hitsArrNode = hitsCollectionNode.getJSONArray("hits");
			for (int ii = 0; ii < hitsArrNode.length(); ii++) {
				JSONObject hitJson = hitsArrNode.getJSONObject(ii);
				T hitObject = response2doc(hitJson, docPrototype, "");
				Double hitScore = hitJson.getDouble("_score");

				JSONObject highlight = new JSONObject();
				if (hitJson.has("highlight")) {
					highlight = hitJson.getJSONObject("highlight");
				}
				scoredDocuments.add(new Hit<T>(hitObject, hitScore, highlight));
			}
		} catch (Exception e) {
			String mess =
				"Error parsing ESFactory search response:\n" + jsonSearchResponse;
			logger.error(mess+ Debug.printCallStack(e));
			throw new ElasticSearchException(
				"Error parsing ESFactory search response:\n" + jsonSearchResponse,
				e, this.indexName);
		}

		return Pair.of(Pair.of(totalHits, scrollID), scoredDocuments);
	}

	@JsonIgnore
	private Long getTotalHits(JSONObject hits) {
		Long totalHits = new Long(0);
		if (hits.get("total") instanceof JSONObject) {
			totalHits = hits.getJSONObject("total").getLong("value");
		} else {
			totalHits = hits.getLong("total");
		}
		return totalHits;
	}
}
