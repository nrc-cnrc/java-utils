package ca.nrc.dtrc.elasticsearch.es6;

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
