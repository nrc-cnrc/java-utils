package ca.nrc.dtrc.elasticsearch;

import ca.nrc.debug.Debug;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;

/** This class is used to map the Elastic Search responses to objects */

public class ResponseMapper {
	/** For some unknown reason, ES records sometimes get corrupted to a state
	 * where they don't fit the structure of the object they are supposed to represent
	 * (typically, they end up with unexpected fields like 'scroll').
	 *
	 * The onBadRecord attribute specifies what to do in such cases.
	 *
	 *   RAISE_EXCEPTION: Raise a BadESRecordException.
	 *
	 *   LOG_EXCEPTION: Log the exceptino and return a null Document.
	 */
	public static enum BadRecordPolicy {RAISE_EXCEPTION, LOG_EXCEPTION}
	public BadRecordPolicy onBadRecord = BadRecordPolicy.RAISE_EXCEPTION;

	private static ObjectMapper mapper = new ObjectMapper();

	public ResponseMapper() {
		init__ResponseMapper((BadRecordPolicy)null);
	}

	public ResponseMapper(BadRecordPolicy _onBadRecord) {
		init__ResponseMapper(_onBadRecord);
	}

	private void init__ResponseMapper(BadRecordPolicy _onBadRecord) {
		if (_onBadRecord != null) {
			onBadRecord = _onBadRecord;
		}
	}

	public <T extends Document> T mapSingleDocResponse(
		String jsonResp, Class<T> docClass, String contextMess, String indexName)
		throws ElasticSearchException {
		T proto = (T)Document.prototype4class(docClass);
		return mapSingleDocResponse(jsonResp, proto, contextMess, indexName);
	}


	public <T extends Document> T mapSingleDocResponse(
		String jsonResp, T docProto, String contextMess, String indexName)
		throws ElasticSearchException {

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
				logger.error(contextMess+ Debug.printCallStack(exc));
			}

			if (this.onBadRecord != BadRecordPolicy.LOG_EXCEPTION ||
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
