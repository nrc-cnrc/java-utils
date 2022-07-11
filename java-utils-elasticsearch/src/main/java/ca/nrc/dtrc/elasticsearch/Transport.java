package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.index.NoSuchTypeException;
import ca.nrc.web.Http;
import ca.nrc.web.HttpException;
import ca.nrc.web.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.RequestBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Submit Http requests to the ESFactory server, and check response for errors.
 */
public class Transport {

	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private String forIndex = "UNKNOWN";

	private ObjectMapper mapper = new ObjectMapper();

	private List<ESObserver> observers = new ArrayList<ESObserver>();

	/**
	 * Note: As of 2020-01, we have noticed that when several StreamlinedClient_v5
	 * 	are used concurrently in different threads, it ends up creating
	 *    documents whose JSON structure does not correspond to the structure
	 *    of a document.
	 *
	 *    If you find that to be the case, then configure your StreamlinedClients
	 *    with syncHttpCalls = true. Note that this may significantly slow down
	 *    the operation of the various StreamlinedClients.
	 *
	 *    This will cause the client to invoke the Http client throug the
	 *    synchronized method httpCall_sync below.
	 */
	public boolean synchedHttpCalls = true;

	public Transport(String _indexName, List<ESObserver> _observers) {
		if (_indexName != null) {
			forIndex = _indexName;
		}
		if (_observers != null) {
			observers = _observers;
		}
	}

	public int head(URL url) throws ElasticSearchException {
		return head(url, (String) null);
	}

	public int head(URL url, String json) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.Transport.head");

		String requestDetails = "   url=" + url + "\n   json=" + json + "]";
		tLogger.trace("invoking request:\n" + requestDetails);

		for (ESObserver obs : observers) {
			obs.observeBeforeHEAD(url, json);
		}

		if (json == null) {
			json = "";
		}
		RequestBody body = RequestBody.create(JSON, json);

		HttpResponse response = httpCall(Http.Method.HEAD, url, json, tLogger);

		int status = response.code;

		for (ESObserver obs : observers) {
			obs.observeAfterHEAD(url, json);
		}

		return status;
	}


	public String post(URL url) throws ElasticSearchException {
		return post(url, null);
	}

	public String post(URL url, String json) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.Transport.post");
		tLogger.trace("posting url=" + url + ", with json=" + json);

		for (ESObserver obs : observers) {
			obs.observeBeforePOST(url, json);
		}

		if (json == null) json = "";
		HttpResponse response = httpCall(Http.Method.POST, url, json, tLogger);
		String jsonResponse = response.body;

		checkForESErrorResponse(jsonResponse);

		for (ESObserver obs : observers) {
			obs.observeAfterPOST(url, json);
		}

		return jsonResponse;
	}

	public String get(URL url) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.Transport.get");

		for (ESObserver obs : observers) {
			obs.observeBeforeGET(url);
		}

		HttpResponse response = httpCall(Http.Method.GET, url, (String)null, tLogger);

		String jsonResponse = response.body;

		checkForESErrorResponse(jsonResponse);

		for (ESObserver obs : observers) {
			obs.observeAfterGET(url);
		}


		tLogger.trace("returning: " + jsonResponse);

		return jsonResponse;
	}

	public String put(URL url, JSONObject json) throws ElasticSearchException {
		String jsonStr = json.toString();
		return put(url, jsonStr);
	}


	public String put(URL url, String json) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.Transport.put");
		tLogger.trace("putting url=" + url + ", with json=\n" + json);

		for (ESObserver obs : observers) {
			obs.observeBeforePUT(url, json);
		}

		if (json == null) json = "";
		HttpResponse response = httpCall(Http.Method.PUT, url, json, tLogger);

		String jsonResponse = response.body;

		checkForESErrorResponse(jsonResponse);

		for (ESObserver obs : observers) {
			obs.observeAfterPUT(url, json);
		}


		return jsonResponse;
	}

	public String delete(URL url) throws ElasticSearchException {
		return delete(url, "");
	}

	public String delete(URL url, String jsonBody) throws ElasticSearchException {
		@SuppressWarnings("unused")
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.Transport.delete");

		for (ESObserver obs : observers) {
			obs.observeBeforeDELETE(url, jsonBody);
		}

		if (jsonBody == null) jsonBody = "";
		HttpResponse response = httpCall(Http.Method.DELETE, url, jsonBody, tLogger);
		String jsonResponse = response.body;

		checkForESErrorResponse(jsonResponse);

		for (ESObserver obs : observers) {
			obs.observeAfterDELETE(url, jsonBody);
		}

		return jsonResponse;
	}

	public void checkForESErrorResponse(String jsonResponse) throws ElasticSearchException {
		ElasticSearchException exception = null;
		boolean checked = false;

		// First, try treating response as a JSONObject
		try {
			JSONObject jsonResp = new JSONObject(jsonResponse);
			checked = true;
			if (jsonResp.has("error")) {
				exception = makeElasticSearchException(jsonResp);
			}
		} catch (Exception e){
			// If this is not a JSONObject, just leave checked=false;
		}

		if (!checked) {
			// Next, try treating the response as a JSONArray
			JSONArray jsonResp = new JSONArray(jsonResponse);
			// Note: no error can be contained in a JSONArray
			checked = true;
		}

		if (!checked) {
			// Response is neither a JSONObject nor a JSONArray. So it must be a plain
			// string tha provides the error message.
			JSONObject excDetails = new JSONObject()
				.put("error", jsonResponse);
			exception = new ElasticSearchException(excDetails, this.forIndex);
			checked = true;
		}

		if (exception != null) throw exception;
	}

	private Map<String, Object> checkESErrorResponse_JSONObject(String jsonResponse) {
		return null;
	}


	// Note: As of 2020-01, we have noticed that when several StreamlinedClient_v5
	//    are used concurrently in different threads, it ends up creating
	//    documents whose JSON structure does not correspond to the structure
	//    of a document.
	//
	//    If you find that to be the case, then configure your StreamlinedClients
	//    with syncHttpCalls = true.
	//
	//    This will cause the client to invoke the Http client throug the
	//    synchronized method httpCall_sync below.
	private HttpResponse httpCall(
		Http.Method method, URL url, String bodyJson, Logger tLogger)
		throws ElasticSearchException {
		HttpResponse resp = null;
		if (synchedHttpCalls) {
			resp = httpCall_sync(method, url, bodyJson, tLogger);
		} else {
			resp = httpCall_async(method, url, bodyJson, tLogger);
		}
		return resp;
	}


	// Note: We make this method synchronized in an attempt to prevent corruption
	//   of the index when multiple concurrent requests are issued.
	//
	private synchronized HttpResponse httpCall_sync(
		Http.Method method, URL url, String bodyJson, Logger tLogger)
		throws ElasticSearchException {
		return httpCall_async(method, url, bodyJson, tLogger);
	}

	private HttpResponse httpCall_async(
		Http.Method method, URL url, String bodyJson, Logger tLogger)
		throws ElasticSearchException {

		String callDetails =
			"   " + method.name() + " " + url + "\n" +
			"   " + bodyJson + "\n";


		HttpResponse httpResponse = null;

		try {
			if (method == Http.Method.DELETE) {
				httpResponse = Http.delete(url);
			} else if (method == Http.Method.GET) {
				httpResponse = Http.get(url);
			} else if (method == Http.Method.HEAD) {
				httpResponse = Http.head(url);
			} else if (method == Http.Method.POST) {
				httpResponse = Http.post(url, bodyJson);
			} else if (method == Http.Method.PUT) {
				httpResponse = Http.put(url, bodyJson);
			}
		} catch (HttpException  e) {
			throw new ElasticSearchException(e);
		}

		if (tLogger != null && tLogger.isTraceEnabled()) {
			tLogger.trace(
				"returning from http call:\n" +
				callDetails + "\n" +
				"   response code : " + httpResponse.code + "\n" +
				"   response body : " + httpResponse.body
			);
		}

		return httpResponse;
	}

	private static ElasticSearchException makeElasticSearchException(
		JSONObject responseObj) {
		ElasticSearchException exc =
			new ElasticSearchException(responseObj);

		if (responseObj.has("error")) {
			JSONObject error = responseObj.getJSONObject("error");
			if (error.has("root_cause")) {
				JSONArray rootCauseLst = error.getJSONArray("root_cause");
				if (rootCauseLst.length() > 0) {
					JSONObject rootCause = rootCauseLst.getJSONObject(0);
					if (rootCause.has("type")) {
						String errorType = rootCause.getString("type");
						if (errorType.equals("index_not_found_exception")) {
							String indexName = "<unknown>";
							if (rootCause.has("index")) {
								indexName = rootCause.getString("index");
							}
							exc = new NoSuchIndexException("No such index: " + indexName);
						} else if (errorType.equals("type_missing_exception")) {
							exc = new NoSuchTypeException();
						}
					}
				}
			}
		}

		return exc;
	}

	private static ElasticSearchException makeElasticSearchException(Map<String, Object> responseObj) {
		ElasticSearchException exc =
			new ElasticSearchException(responseObj);

		if (responseObj.containsKey("error")) {
			Map<String, Object> error = (Map<String, Object>) responseObj.get("error");
			if (error.containsKey("root_cause")) {
				List<Object> rootCauseLst = (List<Object>) error.get("root_cause");
				if (rootCauseLst.size() > 0) {
					Map<String, Object> rootCause = (Map<String, Object>) rootCauseLst.get(0);
					if (rootCause.containsKey("type")) {
						String errorType = (String) rootCause.get("type");
						if (errorType.equals("index_not_found_exception")) {
							String indexName = "<unknown>";
							if (rootCause.containsKey("index")) {
								indexName = (String) rootCause.get("index");
							}
							exc = new NoSuchIndexException("No such index: " + indexName);
						} else if (errorType.equals("type_missing_exception")) {
							exc = new NoSuchTypeException();
						}
					}
				}
			}
		}


		return exc;
	}
}