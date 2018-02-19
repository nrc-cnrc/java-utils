package ca.nrc.dtrc.elasticsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.nrc.json.PrettyPrinter;
import ca.nrc.datastructure.Pair;
import ca.nrc.dtrc.elasticsearch.ESUrlBuilder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StreamlinedClient {
	
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	
	// Whenever the client issues a transaction that modifies the DB,
	// it will sleep by that much to give ES time to update all the 
	// nodes and shards.
	private double sleepSecs = 0.0;

	private String indexName;
		public String getIndexName() {return indexName;}
		
	private String serverName = "localhost";
	private int port = 9200;
	
	// Stores the types of the various fields for a given document type
	private static Map<String,Map<String,String>> fieldTypesCache = null;

	private static Builder requestBuilder = new Request.Builder();
	
	// As recommended in the OkHttp documentation, we use a single
	// OkHttpClient instance for all our needs.
	private static OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60,  TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.build();

	private List<StreamlinedClientObserver> observers = new ArrayList<StreamlinedClientObserver>();

	public StreamlinedClient(String _indexName) {
		initialize(_indexName);
	}

	public StreamlinedClient(String _indexName, double _sleepSecs) {
		initialize(_indexName, _sleepSecs);
	}

	public void initialize(String _indexName) {
		initialize(_indexName, 0.0);
	}

	public void initialize(String _indexName, double _sleepSecs) {
		this.indexName = canonicalIndexName(_indexName);
		this.sleepSecs = _sleepSecs;
	}
	
	public StreamlinedClient setSleepSecs(double _sleepSecs) {
		this.sleepSecs = _sleepSecs;
		return this;
	}

	public StreamlinedClient setIndex(String _indexName) {
		this.indexName = canonicalIndexName(_indexName);
		
		return this;
	}

	public StreamlinedClient setServer(String _serverName) {
		this.serverName = _serverName;
		return this;
	}

	public StreamlinedClient setPort(int _port) {
		this.port = _port;
		return this;
	}

	public String refreshIndex() throws IOException, ElasticSearchException, InterruptedException {
		return refreshIndex(null);
	}

	
	public String refreshIndex(String type) throws IOException, ElasticSearchException, InterruptedException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.refreshIndex");
		URL url = esUrlBuilder().forDocType(type).forEndPoint("_refresh").build();
		String jsonResponse = post(url);
		tLogger.trace("url="+url+", jsonResponse="+jsonResponse);
		
		return jsonResponse;
	}
	
	public String putDocument(Document doc) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.putDocument");
		tLogger.trace("putting document: "+doc.getKey());
		String jsonDoc;
		try {
			jsonDoc = new ObjectMapper().writeValueAsString(doc);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}	
		String docType = doc.getClass().getName();
		String docID = doc.getKey();
		String jsonResponse = putDocument(docType, docID, jsonDoc);
		
		return jsonResponse;
	}

	public String putDocument(String type, Document_DynTyped dynDoc) throws ElasticSearchException {
		String docID = dynDoc.getKey();
		String jsonDoc;
		try {
			jsonDoc = new ObjectMapper().writeValueAsString(dynDoc);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		String jsonResp = putDocument(type, docID, jsonDoc);
		
		return jsonResp;
	}
		
	public String putDocument(String type, String docID, String jsonDoc) throws ElasticSearchException {
		@SuppressWarnings("unused")
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.putDocument");
		URL url = esUrlBuilder().forDocType(type).forDocID(docID).build();
		String jsonResponse = post(url, jsonDoc);
		
		clearFieldTypesCache(type);		
		
		sleep();
		
		return jsonResponse;
	}	
	
	public <T extends Document> List<T> listFirstNDocuments(T docPrototype, Integer maxN) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.listAll");
		@SuppressWarnings("unchecked")
		Class<T> docClass = (Class<T>) docPrototype.getClass();
		String type = docClass.getName();
		tLogger.trace("searching for all type="+type);
		URL url = esUrlBuilder().forClass(docClass).forEndPoint("_search").scroll().build();
		String jsonResponse = post(url, "{}");
		
		Pair<Pair<Long,String>,List<Pair<T,Double>>> parsedResults = parseJsonSearchResponse(jsonResponse, docPrototype);		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		
		Long totalHits = parsedResults.getFirst().getFirst();
		List<Pair<T,Double>> firstBatch = parsedResults.getSecond();
		String scrollID = parsedResults.getFirst().getSecond();
		
		SearchResults results = new SearchResults(firstBatch, scrollID, totalHits, docPrototype, this);
		
		int count = 0;
		List<T> docs = new ArrayList<T>();
		Iterator<Pair<T,Double>> iter = results.iterator();
		while (iter.hasNext()) {
			@SuppressWarnings("unchecked")
			T nextDoc = (T) iter.next().getFirst();
			docs.add(nextDoc);
			count++;
			if (maxN != null && count > maxN) break;
		}

		
		return docs;
	}	
	
	public <T extends Document> SearchResults<T> listAll(T docPrototype) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.listAll");
		@SuppressWarnings("unchecked")
		Class<T> docClass = (Class<T>) docPrototype.getClass();
		String type = docClass.getName();
		tLogger.trace("searching for all type="+type);
		URL url = esUrlBuilder().forClass(docClass).forEndPoint("_search").scroll().build();
		String jsonResponse = post(url, "{}");
		
		SearchResults<T> results = new SearchResults<T>(jsonResponse, docPrototype, this);
		
		return results;
	}		

	public <T extends Document> SearchResults<T> listAll(String esDocTypeName , T docPrototype) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.listAll");
		tLogger.trace("searching for all type="+esDocTypeName);
		URL url = esUrlBuilder().forDocType(esDocTypeName).forEndPoint("_search").scroll().build();
		String jsonResponse = post(url, "{}");
		
		SearchResults<T> results = new SearchResults<T>(jsonResponse, docPrototype, this);
		
		return results;
	}		
	
	private String post(URL url) throws IOException, ElasticSearchException, InterruptedException {
		return post(url, null);
	}
	
	private String post(URL url, String json) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.post");
		
		if (json == null) json = "";
	    RequestBody body = RequestBody.create(JSON, json);
	    
	    Request request = requestBuilder
	        .url(url)
	        .post(body)
	        .build();
	    
	    tLogger.trace("url="+url+", body="+body.toString());
	    Response response;
		try {
			response = httpClient.newCall(request).execute();
		} catch (IOException e) {
			throw new ElasticSearchException("Could not execute ElasticSearch request", e);
		}
	    String jsonResponse;
		try {
			jsonResponse = response.body().string();
		} catch (IOException e) {
			throw new ElasticSearchException("Could not retrieve response for ElasticSearch request", e);
		}

	    checkForESErrorResponse(jsonResponse);
	    
	    return jsonResponse;
	  }
	
	private String get(URL url) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.get");
		Request request = requestBuilder.get()
		      .url(url)
		      .build();

		Response response;
		String jsonResponse;
		try {
			response = httpClient.newCall(request).execute();
			jsonResponse = response.body().string();			
		} catch (IOException exc) {
			String message = exc.getMessage();
			if (message.startsWith("Failed to connect to ")) {
				message = "Failed to connect to ElasticSearch server at url="+url;
			}
			throw new ElasticSearchException(message, exc);
		}
		
		  
		checkForESErrorResponse(jsonResponse);
		  
		tLogger.trace("returning: "+jsonResponse);
		  
		return jsonResponse;		
	}
	
	private String put(URL url, String json) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.put");
		
		if (json == null) json = "";
	    RequestBody body = RequestBody.create(JSON, json);
	    
	    
	    Request request = requestBuilder
	        .url(url.toString())
	        .put(body)
	        .build();
	    
	    if (tLogger.isTraceEnabled()) tLogger.trace("url="+url);
	    Response response;
	    String jsonResponse;
		try {
			response = httpClient.newCall(request).execute();
		    jsonResponse = response.body().string();			
		} catch (IOException exc) {
			throw new ElasticSearchException(exc);
		}

	    checkForESErrorResponse(jsonResponse);
	    
	    return jsonResponse;
	  }	
	
	private void delete(URL url) throws ElasticSearchException {
		delete(url, "");
	}

	
	private void delete(URL url, String jsonBody) throws ElasticSearchException{
		@SuppressWarnings("unused")
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.delete");
		if (jsonBody == null) jsonBody = "";
		Request request = requestBuilder
				.delete()
				.url(url)
				.build();
		

		Response response = null;
		String jsonResponse = null;
		try {
			response = httpClient.newCall(request).execute();
			jsonResponse = response.body().string();			
		} catch (IOException e) {
			throw new ElasticSearchException("Cannot execute ElasticSearch HTTP request "+url.toString());
		}

		checkForESErrorResponse(jsonResponse);
	}
	
	private void checkForESErrorResponse(String jsonResponse) throws ElasticSearchException {
		ElasticSearchException exception = null;
		
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> responseObj = new HashMap<String,Object>();
		
		try {
			responseObj = mapper.readValue(jsonResponse, responseObj.getClass());
		} catch (Exception exc) {
			// jsonResponse is not a JSON object. So it must be a plain old string
			// (as opposed to a JSON string), containing an error message
			// issued by ElasticSearch
			Map<String,Object> excDetails = new HashMap<String,Object>();
			excDetails.put("error", jsonResponse);
			exception =  new ElasticSearchException(excDetails);
		}
		
		if (exception == null && responseObj.containsKey("error")) {
			exception = new ElasticSearchException(responseObj);
		}
		
		if (exception != null) throw exception;
		
	}
	
	public <T extends Document> SearchResults<T> search(String jsonQuery, T docPrototype) throws ElasticSearchException {
		
//		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.search");
//		URL url = esUrlBuilder()
//					.forClass(docPrototype.getClass()).forEndPoint("_search")
//					.scroll().build();
//		tLogger.trace("url="+url+", jsonQuery="+jsonQuery);
//		String jsonResponse = post(url, jsonQuery);
//		
//		@SuppressWarnings({ "unchecked", "rawtypes" })
//		Pair<String,List<Pair<T,Double>>> parsedResults = parseJsonSearchResponse(jsonResponse, docPrototype);		
//		SearchResults results = new SearchResults(parsedResults.getSecond(), parsedResults.getFirst(), docPrototype, this);
//		
//		return results;
		String docTypeName = docPrototype.getClass().getName();
		SearchResults<T> hits = search(jsonQuery, docTypeName, docPrototype);
		
		return hits;
	}	

	public <T extends Document> SearchResults<T> search(String jsonQuery, String docTypeName, T docPrototype) throws ElasticSearchException {
		
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.search");
		URL url = esUrlBuilder()
					.forDocType(docTypeName).forEndPoint("_search")
					.scroll().build();
		tLogger.trace("url="+url+", jsonQuery="+jsonQuery);
		String jsonResponse = post(url, jsonQuery);

		SearchResults<T> results = new SearchResults<T>(jsonResponse, docPrototype, this);
				
		return results;
	}
	
	public <T extends Document> SearchResults<T> searchFreeform(String query, String docTypeName, T docPrototype) throws ElasticSearchException {
		String jsonQuery = "{\"query\": {\"query_string\": {\"query\": \""+query+"\"}}}\n";
		SearchResults<T> hits = search(jsonQuery, docTypeName, docPrototype);
		
		return hits;
	}
	
	public <T extends Document> SearchResults<T> searchFreeform(String query, T docPrototype) throws ElasticSearchException {
		String docTypeName = docPrototype.getClass().getName();
		SearchResults<T> hits = searchFreeform(query, docTypeName, docPrototype);
		
		return hits;
	}
	
	public DocClusterSet clusterDocuments(String query, String docTypeName, String[] useFields, String algName, Integer maxDocs) throws ElasticSearchException, JsonProcessingException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.clusterDocuments");
		URL url = esUrlBuilder().forDocType(docTypeName)
					.forEndPoint("_search_with_clusters").build();

		String jsonQuery = clusterDocumentJsonBody(query, docTypeName, useFields, algName, maxDocs);
		String jsonResponse = post(url, jsonQuery);
		tLogger.trace("** ur="+url+"\njsonQuery="+jsonQuery);
		tLogger.trace("** Received jsonResponse="+jsonResponse);
		
		DocClusterSet clusters = parseClusterResponse(jsonResponse, docTypeName);
		
		return clusters;
	}

	protected String clusterDocumentJsonBody(String freeformQuery, String docTypeName, String[] useFields, String algName, Integer maxDocs) throws ElasticSearchException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper(); 
		JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

		ObjectNode root = nodeFactory.objectNode();
		try {
			ObjectNode searchRequest = nodeFactory.objectNode();
			root.set("search_request", searchRequest);
			{
				ObjectNode query = nodeFactory.objectNode();
				searchRequest.set("query", query);
				{	
					ObjectNode queryString = nodeFactory.objectNode();
					query.set("query_string", queryString);
					{
						queryString.put("query", freeformQuery);
					}
				}
				searchRequest.put("size", maxDocs);
			}
			root.put("query_hint", "");
			
			root.put("algorithm", algName);
			
			ArrayNode source = nodeFactory.arrayNode();
			for (int ii=0; ii < useFields.length; ii++) source.add(useFields[ii]);
			root.set("_source", source);

			ObjectNode fieldMapping = nodeFactory.objectNode();
			root.set("field_mapping", fieldMapping);
			{
				ArrayNode content = nodeFactory.arrayNode();
				for (int ii=0; ii < useFields.length; ii++) content.add("_source."+useFields[ii]);
				fieldMapping.set("content", content);
			}		
		} catch (Exception exc) {
			throw new ElasticSearchException(exc);
		}
		
		String jsonBody = mapper.writeValueAsString(root);

		return jsonBody;
	}
	
	private DocClusterSet parseClusterResponse(String jsonClusterResponse, String docTypeName) throws ElasticSearchException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonRespNode;
		DocClusterSet clusters = new DocClusterSet(getIndexName(), docTypeName);
		try {
			jsonRespNode = (ObjectNode) mapper.readTree(jsonClusterResponse);	
			ArrayNode clustersNode = (ArrayNode) jsonRespNode.get("clusters");
			for (int ii=0; ii < clustersNode.size(); ii++) {
				ObjectNode aClusterNode = (ObjectNode) clustersNode.get(ii);	
				String clusterName = aClusterNode.get("label").asText();
				ArrayNode documentIDsNode = (ArrayNode) aClusterNode.get("documents");
				for (int jj=0; jj < documentIDsNode.size(); jj++) {
					String docID = documentIDsNode.get(jj).asText();
					clusters.addToCluster(clusterName, docID);
				}
			}

		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}					
		
		return clusters;
	}	

	public <T extends Document> List<Pair<T,Double>> scroll(String scrollID, T docPrototype) throws ElasticSearchException {
		URL url = esUrlBuilder().forEndPoint("_search/scroll").build();
		
		Map<String,String> postJson = new HashMap<String,String>();
		{
			postJson.put("scroll_id", scrollID);
			postJson.put("scroll", "1m");
		}
		String jsonResponse = null;
		try {
			jsonResponse = post(url, new ObjectMapper().writeValueAsString(postJson));
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		
		Pair<Pair<Long,String>,List<Pair<T,Double>>> parsedResults = parseJsonSearchResponse(jsonResponse, docPrototype);
 		
		return parsedResults.getSecond();
	}
	
	private <T extends Document> Pair<Pair<Long,String>,List<Pair<T, Double>>> parseJsonSearchResponse(String jsonSearchResponse, T docPrototype) throws ElasticSearchException {
		List<Pair<T, Double>> scoredDocuments = new ArrayList<Pair<T,Double>>();
		String scrollID = null;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonRespNode;
		Long totalHits;
		try {
			jsonRespNode = (ObjectNode) mapper.readTree(jsonSearchResponse);	
			scrollID = jsonRespNode.get("_scroll_id").asText();
			ObjectNode hitsCollectionNode = (ObjectNode) jsonRespNode.get("hits");
			totalHits = hitsCollectionNode.get("total").asLong();
			ArrayNode hitsArrNode = (ArrayNode) hitsCollectionNode.get("hits");
			for (int ii=0; ii < hitsArrNode.size(); ii++) {
				String hitJson = hitsArrNode.get(ii).get("_source").toString();
				T hitObject = (T) mapper.readValue(hitJson, docPrototype.getClass());
				Double hitScore = hitsArrNode.get(ii).get("_score").asDouble();
				
				scoredDocuments.add(Pair.of(hitObject, hitScore));
			}
		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}			
		
		return Pair.of(Pair.of(totalHits, scrollID), scoredDocuments);
	}

	public String clearIndex() throws IOException, ElasticSearchException, InterruptedException {
		return clearIndex(true);
	}

	public String clearIndex(Boolean failIfIndexNotFound) throws IOException, ElasticSearchException, InterruptedException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.clearIndex");	
		tLogger.trace("invoked");
		
		URL url = esUrlBuilder().forEndPoint("_delete_by_query").build();
		String jsonInput = 
			  "{\n"
			+ "  \"query\": {\n"
			+ "    \"match_all\": {}\n"
			+ "  }\n"
			+ "}"
			;
		String jsonResp = "{}";
		
		tLogger.trace("url="+url+", jsonInput="+jsonInput);
		try {
			jsonResp = post(url, jsonInput);			
		} catch (Exception exc) {
			if (failIfIndexNotFound) throw exc;
		}
		
		sleep();
		
		return jsonResp;
	}		
	
	public void deleteIndex() throws ElasticSearchException {
		URL url = esUrlBuilder().build();
		
		try {
			delete(url);	
			clearFieldTypesCache();
		} catch (ElasticSearchException exc) {
			if (exc.getMessage().contains("index_not_found_exception")) {
				// OK... we tried to delete an index that did not exist
			} else {
				// This was something else... so re-throw the exception
				throw exc;
			}
		}
	}

	public <T extends Document> SearchResults<T> moreLikeThis(T queryDoc) throws ElasticSearchException, IOException, InterruptedException {
		SearchResults<T> results = moreLikeThis(queryDoc, null);
		return results;
	}
	
	public <T extends Document> SearchResults<T> moreLikeThis(T queryDoc, FieldFilter fldFilter) throws ElasticSearchException, IOException, InterruptedException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.moreLikeThis_NEW");
		
		Map<String,Object> queryDocMap = null;
		
		if (queryDoc instanceof Map<?,?>) {
			// The query document was specified as an "untyped" map.
			// Just remove the fields to be ignored
			queryDocMap = new HashMap<String,Object>();
			Map<String,Object> queryDocCast = (Map<String,Object>) queryDoc;
			for (String fieldName: queryDocCast.keySet()) {
				queryDocMap.put(fieldName, queryDocCast.get(fieldName));
			}
		} else {
			// The query document was specified as a typed object
			// Convert it to a map a map
			queryDocMap = filterFields(queryDoc, fldFilter);
		}
		
		String esType = queryDoc.getClass().getName();
		String mltBody = moreLikeThisJsonBody(esType, queryDocMap);
		if (tLogger.isTraceEnabled()) tLogger.trace("** queryDocMap="+PrettyPrinter.print(queryDocMap));
		
		SearchResults results = null;
		results = search(mltBody, queryDoc);
	
		return results;
	}		
	
	protected String moreLikeThisJsonBody(String type, Map<String, Object> queryDoc) throws ElasticSearchException {
		ObjectMapper mapper = new ObjectMapper(); 
		JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

		ObjectNode root = nodeFactory.objectNode();
		try {
			ObjectNode query = nodeFactory.objectNode();
			root.set("query", query);
			{
				ObjectNode mlt = nodeFactory.objectNode();
				query.set("more_like_this", mlt);
				{
					mlt.put("min_term_freq", 1);
					mlt.put("max_query_terms",12);
					
					ArrayNode fields = nodeFactory.arrayNode();
					mlt.set("fields", fields);
					
					ObjectNode like = nodeFactory.objectNode();
					mlt.set("like", like);
					{
						like.put("_index", indexName);
						like.put("_type", type);
						ObjectNode doc = nodeFactory.objectNode();
						like.set("doc", doc);
						{
							for (String fieldName: queryDoc.keySet()) {
								// Ignore all but the 'text' fields
								String fieldType = getFieldType(fieldName, type);
								if (fieldType != null && fieldType.equals("text")) {
									fields.add(fieldName);
									Object fieldValue = queryDoc.get(fieldName);
									String json = mapper.writeValueAsString(fieldValue);
									JsonNode jsonNode = mapper.readTree(json);			
									doc.set(fieldName, jsonNode);
								}
							}
						}
					}
				}
			}
		
		} catch (Exception exc) {
			throw new ElasticSearchException(exc);
		}
		
		String jsonBody = root.toString();
		
		return jsonBody;
	}

	protected static Map<String, Object> filterFields(Object queryDoc) throws ElasticSearchException {
		return filterFields(queryDoc, null);
	}

	protected static Map<String, Object> filterFields(Map<String,Object> queryDoc) throws ElasticSearchException {
		return filterFields(queryDoc, null);
	}
	
	
	protected static Map<String, Object> filterFields(Map<String,Object> queryDoc, FieldFilter filter) {
		Map<String,Object> objMap = new HashMap<String,Object>();
		for (String fieldName: queryDoc.keySet()) {
			if (filter == null || filter.keepField(fieldName)) {
				objMap.put(fieldName, queryDoc.get(fieldName));
			}
		}
		
		return objMap;
	}

	protected static Map<String, Object> filterFields(Object queryDoc, FieldFilter filter) throws ElasticSearchException {
		Map<String,Object> objMap = new HashMap<String,Object>();
		
		Field[] fields = queryDoc.getClass().getFields();

		for (int ii=0; ii < fields.length; ii++) {
			Field aField = fields[ii];
			String fieldName = aField.getName();
			if (filter == null || filter.keepField(fieldName)) {
				try {
					objMap.put(fieldName, aField.get(queryDoc));
				} catch (IllegalArgumentException | IllegalAccessException exc) {
					throw new ElasticSearchException(exc);
				}
			}
		}
			
		return objMap;
	}
	

	public void sleep()  {
		try {
			sleep(this.sleepSecs);
		} catch (InterruptedException e) {
			System.exit(0);
		}
	}
	
	public void sleep(double secs) throws InterruptedException {
		int millis = (int) (1000 * secs);
		Thread.sleep(millis);
	}

	public void bulk(String jsonFPath, Class<? extends Document> docClass) throws ElasticSearchException, IOException {
		bulk(new File(jsonFPath), docClass);
	}


	public void bulk(File jsonFile, Class<? extends Document> docClass) throws ElasticSearchException, IOException {
		String docTypeName = docClass.getName();
		List<String> jsonLines = Files.readAllLines(jsonFile.toPath());
		String json = String.join("\n", jsonLines);
		bulk(json, docTypeName);
	}

	public void bulk(String jsonContent, String docTypeName) throws ElasticSearchException, IOException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.bulk");
		URL url = esUrlBuilder().forDocType(docTypeName).forEndPoint("_bulk").build();
		tLogger.trace("url="+url);
		put(url, jsonContent);
		
		// A bulk operation may have changed the properties of different document types in different indices
		clearFieldTypesCache();
	}
	
	public void bulkIndex(String dataFPath, String docTypeName) throws ElasticSearchException {
		bulkIndex(dataFPath, docTypeName, -1, false);
	}

	public void bulkIndex(String dataFPath, String docTypeName, Boolean verbose) throws ElasticSearchException {
		bulkIndex(dataFPath, docTypeName, -1, verbose);
	}
	
	public void bulkIndex(String dataFPath, String docTypeName, int batchSize, boolean verbose) throws ElasticSearchException {
		String id = null;
		try {
			if (batchSize < 0) batchSize = 100;
			int batchStart = 1;
			File dataFile = new File(dataFPath);			
			BufferedReader br = new BufferedReader(new FileReader(dataFile));
			int currBatchSize = 0;
			String jsonBatch = "";
			String  jsonLine = br.readLine();
			while (jsonLine != null) {
				id = getLineID(jsonLine, verbose);
				jsonBatch += 
					"\n{\"index\": {\"_index\": \""+indexName+"\", \"_type\" : \""+docTypeName+"\", \"_id\": \""+id+"\"}}" +
					"\n" + jsonLine;
				
				if (currBatchSize > batchSize) {
					for (StreamlinedClientObserver obs: observers) {
						obs.onBulkIndex(batchStart, batchStart+currBatchSize, indexName, docTypeName);
					}
					bulk(jsonBatch, docTypeName);
					batchStart += currBatchSize;
					currBatchSize = 0;
					jsonBatch = "";
				} else {
					currBatchSize++;
				}
				
				jsonLine = br.readLine();
			}
		} catch (FileNotFoundException e) {
			throw new ElasticSearchException("Could not open file "+dataFPath+" for bulk indexing.");
		} catch (IOException e) {
			throw new ElasticSearchException("Could not read from data file "+dataFPath, e);
		} catch (ElasticSearchException e) {
			throw(e);
		}
		
	}
	
	private String getLineID(String jsonLine, boolean verbose) throws ElasticSearchException {
		Document_DynTyped doc = null;
		try {
			doc = (Document_DynTyped) new ObjectMapper().readValue(jsonLine, Document_DynTyped.class);
			if (verbose) {
				System.out.println("Indexing doc with ID "+doc.getKey());
			}
		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}		
		String id =  doc.getKey();
		
		return id;
	}

	public Document getDocumentWithID(String docID, Class<?extends Document> docClass) throws ElasticSearchException {
		return getDocumentWithID(docID, docClass, null);
	}
	
	
	public Document getDocumentWithID(String docID, Class<?extends Document> docClass, String esDocType) throws ElasticSearchException {
		if (esDocType == null) {
			esDocType = docClass.getName();
		}
		Document doc = null;
		
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.getDocumentWithID");
		URL url = esUrlBuilder().forDocType(esDocType).forDocID(docID).build();
		tLogger.trace("url="+url);
		
		ObjectMapper mapper = new ObjectMapper();
		
		
		String jsonResp = get(url);
		try {
			ObjectNode respNode  = mapper.readValue(jsonResp, ObjectNode.class);
			JsonNode sourceNode = respNode.get("_source");
			if (sourceNode != null) {
				doc = mapper.treeToValue(sourceNode, docClass);
			}
		} catch (IOException exc) {
			throw new ElasticSearchException(exc);
		}

		return doc;
	}
	
	private String canonicalIndexName(String origIndexName) {
		String canonical = origIndexName;
		canonical = canonical.toLowerCase();
		
		return canonical;
	}

	public Map<String,String> getFieldTypes(Class<? extends Document> docClass) throws ElasticSearchException {
		return getFieldTypes(docClass.getName());
	}

	public String getFieldType(String fieldName, String docType) throws ElasticSearchException {
		String fieldType = null;
		
		Map<String,String> allFieldTypes = getFieldTypes(docType);
		if (allFieldTypes.containsKey(fieldName)) {
			fieldType = allFieldTypes.get(fieldName);
		}
		
		return fieldType;
	}
	
	
	public Map<String,String> getFieldTypes(String type) throws ElasticSearchException {
		Map<String,String> fieldTypes = uncacheFieldTypes(type);
		if (fieldTypes == null) {
			fieldTypes = new HashMap<String,String>();
			URL url = esUrlBuilder().forDocType(type)
						.forEndPoint("_mapping")
						.endPointBeforeType(true).build();
			String jsonResponse = get(url);
			ObjectNode oNode = objectNode();
			ObjectMapper mapper = new ObjectMapper();
			try {
				oNode = mapper.readValue(jsonResponse, oNode.getClass());
			} catch (IOException exc) {
				throw new ElasticSearchException(exc);
			}
			
			ObjectNode fieldsProps = (ObjectNode) oNode.get(indexName).get("mappings").get(type).get("properties");
			
			Iterator<Entry<String, JsonNode>> iterator = fieldsProps.fields();
			while (iterator.hasNext()) {
				Entry<String, JsonNode> entry = iterator.next();
			    String aFldName = entry.getKey();
			    JsonNode aFldProps = entry.getValue();
			    String aFldType = null;
			    if (aFldProps.has("type")) {
			    	aFldType = aFldProps.get("type").asText();
			    } else {
			    	aFldType = "_EMBEDDED_STRUCTURE";
			    }
			    fieldTypes.put(aFldName, aFldType);
			}
			
			cacheFieldTypes(fieldTypes, type);
		}
		
		return fieldTypes;
	}
	
	private static Map<String,String> uncacheFieldTypes(String docClassName) {
		Map<String,String> fieldTypes = null;
		if (fieldTypesCache != null && fieldTypesCache.containsKey(docClassName)) {
			fieldTypes = fieldTypesCache.get(docClassName);
		}
		return fieldTypes;
	}
	
	public static void cacheFieldTypes(Map<String,String> types, String docClassName) {
		if (fieldTypesCache == null) {
			fieldTypesCache = new HashMap<String,Map<String,String>>();
		}
		fieldTypesCache.put(docClassName, types);
	}

	private static void clearFieldTypesCache(Class<? extends Document> docClass) {
		clearFieldTypesCache(docClass.getName());
	}	
	
	private static void clearFieldTypesCache(String docTypeName) {
		if (fieldTypesCache != null) {
			fieldTypesCache.put(docTypeName, null);
		}		
	}
	
	
	private static void clearFieldTypesCache() {
		fieldTypesCache = null;
	}	

	private ObjectNode objectNode() {
		return new ObjectMapper().createObjectNode();
	}

	public void updateDocument(Class<? extends Document> docClass, String docID, Map<String, Object> partialDoc) throws ElasticSearchException {
		URL url = esUrlBuilder().forClass(docClass).forDocID(docID)
				    .forEndPoint("_update").build();
		String jsonBody = null;
		Map<String,Object> jsonData = new HashMap<String,Object>();
		jsonData.put("doc", partialDoc);
		try {
			jsonBody = new ObjectMapper().writeValueAsString(jsonData);
		} catch (JsonProcessingException exc) {
			throw new ElasticSearchException(exc);
		}
		
		post(url, jsonBody);
	}
	
	private ESUrlBuilder esUrlBuilder() {
		ESUrlBuilder builder = new ESUrlBuilder(indexName, serverName, port);
		return builder;
	}
	
	public <T extends Document> void dumpToFile(File outputFile, String freeformQuery, String docTypeName, T docPrototype) throws ElasticSearchException {
		try {			
			SearchResults<T> results = searchFreeform(freeformQuery, docTypeName, docPrototype);
			dumpToFile(outputFile, results);
		} catch (Exception e) {
			throw new ElasticSearchException(e);
		}
	}
	
	public void dumpToFile(String fPath, Class<? extends Document> docClass) throws ElasticSearchException {		
		try {
			Document docPrototype = docClass.getDeclaredConstructor().newInstance();
			
			@SuppressWarnings("unchecked")
			SearchResults<? extends Document> allDocs = listAll(docPrototype);
			File outputFile = new File(fPath);
			dumpToFile(outputFile, allDocs);
		} catch (Exception e) {
			throw new ElasticSearchException(e);
		}
	}	

	
	private void dumpToFile(File outputFile, SearchResults<? extends Document> results) throws ElasticSearchException {
		try {
			FileWriter fWriter = new FileWriter(outputFile);
			ObjectMapper mapper = new ObjectMapper();
			Iterator<?> iter = results.iterator();
			while (iter.hasNext()) {
				Pair<Document,Double> aScoredDoc = (Pair<Document,Double>)iter.next();
				String json = mapper.writeValueAsString(aScoredDoc.getFirst());
				fWriter.write(json+"\n");
			}
			fWriter.close();
		} catch (Exception e) {
			throw new ElasticSearchException(e);
		}	
	}


	public void createIndex(String emptytestindex) throws ElasticSearchException {
		URL url = esUrlBuilder().build();
		
		put(url, null);
	
		int x = 0;
		
	}
	
	public void attachObserver(StreamlinedClientObserver _obs) {
		observers.add(_obs);
	}

}
