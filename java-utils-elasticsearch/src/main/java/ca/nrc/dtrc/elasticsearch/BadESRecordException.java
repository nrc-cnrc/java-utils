package ca.nrc.dtrc.elasticsearch;

import org.json.JSONObject;

public class BadESRecordException extends ElasticSearchException {
	public BadESRecordException(Exception exc, String mess, JSONObject jsonSource, String indexName) {
		super(exc, mess, jsonSource, indexName);
	}
	public BadESRecordException(Exception exc, String mess, String jsonSource, String indexName) {
		super(exc, mess, jsonSource, indexName);
	}
}
