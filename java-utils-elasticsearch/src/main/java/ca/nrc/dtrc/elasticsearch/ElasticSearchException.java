package ca.nrc.dtrc.elasticsearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticSearchException extends Exception {
	
	private Map<String,Object> details = new HashMap<String,Object>();
	private String indexName = null;

	public ElasticSearchException(Exception exc) {
		super(exc);
	}	

	public ElasticSearchException(String message, Exception exc) {
		super(message, exc);
		init_ElasticSearchException(message, null, exc, (String)null);
	}

	public ElasticSearchException(Map<String,Object> _details) {
		init_ElasticSearchException(
			(String)null, _details, (Exception)null, (String)null);
	}

	public ElasticSearchException(Map<String,Object> _details, String _indexName) {
		super("Elastic Search operation return an error JSON response");
		init_ElasticSearchException(
			(String)null, details, (Exception)null, (String)null);
	}

	public ElasticSearchException(String errorMessage) {
		super("Elastic Search operation return an error JSON response");
		init_ElasticSearchException(
			errorMessage, (Map)null, (Exception)null, (String)null);
	}

	private void init_ElasticSearchException(
		String errorMessage, Map<String, Object> _details, Exception exc, String _indexName) {
		this.details = _details;
		this.indexName = _indexName;

		if (details == null && errorMessage != null) {
			errorMessage = errorMessage.replaceAll("\"", "\\\"");
			String jsonDetails =
			"{\"error\":\n"
			+ "  {\"root_cause\":\n"
			+ "    {\n"
			+ "      \"type\": null,\n"
			+ "      \"reason\": \""+errorMessage+"\"\n"
			+ "    }\n"
			+ "  }\n"
			+ "}"
			;
			try {
				details =
					new ObjectMapper().readValue(jsonDetails, Map.class);
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}

	public String getRootReason() {
		String jsonRootReason = null;
		Map<String,Object> details_error = (Map<String, Object>) details.get("error");
		List<Map<String,Object>> details_error_rootcause = 
				(List<Map<String,Object>>) details_error.get("root_cause");
		Map<String,Object> details_error_rootcause_firstElt = details_error_rootcause.get(0);
		Object rootReasonObject = details_error_rootcause_firstElt.get("reason");
		
		try {
			jsonRootReason = new ObjectMapper().writeValueAsString(rootReasonObject);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		return jsonRootReason;
	}
	
	@Override
	public String getMessage() {
		String mess = super.getMessage();

		try {
			mess += "\nDetails; "+new ObjectMapper().writeValueAsString(this.details);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return mess;
	}

	public boolean isNoSuchIndex() {
		boolean answer = (getMessage().toLowerCase().contains("no such index"));
		return answer;
	}
}
