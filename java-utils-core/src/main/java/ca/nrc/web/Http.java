package ca.nrc.web;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request.Builder;

public class Http {

	public static enum Method {DELETE, GET, HEAD, POST, PUT};

	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static Builder requestBuilder = new Request.Builder();

	public static enum ResponseType {STRING, MAP, JSON_NODE};
	
	// As recommended in the OkHttp documentation, we use a single
	// OkHttpClient instance per thread.
	//
	private static Map<Thread,OkHttpClient> httpClients =
		new HashMap<Thread,OkHttpClient>();

	private static synchronized OkHttpClient httpClient() {
		Thread thr = Thread.currentThread();
		OkHttpClient client = httpClients.get(thr);
		if (client == null) {
			client = new OkHttpClient.Builder()
				.connectTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60,  TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.build();
			httpClients.put(thr, client);
		}
		return client;
	}

	public static HttpResponse doRequest(Method method, URL url, String jsonBody)
		throws HttpException {

		HttpResponse resp = null;
		if (method == Method.DELETE) {
			resp = delete(url);
		} else if (method == Method.GET) {
			resp = get(url);
		} else if (method == Method.HEAD) {
			resp = head(url);
		} else if (method == Method.POST) {
			resp = post(url, jsonBody);
		} else if (method == Method.PUT) {
			resp = put(url, jsonBody);
		}
		return resp;
	}

	public static Object post(String url, Map<String,Object> json, ResponseType respType) throws IOException {
		String jsonStr = new ObjectMapper().writeValueAsString(json);
		Object jsonResp = post(url, jsonStr, respType);
		return jsonResp;
	}

	public static Object post(String url, String strBody, ResponseType respType) throws IOException {
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

	public static String post(String url, Map<String,Object> json) throws IOException {
		String jsonStr = new ObjectMapper().writeValueAsString(json);
		String resp = post(url, jsonStr);
		return resp;
	}

	public static String post(String url, String strBody) throws IOException {
		RequestBody body = RequestBody.create(JSON, strBody);

	    Request request = requestBuilder
	        .url(url)
	        .post(body)
	        .build();

	    Response response = httpClient().newCall(request).execute();
	    String jsonResponse = response.body().string();

	    return jsonResponse;
	}

	public static HttpResponse get(URL url) throws HttpException {
		Request request = requestBuilder
			.url(url)
			.get()
			.build();

		Response response = null;
		try {
			response = httpClient().newCall(request).execute();
		} catch (IOException e) {
			throw new HttpException("Error invoking GET "+url, e);
		}
		HttpResponse httpResp = new HttpResponse(response);

		return httpResp;
	}

	public static HttpResponse post(URL url, String bodyStr)
		throws HttpException {
		return post(url, bodyStr, JSON);
	}

	public static HttpResponse post(URL url, String bodyStr, MediaType mediaType)
		throws HttpException {
		RequestBody body = RequestBody.create(mediaType, bodyStr);
		Request request = requestBuilder
			.url(url)
			.post(body)
			.build();

		Response response = null;
		try {
			response = httpClient().newCall(request).execute();
		} catch (IOException e) {
			throw new HttpException("Error invoking POST "+url+"\n"+bodyStr, e);
		}
		HttpResponse httpResp = new HttpResponse(response);

		return httpResp;
	}

	public static HttpResponse put(URL url, String jsonBody) throws HttpException {
		return put(url, jsonBody, JSON);
	}

	public static HttpResponse put(URL url, String bodyStr, MediaType mediaType)
		throws HttpException {
		RequestBody body = RequestBody.create(mediaType, bodyStr);
		Request request = requestBuilder
			.url(url)
			.put(body)
			.build();

		Response response = null;
		try {
			response = httpClient().newCall(request).execute();
		} catch (IOException e) {
			throw new HttpException("Error invoking PUT "+url+"\n"+bodyStr, e);
		}
		HttpResponse httpResp = new HttpResponse(response);

		return httpResp;
	}
	public static HttpResponse delete(URL url) throws HttpException {
		Request request = requestBuilder
			.url(url)
			.delete()
			.build();

		Response response = null;
		try {
			response = httpClient().newCall(request).execute();
		} catch (IOException e) {
			throw new HttpException("Error invoking DELETE "+url, e);
		}
		HttpResponse httpResp = new HttpResponse(response);

		return httpResp;
	}

	public static HttpResponse head(URL url) throws HttpException {
		Request request = requestBuilder
			.url(url)
			.head()
			.build();

		Response response = null;
		try {
			response = httpClient().newCall(request).execute();
		} catch (IOException e) {
			throw new HttpException("Error invoking HEAD "+url, e);
		}
		HttpResponse httpResp = new HttpResponse(response);

		return httpResp;
	}
}
