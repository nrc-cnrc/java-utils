package ca.nrc.dtrc.elasticsearch;

import ca.nrc.config.ConfigException;
import ca.nrc.data.file.ObjectStreamReader;
import ca.nrc.data.file.ObjectStreamReaderException;
import ca.nrc.datastructure.Pair;
import ca.nrc.dtrc.elasticsearch.request.Highlight;
import ca.nrc.dtrc.elasticsearch.request.JsonString;
import ca.nrc.dtrc.elasticsearch.request.Query;
import ca.nrc.dtrc.elasticsearch.request.RequestBodyElement;
import ca.nrc.introspection.Introspection;
import ca.nrc.introspection.IntrospectionException;
import ca.nrc.ui.commandline.UserIO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import okhttp3.Request.Builder;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamlinedClient {
		
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	public static enum ESOptions {CREATE_IF_NOT_EXISTS, UPDATES_WAIT_FOR_REFRESH};

	public boolean updatesWaitForRefresh = false;
	
	// Whenever the client issues a transaction that modifies the DB,
	// it will sleep by that much to give ES time to update all the 
	// nodes and shards.
	private double sleepSecs = 0.0;

	protected String indexName;
		public String getIndexName() {return indexName;}
		
	private String serverName = "localhost";
	private int port = 9200;

	Boolean _indexExists = null;
	
	private UserIO userIO = null;
		public void setUserIO(UserIO _userIO) {this.userIO = _userIO;}
		public UserIO getUserIO() {return this.userIO;}
		public void echo(String message, UserIO.Verbosity level) {
			if (this.userIO != null) userIO.echo(message, level);
		}
	
	// Stores the types of the various fields for a given document type
//	private static Map<String,Map<String,String>> fieldTypesCache = null;

	private static Builder requestBuilder = new Request.Builder();
	
	// As recommended in the OkHttp documentation, we use a single
	// OkHttpClient instance for all our needs.
	private static OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(60, TimeUnit.SECONDS)
			.readTimeout(60,  TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.build();

	private List<StreamlinedClientObserver> observers = new ArrayList<StreamlinedClientObserver>();

	public StreamlinedClient() throws ElasticSearchException {
		initialize(null, null, null);
	}
	
	public StreamlinedClient(String _indexName) throws ElasticSearchException {
		initialize(_indexName, null, null);
	}

	public StreamlinedClient(String _indexName, ESOptions... options) throws ElasticSearchException {
		this.initialize(_indexName, (Double)null, options);
	}


	public StreamlinedClient(String _indexName, double _sleepSecs)
		throws ElasticSearchException {
		initialize(_indexName, new Double(_sleepSecs));
	}

	public void initialize(
		String _indexName, Double _sleepSecs, ESOptions... options)
		throws ElasticSearchException {
		if (_sleepSecs == null) {
			_sleepSecs = new Double(0.0);
		}
		if (options == null) {
			options = new ESOptions[0];
		}
		boolean createIfNotExist = false;
		for (ESOptions anOption: options) {
			if (anOption == ESOptions.CREATE_IF_NOT_EXISTS) {
				createIfNotExist = true;
			} else if (anOption == ESOptions.UPDATES_WAIT_FOR_REFRESH) {
				updatesWaitForRefresh = true;
			}
		}
		this.indexName = canonicalIndexName(_indexName);
		this.sleepSecs = _sleepSecs;

		if (createIfNotExist) {
			if (!indexExists()) {
				createIndex(indexName);
			}
		}
	}
	
	public StreamlinedClient setSleepSecs(double _sleepSecs) {
		this.sleepSecs = _sleepSecs;
		return this;
	}

	public StreamlinedClient setIndexName(String _indexName) {
		this.indexName = canonicalIndexName(_indexName);
		this._indexExists = null;
		
		return this;
	}
	
	Index index = null;
		@JsonIgnore
		public Index getIndex() throws ElasticSearchException {
			if (index == null) {
				index = new Index(indexName);;
			}
			return index;
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

	public void defineIndex() throws ElasticSearchException {
		defineIndex(null, null);
	}

	public void defineIndex(IndexDef iDef, Boolean force) throws ElasticSearchException {
		if (iDef == null) {
			iDef = new IndexDef();
		}

		if (force == null) {
			force = true;
		}

		Map<String,Object> indexMappings = iDef.indexMappings();
		Map<String,Object> indexSettings = iDef.settingsAsProps();
		defineIndex(indexSettings, indexMappings, force);
	}
	
	
	public void defineIndex(Map<String, Object> indexSettings, Map<String, Object> indexMappings, Boolean force) throws ElasticSearchException {
		getIndex().setDefinition(indexSettings, indexMappings, force);
		return;
	}

	private void defineIndexIfNotExists() throws ElasticSearchException {
		if (!indexExists()) {
			defineIndex();
		}
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

	public boolean indexExists() {
		if (_indexExists == null) {
			URL url = null;
			_indexExists = true;
			try {
				url = esUrlBuilder().forEndPoint("_search").build();
			} catch (ElasticSearchException e) {
				throw new RuntimeException(e);
			}
			try {
				get(url);
			} catch (ElasticSearchException e) {
				_indexExists = false;
			}
		}

		return _indexExists;
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

		// The 'id' field should always be of type 'keyword'
		{
			Map<String, String> aMapping = new HashMap<String, String>();
			aMapping.put("type", "keyword");
			mappingsDict.put("id", aMapping);
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
		defineIndexIfNotExists();
		if (tLogger.isTraceEnabled()) {
			try {
				tLogger.trace("(Document): putting document with id="+doc.getId()+", doc="+
					new ObjectMapper().writeValueAsString(doc));
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}
		}
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
		if (tLogger.isTraceEnabled()) {
			try {
				tLogger.trace("(String, Document): putting document of type="+
					type+", id="+dynDoc.getId()+", dynDoc="+
					new ObjectMapper().writeValueAsString(dynDoc));
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}
		}
		
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
		URL url =
			esUrlBuilder()
			.forDocType(type)
			.forDocID(docID)
			.refresh(updatesWaitForRefresh)
			.build();
		tLogger.trace("(String, String, String) putting url="+url+", type="+type+", docID="+docID+", updatesWaitForRefresh="+updatesWaitForRefresh+", jsonDoc="+jsonDoc);
		
		String jsonResponse = put(url, jsonDoc);
		
		getIndex().clearFieldTypesCache(type);		
		
		sleep();
		return jsonResponse;
	}	
	
	public void deleteDocumentWithID(String docID, Class<?extends Document> docClass) throws ElasticSearchException {
		deleteDocumentWithID(docID, docClass.getName());
	}
	
	public void deleteDocumentWithID(String docID, String esDocType) throws ElasticSearchException {
		URL url =
			esUrlBuilder()
			.forDocType(esDocType)
			.forDocID(docID)
			.refresh(updatesWaitForRefresh)
			.build();
		delete(url);
		sleep();
	}

	
	public <T extends Document> List<T> listFirstNDocuments(T docPrototype, Integer maxN) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.listFirstNDocuments");
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

	public <T extends Document> SearchResults<T> listAll(
			String esDocTypeName , T docPrototype) throws ElasticSearchException {
		return listAll(esDocTypeName, docPrototype, new RequestBodyElement[0]);
	}

	public <T extends Document> SearchResults<T> listAll(T docPrototype) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.listAll");
		@SuppressWarnings("unchecked")
		Class<T> docClass = (Class<T>) docPrototype.getClass();
		String type = docClass.getName();
		tLogger.trace("searching for all type="+type);

//		String query = "";
		Query query = new Query();
		query
			.openAttr("exists")
				.openAttr("field")
					.setOpenedAttr("id")
			;

		SearchResults<T> results = search(query, type, docPrototype);
		return results;
	}

	public <T extends Document> SearchResults<T> listAll(
		String esDocTypeName , T docPrototype, RequestBodyElement... options)
		throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.listAll");
		tLogger.trace("searching for all type="+esDocTypeName);

		Query query = new Query();
		query
			.openAttr("exists")
				.openAttr("field")
				.setOpenedAttr("id")
			;

		SearchResults<T> results =
			search(query, esDocTypeName, docPrototype, options);

		return results;
	}

	public <T extends Document> Iterator<T> typeIterator(String esDocTypeName , T docPrototype) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.listAll");
		tLogger.trace("searching for all type="+esDocTypeName);

		URL url = esUrlBuilder().forDocType(esDocTypeName).forEndPoint("_search").scroll().build();
		
		tLogger.trace("invoking url="+url);
		String jsonResponse = post(url, "{}");
		
		SearchResults<T> results = new SearchResults<T>(jsonResponse, docPrototype, this);
		Iterator<T> iterator = new ESDocumentIterator<>(results);
		
		return iterator;
	}		

	public String post(URL url) throws IOException, ElasticSearchException, InterruptedException {
		return post(url, null);
	}
	
	public String post(URL url, String json) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.post");
		tLogger.trace("posting url="+url+", with json="+json);
		
		if (json == null) json = "";
	    RequestBody body = RequestBody.create(JSON, json);
	    
	    Request request = requestBuilder
	        .url(url)
	        .post(body)
	        .build();
	    
	    Response response;
		try {
			response = httpClient.newCall(request).execute();
		} catch (IOException e) {
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
	
	protected String get(URL url) throws ElasticSearchException {
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
			response = httpClient.newCall(request).execute();
		    jsonResponse = response.body().string();			
		} catch (IOException exc) {
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
			exception = makeElasticSearchException(responseObj);
		}
		
		if (exception != null) throw exception;
		
	}

	public <T extends Document> SearchResults<T> search(
			String freeformQuery, String docTypeName,
			T docPrototype) throws ElasticSearchException {
		return search(freeformQuery, docTypeName, docPrototype,
				new RequestBodyElement[0]);
	}

	public <T extends Document> SearchResults<T> search(
			String freeformQuery, T docPrototype,
			RequestBodyElement... xtraReqSpecs) throws ElasticSearchException {
		return search(freeformQuery, null,
				docPrototype, xtraReqSpecs);
	}


	public <T extends Document> SearchResults<T> search(
			String freeformQuery, String docTypeName, T docPrototype,
			RequestBodyElement... additionalSearchSpecs)
			throws ElasticSearchException {

		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.searchFreeform");

		Query queryBody = new Query();
		queryBody
			.openAttr("query_string")
				.openAttr("query")
				.setOpenedAttr(freeformQuery)
			;

		SearchResults<T> hits = search(queryBody, docTypeName, docPrototype,
			additionalSearchSpecs);

		tLogger.trace("Returning results with #hits="+hits.getTotalHits());

		return hits;
	}

	public <T extends Document> SearchResults<T> search(Query queryBody, T docPrototype) throws ElasticSearchException {
		return search(queryBody, null, docPrototype);
	}

	public <T extends Document> SearchResults<T> search(
			Query query, String docTypeName, T docPrototype) throws ElasticSearchException {
		return search(query, docTypeName, docPrototype, new RequestBodyElement[0]);
	}

	public <T extends Document> SearchResults<T> search(
			Query query, T docPrototype, RequestBodyElement... additionalSearchSpecs)
			throws ElasticSearchException {
		return search(query, null, docPrototype, additionalSearchSpecs);
	}

	public <T extends Document> SearchResults<T> search(
			Query query, String docTypeName, T docPrototype,
			RequestBodyElement... additionalBodyElts) throws ElasticSearchException {

		RequestBodyElement[] bodyElements =
			new RequestBodyElement[additionalBodyElts.length+1];
		bodyElements[0] = query;
		for (int ii=1; ii < bodyElements.length; ii++) {
			bodyElements[ii] = additionalBodyElts[ii-1];
		}
		Map<String,Object> reqMap = RequestBodyElement.merge(bodyElements);
		if (!reqMap.containsKey("highlight")) {
			Highlight highlight = new Highlight().hihglightField("longDescription");
			RequestBodyElement.mergeIntoMap(reqMap, highlight);
		}

		String reqJson = null;
		try {
			reqJson = new ObjectMapper().writeValueAsString(reqMap);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		return search(new JsonString(reqJson), docTypeName, docPrototype);
	}


	private <T extends Document> SearchResults<T> search(JsonString jsonQuery,
	 	String docTypeName, T docPrototype) throws ElasticSearchException {
		if (docTypeName == null) {
			docTypeName = docPrototype.getClass().getName();
		}

		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.search");
		URL url = esUrlBuilder()
				.forDocType(docTypeName).forEndPoint("_search")
				.scroll().build();
		tLogger.trace("url="+url+", jsonQuery="+jsonQuery);
		String jsonResponse = post(url, jsonQuery.toString());

		SearchResults<T> results = new SearchResults<T>(jsonResponse, docPrototype, this);

		tLogger.trace("returning results with #hits="+results.getTotalHits());

		return results;
	}

	protected String escapeQuotes(String query) {
		String escQuery = query;
		if (query != null) {
			Matcher matcher = Pattern.compile("\"").matcher(query);
			escQuery = matcher.replaceAll("\\\\\"");
		}
			
		return escQuery;
	}

	public <T extends Document> SearchResults<T> search(String query, T docPrototype) throws ElasticSearchException {
		String docTypeName = docPrototype.getClass().getName();
		SearchResults<T> hits = search(query, docTypeName, docPrototype);
		
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
		getIndex().deleteIndex();
	}

	public <T extends Document> SearchResults<T> moreLikeThis(T queryDoc) throws ElasticSearchException, IOException, InterruptedException {
		SearchResults<T> results = moreLikeThis(queryDoc, null);
		return results;
	}
	
	public <T extends Document> SearchResults<T> moreLikeThis(T queryDoc, FieldFilter fldFilter) throws ElasticSearchException, IOException, InterruptedException {
		return moreLikeThis(queryDoc, fldFilter, null);
	}

	public <T extends Document> SearchResults<T> moreLikeThis(T queryDoc, FieldFilter fldFilter, String esDocTypeName) throws ElasticSearchException, IOException, InterruptedException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.StreamlinedClient.moreLikeThis");
		
		Map<String,Object> queryDocMap = null;
		queryDocMap = filterFields(queryDoc, esDocTypeName, fldFilter);
		
		String esType = esDocTypeName;
		if (esType == null) esType = queryDoc.getClass().getName();
		String mltBody = moreLikeThisJsonBody(esType, queryDocMap);
		
		SearchResults<T> results = search(new JsonString(mltBody), esType, queryDoc);
		
		tLogger.trace("Returned results.iterator().hasNext()="+results.iterator().hasNext());
	
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
		results = search(new JsonString(mltBody), esDocTypeName, queryDocs.get(0));
	
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

	protected <T extends Document> Map<String, Object> filterFields(
			T queryDoc, String esDocType, FieldFilter filter) 
			throws ElasticSearchException {
		Map<String,Object> objMap = new HashMap<String,Object>();
		if (esDocType == null) esDocType = queryDoc.defaultESDocType();
		
		Map<String,Object> unfilteredMemberAttibutes = null;
		try {
			unfilteredMemberAttibutes = Introspection.publicFields(queryDoc);
		} catch (IntrospectionException e) {
			throw new ElasticSearchException(e);
		}
		
		// Filter member attributes
		for (String fieldName: unfilteredMemberAttibutes.keySet()) {
			if (fieldName.equals("additionalFields")) continue; 
			if (filter == null || filter.keepField(fieldName)) {
				if (!isTextField(esDocType, fieldName)) continue;
				if (!fieldName.equals("longDescription")) {
					objMap.put(fieldName, unfilteredMemberAttibutes.get(fieldName));					
				} else {
					// Note: longDescription is an alias for content. So if it
					//   it is to be retained, then retain content instead.
					objMap.put("content", unfilteredMemberAttibutes.get("content"));
				}
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
		getIndex().clearFieldTypesCache();
	}
	
	public Document bulkIndex(String dataFPath, String defDocTypeName) throws ElasticSearchException {
		return bulkIndex(dataFPath, defDocTypeName, -1, null, null);
	}

	public Document bulkIndex(String dataFPath, String defDocTypeName, Boolean verbose) throws ElasticSearchException {
		return bulkIndex(dataFPath, defDocTypeName, -1, verbose, null);
	}
	
	public Document bulkIndex(String dataFPath, String defDocTypeName, Boolean verbose, Boolean force) throws ElasticSearchException {
		return bulkIndex(dataFPath, defDocTypeName, -1, verbose, force);
	}

	public Document bulkIndex(String dataFPath, String defDocTypeName, int batchSize, Boolean verbose, Boolean force) throws ElasticSearchException {
		if (verbose == null) {
			verbose = false;
		}
		if (force == null) {
			force = false;
		}
		int docCounter = 0;
		Document docPrototype = null;
		ObjectMapper mapper = new ObjectMapper();
		String currDocTypeName = defDocTypeName;
		if (currDocTypeName == null) {
			currDocTypeName = "DefaultType";
		}
		ObjectStreamReader reader = null;
		try {
			boolean firstDocumentWasRead = false;
			if (batchSize < 0) batchSize = 100;
			int batchStart = 1;
			
			reader = new ObjectStreamReader(new File(dataFPath));
			reader.onError = ObjectStreamReader.OnError.LOG_ERROR;
			Object obj = reader.readObject();
			String jsonBatch = "";
			long docNum = 0;
			int currBatchSize = 0;
			while (obj != null) {
				String jsonLine = null;
				
				if (obj instanceof IndexDef) {
					if (firstDocumentWasRead) {
						String errMess = 
								"\nIndexDef object did not precede the first Document object in the json file: "+dataFPath+"\n"+
								"Error was found at line "+reader.lineCount+" of json data file.\n";
						System.err.println(errMess);
						throw new ElasticSearchException(errMess);
					} else {
						defineIndex((IndexDef) obj, force);
					}
				} else if (obj instanceof CurrentDocType) {
					currDocTypeName = ((CurrentDocType)obj).name;
				} else if (obj instanceof Document){
					firstDocumentWasRead = true;
					Document doc = (Document)obj;
					docCounter++;
					
					echo("Indexing document #"+docCounter+": "+doc.getId(), UserIO.Verbosity.Level1);
					
					// Keep the first document read as a prototype.
					if (docPrototype == null) docPrototype = doc;
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
				obj = reader.readObject();
			}
			
			if (!jsonBatch.isEmpty()) {
				// Process the very last partial batch
				bulk(jsonBatch, defDocTypeName);
			}
		} catch (FileNotFoundException e) {
			throw new ElasticSearchException("Could not open file "+dataFPath+" for bulk indexing.");
		} catch (IOException e) {
			throw new ElasticSearchException("Could not read from data file "+dataFPath, e);
		} catch (ElasticSearchException e) {
			throw(e);
		} catch (ClassNotFoundException e) {
			throw new ElasticSearchException(e);
		} catch (ObjectStreamReaderException e) {
			throw new ElasticSearchException(e);
		} finally {
			try {
				if (reader != null) { reader.close(); }
			} catch (IOException e) {
				throw new ElasticSearchException("Problem closing the JSON object reader for file: "+dataFPath, e);
			}
		}
		
		
		// Sleep a bit to give time for ES to incorporate the documents.
		try {
			Thread.sleep(2*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return docPrototype;
	}

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
		return getIndex().getFieldTypes(docClass.getName());
	}

	public String getFieldType(String fieldName, String docType) throws ElasticSearchException {
		String fieldType = null;
		
		Map<String,String> allFieldTypes = getIndex().getFieldTypes(docType);
		if (allFieldTypes.containsKey(fieldName)) {
			fieldType = allFieldTypes.get(fieldName);
		}
		
		return fieldType;
	}

	public void updateDocument(Class<? extends Document> docClass, String docID, Map<String, Object> partialDoc) throws ElasticSearchException {
		updateDocument(docClass.getName(), docID, partialDoc);
	}

	public void updateDocument(String esDocType, String docID, Map<String, Object> partialDoc) throws ElasticSearchException {
		URL url =
			esUrlBuilder()
			.forDocType(esDocType)
			.forDocID(docID)
			.forEndPoint("_update")
			.refresh(this.updatesWaitForRefresh)
			.build();
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
	
	protected ESUrlBuilder esUrlBuilder() throws ElasticSearchException  {
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
			SearchResults<T> results = search(freeformQuery, docTypeName, docPrototype);
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
		SearchResults<T> allDocs = (SearchResults<T>) search(query, esDocType, docPrototype);
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
		URL url =
			esUrlBuilder()
			.forDocType(docType)
			.forEndPoint("_delete_by_query")
			.refresh(true)
			.build();
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
			URL url =
				esUrlBuilder()
				.forEndPoint("_settings")
				.build();
			put(url, json);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
	}
	
	private static ElasticSearchException makeElasticSearchException(Map<String, Object> responseObj) {
		ElasticSearchException exc = new ElasticSearchException(responseObj);
		
		if (responseObj.containsKey("error")) {
			Map<String,Object> error = (Map<String, Object>) responseObj.get("error");
			if (error.containsKey("root_cause")) {
				List<Object> rootCauseLst = (List<Object>) error.get("root_cause");
				if (rootCauseLst.size() > 0) {
					Map<String,Object> rootCause = (Map<String, Object>) rootCauseLst.get(0);
					if (rootCause.containsKey("type")) {
						String errorType = (String) rootCause.get("type");
						if (errorType.equals("index_not_found_exception")) {
							String indexName = "<unknown>";
							if (rootCause.containsKey("index")) {
								indexName = (String) rootCause.get("index");
							}
							exc =  new NoSuchIndexException("No such index: "+indexName);
						} 
					}
				}
			}
		}
		
		
		return exc;
	}
	
}
