package ca.nrc.dtrc.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class CorruptedESRecordException extends ElasticSearchException {
	public CorruptedESRecordException(JsonProcessingException exc, String mess,
 		JsonNode sourceNode, String indexName) {
		super(exc, mess, sourceNode, indexName);
	}
}
