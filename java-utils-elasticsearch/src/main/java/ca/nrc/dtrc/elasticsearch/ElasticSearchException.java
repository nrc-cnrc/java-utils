package ca.nrc.dtrc.elasticsearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticSearchException extends Exception {
	
	private Map<String,Object> details = new HashMap<String,Object>();
	
	public ElasticSearchException(Exception exc) {
		super(exc);
	}	

	public ElasticSearchException(String message, Exception exc) {
		super(message, exc);
	}	

	public ElasticSearchException(Map<String,Object> _details) {
		super("Elastic Search operation return an error JSON response");
		details = _details;
	}

	public ElasticSearchException(String errorMessage) {
		super("Elastic Search operation return an error JSON response");
		
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
			details = new ObjectMapper().readValue(jsonDetails, details.getClass());
		} catch (IOException e) {
			e.printStackTrace();
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

}
