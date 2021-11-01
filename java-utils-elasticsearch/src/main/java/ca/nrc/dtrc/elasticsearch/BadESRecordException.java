package ca.nrc.dtrc.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;

import java.io.IOException;

public class BadESRecordException extends ElasticSearchException {
	public BadESRecordException(IOException exc, String mess, JSONObject jsonSource, String indexName) {
		super(exc, mess, jsonSource, indexName);
	}
	public BadESRecordException(IOException exc, String mess, String jsonSource, String indexName) {
		super(exc, mess, jsonSource, indexName);
	}
}
