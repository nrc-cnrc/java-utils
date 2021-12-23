package ca.nrc.dtrc.elasticsearch.index;

import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import org.json.JSONObject;

import java.util.Map;

public class NoSuchTypeException extends ElasticSearchException {

	public NoSuchTypeException() {
		super("No such type");
	}


	public NoSuchTypeException(Exception exc) {
		super(exc);
	}

	public NoSuchTypeException(Map<String, Object> esResponse) {
		super(esResponse);
	}

	public NoSuchTypeException(String errMessage, Exception exc) {
		super(errMessage, exc);
	}

	public NoSuchTypeException(String errMess, Exception exc, String indexName) {
		super(errMess, exc, indexName);
	}

	public NoSuchTypeException(Map<String, Object> esResponse, String indexName) {
		super(esResponse, indexName);
	}

	public NoSuchTypeException(String errorMessage, Exception e, Map<String, Object> esResponse, String indexName) {
		super(errorMessage, e, esResponse, indexName);
	}

	public NoSuchTypeException(String errorMessage, Exception e, String esResponse, String indexName) {
		super(errorMessage, e, esResponse, indexName);
	}

	public NoSuchTypeException(Exception e, String mess, JSONObject esResponse, String indexName) {
		super(e, mess, esResponse, indexName);
	}

	public NoSuchTypeException(Exception e, String mess, String esResponse, String indexName) {
		super(e, mess, esResponse, indexName);
	}

	public NoSuchTypeException(String errorMessage, Exception e, JSONObject esResponse, String indexName) {
		super(errorMessage, e, esResponse, indexName);
	}

	public NoSuchTypeException(String errorMessage) {
		super(errorMessage);
	}
}
