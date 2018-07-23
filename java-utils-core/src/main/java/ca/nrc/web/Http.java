package ca.nrc.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request.Builder;

public class Http {
	
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static Builder requestBuilder = new Request.Builder();
	public static enum ResponseType {STRING, MAP, JSON_NODE};
	
	// As recommended in the OkHttp documentation, we use a single
	// OkHttpClient instance for all our needs.
	private static OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60,  TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.build();
	
	public Object post(String url, String strBody, ResponseType respType) throws IOException {
		String strResponse = post(url, strBody);
		Object response = strResponse;
		ObjectMapper mapper = new ObjectMapper();
		if (respType == ResponseType.JSON_NODE) {
			response  = mapper.readTree(strResponse);
		} else if (respType == ResponseType.MAP) {
			response = new HashMap<String,Object>();
			response = mapper.readValue(strResponse, response.getClass());
		}
		
		
		return response;
	}
	
	public String post(String url, String strBody) throws IOException {
		RequestBody body = RequestBody.create(JSON, strBody);
    
	    Request request = requestBuilder
	        .url(url)
	        .post(body)
	        .build();
	    
	    Response response = httpClient.newCall(request).execute();
	    String jsonResponse = response.body().string();
	    
	    return jsonResponse;
	}
}
