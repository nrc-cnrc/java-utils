package ca.nrc.dtrc.elasticsearch;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

public class ElasticSearchException extends Exception {
	
	private Map<String,Object> details = new HashMap<String,Object>();

	public ElasticSearchException(Exception exc) {
		super(exc);
	}

	public ElasticSearchException(Map<String,Object> esResponse) {
		super(message(esResponse));
	}

	public ElasticSearchException(String errMessage, Exception exc) {
		super(ElasticSearchException.message(errMessage), exc);
	}

	public ElasticSearchException(String errMess, Exception exc,
		String indexName) {
		super(ElasticSearchException.message(errMess, indexName), exc);
	}

	public ElasticSearchException(Map<String,Object> esResponse, String indexName) {
		super(message(esResponse, indexName));
	}

	public ElasticSearchException (String errorMessage,
 		Exception e, Map<String, Object> esResponse, String indexName) {
		super(message(errorMessage, esResponse, indexName), e);
	}

	public ElasticSearchException (String errorMessage,
 		Exception e, String esResponse, String indexName) {
		super(message(errorMessage, esResponse, indexName), e);
	}

	public ElasticSearchException (
		Exception e, String mess, JSONObject esResponse, String indexName) {
		super(message(mess, esResponse, indexName), e);
	}

	public ElasticSearchException (
		Exception e, String mess, String esResponse, String indexName) {
		super(message(mess, esResponse, indexName), e);
	}

	public ElasticSearchException (String errorMessage,
		Exception e, JSONObject esResponse, String indexName) {
		super(message(errorMessage, esResponse, indexName), e);
	}

	public ElasticSearchException(String errorMessage) {
		super(message(errorMessage));
	}

	private static String message(String errorMessage) {
		return message(errorMessage, (Map)null, (String)null);
	}

	private static String message(String errorMessage, String indexName) {
		return message(errorMessage, (String)null, indexName);
	}

	private static String message(Map<String, Object> esResponse) {
		return message((String)null, esResponse, (String)null);
	}

	private static String message(Map<String, Object> esResponse, String indexName) {
		return message((String)null, esResponse, indexName);
	}

	private static String message(String errorMessage, Map<String, Object> esResponse,
		String indexName) {
		String jsonResponse = null;
		if (esResponse != null) {
			try {
				jsonResponse = new ObjectMapper().writeValueAsString(esResponse);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}

		return message(errorMessage, jsonResponse, indexName);
	}

	private static String message(String errorMessage, JSONObject esResponse,
		String indexName) {
		String jsonResponse = null;
		if (esResponse != null) {
			jsonResponse = esResponse.toString();
		}

		return message(errorMessage, jsonResponse, indexName);
	}

	private static String message(JsonNode esResponse, String indexName) {
		String jsonResponse = null;
		if (esResponse != null) {
			try {
				jsonResponse = new ObjectMapper().writeValueAsString(esResponse);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}

		return message(jsonResponse, indexName);
	}


	private static String message(String errorMessage, String jsonResponse,
		String indexName) {

		String mess = "Exception Details:\n";
		if (errorMessage != null) {
			mess += errorMessage;
		}
		if (indexName != null) {
			mess += "\n Index: "+indexName;
		}
		if (jsonResponse != null) {
			mess += "\nESFactory response; " + jsonResponse;
		}

		mess += "END OF Exception Details\n";
		return mess;
	}


	public boolean isNoSuchIndex() {
		boolean answer = (getMessage().toLowerCase().contains("no such index"));
		return answer;
	}
}
