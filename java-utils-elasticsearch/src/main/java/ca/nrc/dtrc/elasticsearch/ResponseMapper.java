package ca.nrc.dtrc.elasticsearch;

import ca.nrc.debug.Debug;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;

/** This class is used to map the Elastic Search responses to objects */

public class ResponseMapper {
	/* When the mapper encounters an ES record that does not have the expected
	 * Document structure, it will behave differently depending on the value of
	 * onBadRecord.
	 *
	 *   STRICT: Raise a BadESRecordException.
	 *
	 *   LENIENT: Log the exception and return a null or empty result
	 *   (depending on the context).
	 */
	public static enum BadRecordHandling {STRICT, LENIENT}
	public BadRecordHandling onBadRecord = BadRecordHandling.STRICT;

	private static ObjectMapper mapper = new ObjectMapper();

	private String indexName = "UNKNOWN";

	public ResponseMapper(String _indexName) {
		init__ResponseMapper(_indexName, (BadRecordHandling)null);
	}

	public ResponseMapper(String _indexName, BadRecordHandling _onBadRecord) {
		init__ResponseMapper(_indexName, _onBadRecord);
	}

	private void init__ResponseMapper(String _indexname,
		BadRecordHandling _onBadRecord) {
		if (_onBadRecord != null) {
			onBadRecord = _onBadRecord;
		}
		if (_indexname != null) {
			indexName = _indexname;
		}
	}

	public <T extends Document> T mapSingleDocResponse(
		String jsonResp, Class<T> docClass, String contextMess)
		throws ElasticSearchException {
		T proto = (T)Document.prototype4class(docClass);
		return mapSingleDocResponse(jsonResp, proto, contextMess);
	}


	public <T extends Document> T mapSingleDocResponse(
		String jsonResp, T docProto, String contextMess)
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
			BadESRecordException badRecExc =
				new BadESRecordException(exc, contextMess, sourceNode, indexName);
			logBadRecordException(contextMess, badRecExc);

			if (this.onBadRecord == BadRecordHandling.STRICT) {
				throw badRecExc;
			}
		}
		return doc;
	}

	private void logBadRecordException(String contextMess, BadESRecordException exc) {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.Document.isCorruptedRecord");
		logger.setLevel(Level.ERROR);
		logger.error(contextMess+ Debug.printCallStack(exc));
	}

//	@JsonIgnore
//	private static boolean isCorruptedRecord(JsonNode jsonData) {
//		Boolean corrupted = null;
//		if (jsonData.has("_scroll") || jsonData.has("scroll")) {
//			corrupted = true;
//		}
//
//		if (corrupted == null) {
//			corrupted = false;
//		}
//		return corrupted;
//	}
}
