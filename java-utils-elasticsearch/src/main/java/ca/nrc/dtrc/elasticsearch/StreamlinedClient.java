package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.ESConfig;

import java.beans.IntrospectionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.nrc.json.PrettyPrinter;
import ca.nrc.config.ConfigException;
import ca.nrc.data.file.ObjectStreamReader;
import ca.nrc.datastructure.Pair;
import ca.nrc.dtrc.elasticsearch.ESUrlBuilder;
import ca.nrc.introspection.Introspection;
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
	
	public void defineIndex(IndexDef iDef) throws ElasticSearchException {
		Map<String,Object> indexMappings = iDef.indexMappings();
		Map<String,Object> indexSettings = iDef.indexSettings();
		defineIndex(indexSettings, indexMappings);
	}
	
	
	public void defineIndex(Map<String, Object> indexSettings, Map<String, Object> indexMappings) throws ElasticSearchException {
		
		if (indexExists()) {
			deleteIndex();
		}
		
		String jsonString;
		try {
			jsonString = new ObjectMapper().writeValueAsString(indexMappings);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		
		URL url = esUrlBuilder().build();
		String json = put(url, jsonString);
				
		try {
			jsonString = new ObjectMapper().writeValueAsString(indexSettings);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		
		url = esUrlBuilder().forEndPoint("_settings").build();
		json = put(url, jsonString);
		
		
		
		return;
	}

	private Map<String, Object> indexSettings() throws ElasticSearchException {
		URL url = esUrlBuilder().forEndPoint("_settings").build();
		String json = get(url);
		Map<String,Object> settings = new HashMap<String,Object>();
		try {
			settings = new ObjectMapper().readValue(json, settings.getClass());
			settings = (Map<String,Object>)(settings.get(indexName));
			settings = (Map<String,Object>)(settings.get("settings"));
			
		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}
		
		return settings;
	}

	private boolean indexExists() {
		URL url = null;
		boolean exists = true;
		try {
			url = esUrlBuilder().forEndPoint("_search").build();
		} catch (ElasticSearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			get(url);
		} catch (ElasticSearchException e) {
			exists = false;
		}
		
		return exists;
	}

	public void defineFieldTypes(Map<String,String> typeDefs) throws ElasticSearchException {
		Map<String,Map<String,String>> mappingsDict = new HashMap<String,Map<String,String>>();
		for (String typeName: typeDefs.keySet()) {
			Map<String,String> aMapping = new HashMap<String,String>();
			
			String aType = typeDefs.get(typeName).toLowerCase();
			if (! aType.matches("^(date|text|keyword)$")) {
				throw new ElasticSearchException("Unknown ElasticSearch field type "+aType);
			}
			aMapping.put("type", aType);
			mappingsDict.put(typeName, aMapping);
		}
		
		String jsonString;
		
		try {
			jsonString = new ObjectMapper().writeValueAsString(mappingsDict);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		jsonString = "{\"mappings\": {\"type_name\": {\"properties\": "+jsonString+"}}}";
		
		
		URL url = esUrlBuilder().build();
		String json = put(url, jsonString);
		
		return;
	}	
	
	public String putDocument(Document doc) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.putDocument");
		tLogger.trace("putting document: "+doc.getId());
		String jsonDoc;
		try {
			jsonDoc = new ObjectMapper().writeValueAsString(doc);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}	
		String docType = doc.getClass().getName();
		String docID = doc.getId();
		String jsonResponse = putDocument(docType, docID, jsonDoc);
		
		return jsonResponse;
	}

	public String putDocument(String type, Document dynDoc) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.putDocument");
		tLogger.trace("type="+type);
		String docID = dynDoc.getId();
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
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.putDocument");
		URL url = esUrlBuilder().forDocType(type).forDocID(docID).build();
		tLogger.trace("posting url="+url+",jsonDoc=\n"+jsonDoc);
		
		String jsonResponse = post(url, jsonDoc);
		
		clearFieldTypesCache(type);		
		
		sleep();
		return jsonResponse;
	}	
	
	public void deleteDocumentWithID(String docID, Class<?extends Document> docClass) throws ElasticSearchException {
		deleteDocumentWithID(docID, docClass.getName());
	}
	
	public void deleteDocumentWithID(String docID, String esDocType) throws ElasticSearchException {
		URL url = esUrlBuilder().forDocType(esDocType).forDocID(docID).build();
		delete(url);
		sleep();
	}

	
	public <T extends Document> List<T> listFirstNDocuments(T docPrototype, Integer maxN) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.listAll");
		@SuppressWarnings("unchecked")
		Class<T> docClass = (Class<T>) docPrototype.getClass();
		String type = docClass.getName();
		tLogger.trace("searching for all type="+type);
		URL url = esUrlBuilder().forClass(docClass).forEndPoint("_search").scroll().build();
		String jsonResponse = post(url, "{}");
		
		Pair<Pair<Long,String>,List<Hit<T>>> parsedResults 
						= parseJsonSearchResponse(jsonResponse, docPrototype);		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		
		Long totalHits = parsedResults.getFirst().getFirst();
		List<Hit<T>> firstBatch = parsedResults.getSecond();
		String scrollID = parsedResults.getFirst().getSecond();
		
		SearchResults results = new SearchResults(firstBatch, scrollID, totalHits, docPrototype, this);
		
		int count = 0;
		List<T> docs = new ArrayList<T>();
		Iterator<Hit<T>> iter = results.iterator();
		while (iter.hasNext()) {
			@SuppressWarnings("unchecked")
			T nextDoc = (T) iter.next().getDocument();
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
		
		tLogger.trace("invoking url="+url);
		String jsonResponse = post(url, "{}");
		
		SearchResults<T> results = new SearchResults<T>(jsonResponse, docPrototype, this);
		
		return results;
	}		
	
	public String post(URL url) throws IOException, ElasticSearchException, InterruptedException {
		return post(url, null);
	}
	
	public String post(URL url, String json) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.post");
		tLogger.trace("posting url="+url+", with json=\n"+json);
		
		if (json == null) json = "";
	    RequestBody body = RequestBody.create(JSON, json);
	    
	    Request request = requestBuilder
	        .url(url)
	        .post(body)
	        .build();
	    
	    Response response;
		try {
			tLogger.trace("** executing http request");
			response = httpClient.newCall(request).execute();
			tLogger.trace("** DONEexecuting http request");
		} catch (IOException e) {
			tLogger.trace("** Caught exception e="+e.getLocalizedMessage());
			throw new ElasticSearchException("Could not execute ElasticSearch request.\n  url="+url+"\n   body="+body.toString(), e);
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
	
	public String put(URL url, String json) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.put");
		tLogger.trace("putting url="+url+", with json=\n"+json);
		
		if (json == null) json = "";
	    RequestBody body = RequestBody.create(JSON, json);
	    
	    
	    Request request = requestBuilder
	        .url(url.toString())
	        .put(body)
	        .build();
	    
	    Response response;
	    String jsonResponse;
		try {
			tLogger.trace("** making the http call");
			response = httpClient.newCall(request).execute();
			tLogger.trace("** DONEmaking the http call");
		    jsonResponse = response.body().string();			
		} catch (IOException exc) {
			tLogger.trace("** There was an exception making the call: exc="+exc.getLocalizedMessage());
			throw new ElasticSearchException(exc);
		}

	    checkForESErrorResponse(jsonResponse);
	    
	    return jsonResponse;
	  }	
	
	public void delete(URL url) throws ElasticSearchException {
		delete(url, "");
	}

	
	public void delete(URL url, String jsonBody) throws ElasticSearchException{
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
	
	public static void checkForESErrorResponse(String jsonResponse) throws ElasticSearchException {
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
		String jsonQuery = null;
		
		query = escapeQuotes(query);
		
		if (query == null) {
			jsonQuery= "{}";
		} else {
			jsonQuery = 
					"{"+
					   "\"query\": {"+
							"\"query_string\": {\"query\": \""+query+"\"}"+
						"},"+
						"\"highlight\": {"+
							"\"fields\": {\"longDescription\": {}}"+
						"}"+
					"}"
					;
		}
		SearchResults<T> hits = search(jsonQuery, docTypeName, docPrototype);
		
		return hits;
	}
	
	protected String escapeQuotes(String query) {
		
		Matcher matcher = Pattern.compile("\"").matcher(query);
		while (matcher.find()) {
			int x = 1;
		}
		String escQuery = matcher.replaceAll("\\\\\"");
			
		return escQuery;
	}

	public <T extends Document> SearchResults<T> searchFreeform(String query, T docPrototype) throws ElasticSearchException {
		String docTypeName = docPrototype.getClass().getName();
		SearchResults<T> hits = searchFreeform(query, docTypeName, docPrototype);
		
		return hits;
	}
	
	public DocClusterSet clusterDocuments(String query, String docTypeName, String[] useFields, String algName, Integer maxDocs) throws ElasticSearchException {
		URL url = esUrlBuilder().forDocType(docTypeName)
					.forEndPoint("_search_with_clusters").build();

		String jsonQuery;
		try {
			jsonQuery = clusterDocumentJsonBody(query, docTypeName, useFields, algName, maxDocs);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		String jsonResponse = post(url, jsonQuery);
		
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
				ArrayNode source = nodeFactory.arrayNode();
				searchRequest.set("_source", source);
				{
					for (int ii=0; ii < useFields.length; ii++) source.add(useFields[ii]);
				}
				
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

	public <T extends Document> List<T> scroll(String scrollID, T docPrototype) throws ElasticSearchException {
		List<Hit<T>> scoredHits = scrollScoredHits(scrollID, docPrototype);
		List<T> unscoredHits = new ArrayList<T>();
		for (Hit<T> aScoredHit: scoredHits) {
			unscoredHits.add(aScoredHit.getDocument());
		}
		
		return unscoredHits;
	}
	
	public <T extends Document> List<Hit<T>> scrollScoredHits(String scrollID, T docPrototype) throws ElasticSearchException {
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
		
		Pair<Pair<Long,String>,List<Hit<T>>> parsedResults = parseJsonSearchResponse(jsonResponse, docPrototype);
 		
		return parsedResults.getSecond();
	}

	private <T extends Document> Pair<Pair<Long,String>,List<Hit<T>>> parseJsonSearchResponse(String jsonSearchResponse, T docPrototype) throws ElasticSearchException {	
		List<Hit<T>> scoredDocuments = new ArrayList<>();
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
				
				scoredDocuments.add(new Hit<T>(hitObject, hitScore, hitsArrNode.get(ii).get("highlight")));
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
		return moreLikeThis(queryDoc, fldFilter, null);
	}

	public <T extends Document> SearchResults<T> moreLikeThis(T queryDoc, FieldFilter fldFilter, String esDocTypeName) throws ElasticSearchException, IOException, InterruptedException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.moreLikeThis_NEW");
		
		Map<String,Object> queryDocMap = null;
		queryDocMap = filterFields(queryDoc, esDocTypeName, fldFilter);
		
		String esType = esDocTypeName;
		if (esType == null) esType = queryDoc.getClass().getName();
		String mltBody = moreLikeThisJsonBody(esType, queryDocMap);
		if (tLogger.isTraceEnabled()) tLogger.trace("** queryDocMap="+PrettyPrinter.print(queryDocMap));
		
		SearchResults<T> results = search(mltBody, esType, queryDoc);
	
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
					mlt.put("min_doc_freq", 1);
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
			//Snippets
			ObjectNode highlight = nodeFactory.objectNode();
			root.set("highlight", highlight);
			{
				highlight.put("order", "score");
				
				ObjectNode fields = nodeFactory.objectNode();
				highlight.set("fields", fields);
				{
					ObjectNode description = nodeFactory.objectNode();
					fields.set("longDescription", description);
					{
						description.put("type", "plain");						
					}
					
					ObjectNode shortDesc = nodeFactory.objectNode();
					fields.set("shortDescription", shortDesc);
					{
						shortDesc.put("type", "plain");
					}
				}
			}
		
		} catch (Exception exc) {
			throw new ElasticSearchException(exc);
		}
		
		String jsonBody = root.toString();
		
		return jsonBody;
	}
	
	public <T extends Document> SearchResults<T> moreLikeThese(List<T> queryDocs) throws ElasticSearchException, IOException, InterruptedException {		
		return moreLikeThese(queryDocs, null, null);
	}
	
	public <T extends Document> SearchResults<T> moreLikeThese(List<T> queryDocs, FieldFilter fldFilter) throws ElasticSearchException, IOException, InterruptedException {
		return moreLikeThese(queryDocs, fldFilter, null);
	}
	
	public <T extends Document> SearchResults<T> moreLikeThese(List<T> queryDocs, FieldFilter fldFilter, String esDocTypeName) throws ElasticSearchException, IOException, InterruptedException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.moreLikeThisese");
		
		List<Map<String,Object>> queryDocMaps = null;
		queryDocMaps = filterFields(queryDocs, esDocTypeName, fldFilter);
		
		String esType = esDocTypeName;
		if (esType == null) esType = queryDocs.get(0).getClass().getName();
		String mltBody = moreLikeTheseJsonBody(esType, queryDocMaps);
		
		
		SearchResults results = null;
		results = search(mltBody, esDocTypeName, queryDocs.get(0));
	
		return results;
	}				
	

	private String moreLikeTheseJsonBody(String type, List<Map<String, Object>> queryDocMaps) throws ElasticSearchException {
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
					
					ArrayNode like = nodeFactory.arrayNode();
					mlt.set("like", like);
					{
						for (Map<String,Object> aQueryDoc: queryDocMaps) {
							ObjectNode queryDocDef = nodeFactory.objectNode();
							like.add(queryDocDef);
							queryDocDef.put("_index", indexName);
							queryDocDef.put("_type", type);
							ObjectNode doc = nodeFactory.objectNode();
							queryDocDef.set("doc", doc);
							for (String fieldName: aQueryDoc.keySet()) {
								// Ignore all but the 'text' fields
								String fieldType = getFieldType(fieldName, type);
								if (fieldType != null && fieldType.equals("text")) {
									fields.add(fieldName);
									Object fieldValue = aQueryDoc.get(fieldName);
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
		
		return jsonBody;	}

	protected Map<String, Object> filterFields(Document queryDoc) throws ElasticSearchException {
		return filterFields(queryDoc, null, null);
	}
	
	
	protected Map<String, Object> filterFields(Document queryDoc, FieldFilter filter) throws ElasticSearchException, DocumentException {
		return filterFields(queryDoc, null, filter);
	}

	protected <T extends Document> Map<String, Object> filterFields(T queryDoc, String esDocType, FieldFilter filter) throws ElasticSearchException {
		Map<String,Object> objMap = new HashMap<String,Object>();
		if (esDocType == null) esDocType = queryDoc.defaultESDocType();
		
		Map<String,Object> unfilteredMemberAttibutes = null;
		try {
			unfilteredMemberAttibutes = Introspection.publicFields(queryDoc);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| IntrospectionException e) {
			throw new ElasticSearchException(e);
		}
		
		// Filter member attributes
		for (String fieldName: unfilteredMemberAttibutes.keySet()) {
			if (fieldName.equals("additionalFields")) continue; 
			if (filter == null || filter.keepField(fieldName)) {
				if (!isTextField(esDocType, fieldName)) continue;
				objMap.put(fieldName, unfilteredMemberAttibutes.get(fieldName));
			}
		}
		
		// Filter additionalFields 
		for (String fieldName: queryDoc.getAdditionalFields().keySet()) {
			fieldName = "additionalFields."+fieldName;
			if (filter == null || filter.keepField(fieldName)) {
				if (!isTextField(esDocType, fieldName)) continue;
				try {
					objMap.put(fieldName, queryDoc.getField(fieldName));
				} catch (DocumentException e) {
					throw new ElasticSearchException(e);
				}
			}
		}
		
		return objMap;
	}
	
	protected <T extends Document> List<Map<String, Object>> filterFields(List<T> queryDocs, String esDocType, FieldFilter filter) throws ElasticSearchException {
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (Document aDoc: queryDocs) {
			Map<String,Object> aMap = filterFields(aDoc, esDocType, filter);
			maps.add(aMap);
		}
		
		return maps;
	}
	

	protected boolean isTextField(String esDocType, String fieldName) throws ElasticSearchException {
		boolean isText = false;
		String fieldType = getFieldType(fieldName, esDocType);
		if (fieldType != null && fieldType.equals("text")) {
			isText = true;
		}
		return isText;
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


	public void bulk(File jsonFile, Class<? extends Document> docClass) throws ElasticSearchException, IOException {
		String docTypeName = docClass.getName();
		bulk(jsonFile, docTypeName);
	}

	public void bulk(File jsonFile, String docTypeName) throws ElasticSearchException, IOException {
		List<String> jsonLines = Files.readAllLines(jsonFile.toPath());
		String json = String.join("\n", jsonLines);
		bulk(json, docTypeName);
	}
	
	
	public void bulk(String jsonContent, String docTypeName) throws ElasticSearchException, IOException {
		
		jsonContent += "\n\n";
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.bulk");
		URL url = esUrlBuilder().forDocType(docTypeName).forEndPoint("_bulk").build();
		tLogger.trace("url="+url);
		put(url, jsonContent);
		
		// A bulk operation may have changed the properties of different document types in different indices
		clearFieldTypesCache();
	}
	
	public void bulkIndex(String dataFPath, String defDocTypeName) throws ElasticSearchException {
		bulkIndex(dataFPath, defDocTypeName, -1, false);
	}

	public void bulkIndex(String dataFPath, String defDocTypeName, Boolean verbose) throws ElasticSearchException {
		bulkIndex(dataFPath, defDocTypeName, -1, verbose);
	}
	
	public void bulkIndex(String dataFPath, String defDocTypeName, int batchSize, boolean verbose) throws ElasticSearchException {
		deleteIndex();
		ObjectMapper mapper = new ObjectMapper();
		String currDocTypeName = defDocTypeName;
		if (currDocTypeName == null) {
			currDocTypeName = "DefaultType";
		}
		try {
			boolean firstDocumentWasRead = false;
			if (batchSize < 0) batchSize = 100;
			int batchStart = 1;
			
			ObjectStreamReader reader = new ObjectStreamReader(new File(dataFPath));
			Object obj = reader.readObject();
			String jsonBatch = "";
			long docNum = 0;
			while (obj != null) {
				int currBatchSize = 0;
				String jsonLine = null;
				
				if (obj instanceof IndexDef) {
					if (firstDocumentWasRead) {
						throw new ElasticSearchException("IndexDef object did not precede the first Document object in the json file: "+dataFPath);
					} else {
						defineIndex((IndexDef) obj);
					}
				} else if (obj instanceof CurrentDocType) {
					currDocTypeName = ((CurrentDocType)obj).name;
				} else if (obj instanceof Document){
					firstDocumentWasRead = true;
					Document doc = (Document)obj;
					docNum++;
					String id = doc.getId();
					if (verbose) {
						System.out.println("Loading document #"+docNum+": "+id);
					}
					jsonLine = mapper.writeValueAsString(doc);
					jsonBatch += 
						"\n{\"index\": {\"_index\": \""+indexName+"\", \"_type\" : \""+currDocTypeName+"\", \"_id\": \""+id+"\"}}" +
						"\n" + jsonLine;
					
					if (currBatchSize > batchSize) {
						for (StreamlinedClientObserver obs: observers) {
							obs.onBulkIndex(batchStart, batchStart+currBatchSize, indexName, currDocTypeName);
						}
						bulk(jsonBatch, defDocTypeName);
						batchStart += currBatchSize;
						currBatchSize = 0;
						jsonBatch = "";
					} else {
						currBatchSize++;
					}
				} else {
					throw new ElasticSearchException("JSON file "+dataFPath+" contained an object of unsupoorted type: "+obj.getClass().getName());
				}

				if (!jsonBatch.isEmpty()) {
					// Process the very last partial batch
					bulk(jsonBatch, defDocTypeName);
				}
				
				obj = reader.readObject();
			}
			
			
		} catch (FileNotFoundException e) {
			throw new ElasticSearchException("Could not open file "+dataFPath+" for bulk indexing.");
		} catch (IOException e) {
			throw new ElasticSearchException("Could not read from data file "+dataFPath, e);
		} catch (ElasticSearchException e) {
			throw(e);
		} catch (ClassNotFoundException e) {
			throw new ElasticSearchException(e);
		}
		
		return;
	}
	
	

//	private void configureIndexAnalyzer() throws ElasticSearchException {
//		String jsonBody = 
//				"{\n" + 
//				"  \"settings\" : {\n" + 
//				"    \"analysis\": {\n" + 
//				"      \"filter\": {\n" + 
//				"        \"filter_snowball_en\": {\n" + 
//				"          \"type\": \"snowball\",\n" + 
//				"          \"language\": \"English\"\n" + 
//				"        }\n" + 
//				"      },\n" + 
//				"      \"analyzer\": {\n" + 
//				"        \"my_analyzer\": {\n" + 
//				"            \"filter\": [\n" + 
//				"              \"lowercase\",\n" + 
//				"              \"filter_snowball_en\"\n" + 
//				"            ],\n" + 
//				"          \"type\": \"custom\",\n" + 
//				"          \"tokenizer\": \"whitespace\"\n" + 
//				"        }\n" + 
//				"      }\n" + 
//				"    }\n" + 
//				"  }\n" + 
//				"}"
//				;
//
//		URL url = esUrlBuilder().build();
//		put(url, jsonBody);
//	}

	public void bulkIndex(BufferedReader br, String docTypeName, int batchSize, boolean verbose) throws IOException, ElasticSearchException {
		if (batchSize < 0) batchSize = 100;
		int batchStart = 1;
		int currBatchSize = 0;		
		String jsonBatch = "";
		String jsonLine = null;
		while (true) {
			jsonLine = br.readLine();
			if (jsonLine == null) break;
			if (jsonLine.matches("^(class|bodyEndMarker)=.*$")) continue;
			String id = getLineID(jsonLine, verbose);
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
		}		
	}
	
	
	
	private String getLineID(String jsonLine, boolean verbose) throws ElasticSearchException {
		Document_DynTyped doc = null;
		try {
			doc = (Document_DynTyped) new ObjectMapper().readValue(jsonLine, Document_DynTyped.class);
			if (verbose) {
				System.out.println("Indexing doc with ID "+doc.getId());
			}
		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}		
		String id =  doc.getId();
		
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
			    if (aFldName.equals("additionalFields")) {
			    	fieldTypes = collectAdditionalFields(aFldProps, fieldTypes);
			    } else {
				    String aFldType = null;
				    if (aFldProps.has("type")) {
				    	aFldType = aFldProps.get("type").asText();
				    } else {
				    	aFldType = "_EMBEDDED_STRUCTURE";
				    }
				    fieldTypes.put(aFldName, aFldType);
			    }
			}
			
			cacheFieldTypes(fieldTypes, type);
		}
		
		return fieldTypes;
	}
	
	private Map<String, String> collectAdditionalFields(JsonNode dynFieldsMapping, Map<String, String> fieldTypes) {
		ObjectNode props = (ObjectNode) dynFieldsMapping.get("properties");
		if (props != null) {
			Iterator<Entry<String, JsonNode>> iterator = props.fields();
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
			    fieldTypes.put("additionalFields."+aFldName, aFldType);			
			}
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
		updateDocument(docClass.getName(), docID, partialDoc);
//		URL url = esUrlBuilder().forClass(docClass).forDocID(docID)
//				    .forEndPoint("_update").build();
//		String jsonBody = null;
//		Map<String,Object> jsonData = new HashMap<String,Object>();
//		jsonData.put("doc", partialDoc);
//		try {
//			jsonBody = new ObjectMapper().writeValueAsString(jsonData);
//		} catch (JsonProcessingException exc) {
//			throw new ElasticSearchException(exc);
//		}
//		
//		post(url, jsonBody);
	}

	public void updateDocument(String esDocType, String docID, Map<String, Object> partialDoc) throws ElasticSearchException {
		URL url = esUrlBuilder().forDocType(esDocType).forDocID(docID)
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
	
	private ESUrlBuilder esUrlBuilder() throws ElasticSearchException  {
		ESUrlBuilder builder = null;
		try {
			builder = new ESUrlBuilder(indexName, ESConfig.host(), ESConfig.port());
		} catch (ConfigException exc) {
			throw new ElasticSearchException("Invalid ElasticSearch configuration.", exc);
		}
		return builder;
	}

	public <T extends Document> void dumpToFile(File outputFile, String freeformQuery, String docTypeName, T docPrototype, Boolean intoSingleJsonFile) throws ElasticSearchException {
		try {			
			SearchResults<T> results = searchFreeform(freeformQuery, docTypeName, docPrototype);
			dumpToFile(outputFile, results, intoSingleJsonFile);
		} catch (Exception e) {
			throw new ElasticSearchException(e);
		}
	}
	
	public <T extends Document> void dumpToFile(File outputFile, Class<T> docClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ElasticSearchException {
		T docPrototype = docClass.getConstructor().newInstance();
		String esTypeName = docClass.getName();
		SearchResults<T> allDocs = (SearchResults<T>) listAll(esTypeName, docPrototype);
		dumpToFile(outputFile, allDocs, true);
	}
	
	public <T extends Document> void dumpToFile(File outputFile, Class<T> docClass, String esTypeName) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ElasticSearchException {
		Document docPrototype = docClass.getConstructor().newInstance();
		SearchResults<T> allDocs = (SearchResults<T>) listAll(esTypeName, docPrototype);
		dumpToFile(outputFile, allDocs, true);
	}	
	
	public <T extends Document> void dumpToFile(File file, Class<? extends Document> docClass, 
			String esDocType, String query, Set<String> fieldsToIgnore) 
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.dumpToFile");
		Document docPrototype = docClass.getConstructor().newInstance();
		if (esDocType == null) {
			esDocType = docPrototype.getClass().getName();
		}
		tLogger.trace("retrieving docs that fit query="+query);
		SearchResults<T> allDocs = (SearchResults<T>) searchFreeform(query, esDocType, docPrototype);
		tLogger.trace("GOT docs that fit query="+query+". total hits="+allDocs.getTotalHits());

		dumpToFile(file, allDocs, true, fieldsToIgnore);
	}
	
	private void dumpToFile(File outputFile, SearchResults<? extends Document> results, 
			Boolean intoSingleJsonFile) throws ElasticSearchException {
		dumpToFile(outputFile, results, intoSingleJsonFile, null);
	}
	
	private void dumpToFile(File outputFile, SearchResults<? extends Document> results, 
			Boolean intoSingleJsonFile, Set<String> fieldsToIgnore) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.dumpToFile");
		
		if (fieldsToIgnore == null) {fieldsToIgnore = new HashSet<String>();}
		
		tLogger.trace("invoked with outputFile="+outputFile.getAbsolutePath()+", results.getTotalHits()="+results.getTotalHits());
		System.out.println("== dumpToFile: invoked with outputFile="+outputFile.getAbsolutePath()+", results.getTotalHits()="+results.getTotalHits());
		if (intoSingleJsonFile == null) intoSingleJsonFile = true;
		
		try {
			FileWriter fWriter = null;
			if (intoSingleJsonFile) {
				fWriter = new FileWriter(outputFile);
				fWriter.write("bodyEndMarker=NEW_LINE\n");
			} else {
				// Clear the output directory
				FileUtils.deleteDirectory(outputFile);
				outputFile.mkdir();
			}
			ObjectMapper mapper = new ObjectMapper();
			Map<String,Object> docMap = new HashMap<String,Object>();
			Iterator<?> iter = results.iterator();
			while (iter.hasNext()) {
				Hit<Document> aScoredDoc = (Hit<Document>)iter.next();
				tLogger.trace("** dumping document with id="+aScoredDoc.getDocument().getId());
				docMap = mapper.convertValue(aScoredDoc.getDocument(), docMap.getClass()) ;
				Map<String,Object> additionalFields = (Map<String, Object>) docMap.get("additionalFields");
				for (String fld: fieldsToIgnore) {
					additionalFields.remove(fld);
				}
				if (intoSingleJsonFile) {
					String json = mapper.writeValueAsString(docMap);
					fWriter.write(json+"\n");
				} else {
					writeToTextFile(aScoredDoc.getDocument(), outputFile.getAbsolutePath());
				}
			}
			if (fWriter != null) fWriter.close();
		} catch (Exception e) {
			throw new ElasticSearchException(e);
		}	
	}


	private void writeToTextFile(Document doc, String outputDir) throws IOException {
		String docID = doc.getId();
		String docFilePath = outputDir+"/"+docID+".txt";
		String docContent = doc.toString();
		FileWriter writer = new FileWriter(new File(docFilePath));
		writer.write("bodyEndMarker=NEW_LINE\n");
		writer.write(docContent);
		writer.close();
	}

	public void createIndex(String emptytestindex) throws ElasticSearchException {
		URL url = esUrlBuilder().build();
		
		put(url, null);
	}
	
	public void attachObserver(StreamlinedClientObserver _obs) {
		observers.add(_obs);
	}

	public String clearDocType(String docType) throws ElasticSearchException {
		String body = "{\"query\": {\"match_all\": {}}}";
		URL url = esUrlBuilder().forDocType(docType).forEndPoint("_delete_by_query").build();
		String jsonResponse = okResponseJson();
		try {
			jsonResponse = post(url, body);
		} catch (Exception e) {
			// If an exception is raised, it means that the docType does not exist
			// In which case, we can consider it to be already cleared.
		}
	
		return jsonResponse;	
	}

	private String okResponseJson() {
		String json = "{\"err\": null, \"status\": \"ok\"}";
		return json;
	}

	public void changeIndexSetting(String settingName, Object settingValue) throws ElasticSearchException {
		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put(settingName, settingValue);
		changeIndexSettings(settings);
	}

	public void changeIndexSettings(Map<String, Object> settings) throws ElasticSearchException {
		String json = null;
		try {
			json = new ObjectMapper().writeValueAsString(settings);
			URL url = esUrlBuilder().forEndPoint("_settings").build();
			put(url, json);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
	}
}
