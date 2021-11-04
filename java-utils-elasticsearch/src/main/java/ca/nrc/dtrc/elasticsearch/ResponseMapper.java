package ca.nrc.dtrc.elasticsearch;

import ca.nrc.debug.Debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;

import static ca.nrc.dtrc.elasticsearch.ErrorHandlingPolicy.STRICT;

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
	public ErrorHandlingPolicy onBadRecord = STRICT;

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
	}

	public <T extends Document> T response2doc(
		JSONObject jsonResp, Class<T> docClass, String contextMess)
		throws ElasticSearchException {
		T proto = (T)Document.prototype4class(docClass);
		return response2doc(jsonResp, proto, contextMess);
	}

	public <T extends Document> T response2doc(
		JSONObject jsonResp, T docProto, String contextMess)
		throws ElasticSearchException {

		T doc = null;
		if (jsonResp.has("_source")) {
			Class<? extends Document> docClass = docProto.getClass();
			String sourceJson = jsonResp.getJSONObject("_source").toString();
			try {
				if (sourceJson != null) {
					doc = (T) mapper.readValue(sourceJson, docClass);
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

	private void logBadRecordException(String contextMess, BadESRecordException exc) {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.ResponseMapper");
		logger.setLevel(Level.ERROR);
		logger.error(contextMess+ Debug.printCallStack(exc));
	}
}
