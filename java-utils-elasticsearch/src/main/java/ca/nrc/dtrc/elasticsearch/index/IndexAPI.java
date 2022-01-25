package ca.nrc.dtrc.elasticsearch.index;

import ca.nrc.data.file.ObjectStreamReader;
import ca.nrc.data.file.ObjectStreamReaderException;
import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.dtrc.elasticsearch.request.RequestBodyElement;
import static ca.nrc.dtrc.elasticsearch.ESFactory.*;
import ca.nrc.json.PrettyPrinter;
import ca.nrc.ui.commandline.UserIO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;


/**
 * API for creating, defining, deleting ElasticSearch indices.
 */
public abstract class IndexAPI extends ES_API {

	protected abstract URL url4singletypeMappings(String docTypeName) throws ElasticSearchException;
	protected abstract URL url4indexDef() throws ElasticSearchException;
	public abstract URL url4deleteByQuery(String docType) throws ElasticSearchException;
	public abstract URL url4bulk(String docTypeName) throws ElasticSearchException;
	protected abstract JSONObject extractFieldsProps(JSONObject jsonObj, String docTypeName);
	protected abstract void putIndexDefintion(IndexDef iDef) throws ElasticSearchException;
	protected abstract String bulkLinePrefix(String currDocTypeName, String id);

	private static  Map<String,Boolean> indexExistsCache =
		new HashMap<String,Boolean>();

	// Stores the types of the various fields for a given document type
	private static Map<String,Map<String,String>> fieldTypesCache = null;

	public IndexAPI(ESFactory _fullAPI) throws ElasticSearchException {
		super(_fullAPI);
	}

	public static String canonicalIndexName(String origIndexName) {
		String canonical = origIndexName;
		if (canonical != null) {
			canonical = canonical.toLowerCase();
		}

		return canonical;
	}


	private void echo(String message, UserIO.Verbosity level) {
		esFactory.echo(message, level);
	}

	public void create() throws ElasticSearchException {
		define(true);
	}

	public void define(Boolean force) throws ElasticSearchException {
		define(new IndexDef(indexName()), force);
	}

	public void define(
		Map<String, Object> settings, Map<String, Object> mappings,
		Boolean force) throws ElasticSearchException {
		if (force == null) {
			force = false;
		}

		IndexDef iDef = new IndexDef(indexName());
		iDef.loadSettings(settings);
		iDef.loadMappings(mappings);

		define(iDef, force);
	}

	public void define(IndexDef iDef, Boolean force) throws ElasticSearchException {
		if (force == null) {
			force = false;
		}

		if (exists()) {
			if (!force) {
				throw new IndexException("Tried to change settings of existing index "+indexName()+" eventhough force=false");
			}
			delete();
		}

		putIndexDefintion(iDef);

		cacheIndexExists(indexName(), true);

		return;

	}

	public void changeIndexSetting(String settingName, Object settingValue) throws ElasticSearchException {
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put(settingName, settingValue);
		changeIndexSettings(settings);
	}

	public void changeIndexSettings(Map<String, Object> settings) throws ElasticSearchException {
		String json = null;
		try {
			json = new ObjectMapper().writeValueAsString(settings);
			URL url =
			urlBuilder()
			.forEndPoint("_settings")
			.build();
			transport().put(url, json);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
	}


	public boolean exists() throws ElasticSearchException {
		Boolean exists = uncacheIndexExists(indexName());
		if (exists == null) {
			URL url = null;
			try {
				url = urlBuilder().build();
			} catch (ElasticSearchException e) {
				throw new RuntimeException(e);
			}

			int status = transport().head(url);
			exists = (200 == status);
			cacheIndexExists(indexName(), exists);
		}

		return exists;
	}

	public  synchronized void cacheIndexExists(Boolean exists) {
		indexExistsCache.put(indexName(), exists);
	}

	public static synchronized void cacheIndexExists(String indexName, Boolean exists) {
		indexExistsCache.put(indexName, exists);
	}

	private static synchronized Boolean uncacheIndexExists(String indexName) {
		Boolean exists = indexExistsCache.get(indexName);
		return exists;
	}

	public void delete() throws ElasticSearchException {
		URL url =
			urlBuilder()
			.build();

		try {
			transport().delete(url);
			clearFieldTypesCache();
		} catch (NoSuchIndexException e) {
			// OK... we tried to delete an index that did not exist
			// All other exception types must be passed along
		}
		cacheIndexExists(indexName(), (Boolean)null);

	}

	public IndexDef definition() throws ElasticSearchException {
		Map<String,Object> settingProps = null;
		// Get the Index settings
		{

			URL url = urlBuilder().forEndPoint("_settings").build();
			String json = transport().get(url);

			Map<String,Object> esSettings = new HashMap<String,Object>();
			try {
				esSettings = (Map<String, Object>) new ObjectMapper().readValue(json, esSettings.getClass());
				esSettings = (Map<String, Object>) esSettings.get(this.indexName());
				esSettings = (Map<String, Object>) esSettings.get("settings");
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}
			settingProps = IndexDef.tree2props(esSettings);
		}

		// Get the index mappings
		Map<String,Object> mappings = new HashMap<String,Object>();
		{
			String json = null;
			try {
				json = new ObjectMapper().writeValueAsString(mappings);
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}

			URL url = urlBuilder().forEndPoint("_mappings").build();
			json = transport().get(url);
			try {
				mappings = (Map<String, Object>) new ObjectMapper().readValue(json, mappings.getClass());
			} catch (JsonProcessingException e) {
				throw new ElasticSearchException(e);
			}
		}

		IndexDef iDef =
				new IndexDef(indexName())
				.loadSettings(settingProps)
				.loadMappings(mappings)
				;

		return iDef;

	}

	public Map<String, String> fieldTypes(Class<? extends Document> docClass) throws ElasticSearchException {
		String docType = Document.determineType(docClass);
		return fieldTypes(docType);
	}

	public String fieldType(String fldName, String docTypeName) throws ElasticSearchException {
		Map<String,String> types = fieldTypes(docTypeName);
		String type = null;
		if (types.containsKey(fldName)) {
			type = types.get(fldName);
		}
		return type;
	}

	public Map<String, String> fieldTypes(String docTypeName) throws ElasticSearchException {
		Map<String,String> fieldTypes = uncacheFieldTypes(docTypeName);
		if (fieldTypes == null) {
			fieldTypes = new HashMap<String,String>();

			URL url = url4singletypeMappings(docTypeName);

			String jsonResponse = transport().get(url);
			JSONObject jsonObj = new JSONObject(jsonResponse);

			JSONObject fieldsProps = extractFieldsProps(jsonObj, docTypeName);

			for (String aFldName: fieldsProps.keySet()) {
				JSONObject aFldProps = fieldsProps.getJSONObject(aFldName);
			    if (aFldName.equals("additionalFields")) {
			    	fieldTypes = collectAdditionalFields(aFldProps, fieldTypes);
			    } else {
				    String aFldType = null;
				    if (aFldProps.has("type")) {
				    	aFldType = aFldProps.getString("type");
				    } else {
				    	aFldType = "_EMBEDDED_STRUCTURE";
				    }
				    fieldTypes.put(aFldName, aFldType);
			    }
			}
			cacheFieldTypes(fieldTypes, docTypeName);
		}

//		 Type for 'id' may not have been set in ESFactory, if all
//		 the documents that were put into the type had a null id

		fieldTypes.put("id", "text");

		return fieldTypes;

	}

	private Map<String, String> collectAdditionalFields(JSONObject dynFieldsMapping, Map<String, String> fieldTypes) {
		if (dynFieldsMapping.has("properties")) {
			JSONObject props = dynFieldsMapping.getJSONObject("properties");
			for (String aFldName: props.keySet()) {
				JSONObject aFldProps = props.getJSONObject(aFldName);
				String aFldType = null;
				if (aFldProps.has("type")) {
					aFldType = aFldProps.getString("type");
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

	public static void clearFieldTypesCache(Class<? extends Document> docClass) {
		clearFieldTypesCache(docClass.getName());
	}

	public static void clearFieldTypesCache(String docTypeName) {
		if (fieldTypesCache != null) {
			fieldTypesCache.put(docTypeName, null);
		}
	}

	public static void clearFieldTypesCache() {
		fieldTypesCache = null;
	}


//	public boolean isEmpty__OLD() throws ElasticSearchException {
//		Boolean empty = null;
//
//		URL url = null;
//		try {
//			url = urlBuilder().forEndPoint("_mappings").build();
//		} catch (ElasticSearchException e) {
//			throw new RuntimeException(e);
//		}
//
//		String jsonResp = null;
//		try {
//			jsonResp = transport().get(url);
//		} catch (NoSuchIndexException e1) {
//			empty = true;
//		}
//
//		if (empty == null) {
//			// Index exists. Does it include some types?
//			JSONObject mappings = new JSONObject(jsonResp)
//				.getJSONObject(indexName())
//				.getJSONObject("mappings");
//
//			empty = (0 == mappings.keySet().size());
//		}
//
//		return empty;
//	}

	public boolean isEmpty() throws ElasticSearchException {
		Boolean empty = null;

		URL url = null;
		try {
			url = urlBuilder().forEndPoint("_search").build();
		} catch (ElasticSearchException e) {
			throw new RuntimeException(e);
		}

		String jsonResp = null;
		try {
			jsonResp = transport().get(url);
		} catch (NoSuchIndexException e1) {
			empty = true;
		}

		if (empty == null) {
			// Index exists. Does it include some documents?
			Long totalHits = null;
			JSONObject hits = new JSONObject(jsonResp)
				.getJSONObject("hits");

			if (hits.get("total") instanceof JSONObject) {
				totalHits = hits.getJSONObject("total")
				.getBigInteger("value").longValue();
			} else {
				totalHits = hits.getBigInteger("total").longValue();
			}

			empty = totalHits.equals(new Long(0));
		}

		return empty;
	}

	public void clear() throws ElasticSearchException {
		clear((String)null, (Boolean)null);
	}

	public void clear(boolean failIfIndexNotFound) throws ElasticSearchException {
		clear((String)null, failIfIndexNotFound);
	}

	public void clear(String docTypeName) throws ElasticSearchException {
		clear(docTypeName, (Boolean)null);
	}

	public void clear(String docTypeName, Boolean failIfIndexNotFound) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.IndexAPI.clear");
		if (failIfIndexNotFound == null) {
			failIfIndexNotFound = true;
		}
		tLogger.trace("invoked");

		URL url = url4deleteByQuery(docTypeName);
		JSONObject json = new JSONObject()
			.put("query", new JSONObject()
				.put("match_all", new JSONObject())
			);
		if (docTypeName != null) {
			json = new JSONObject()
				.put("query", new JSONObject()
					.put("bool", new JSONObject()
						.put("must", new JSONObject()
							.put("match", new JSONObject()
								.put("type", docTypeName)
							)
						)
					)
				);
		}
		String jsonInput = json.toString();
		String jsonResp = "{}";

		tLogger.trace("url=" + url + ", jsonInput=" + jsonInput);
		try {
			jsonResp = transport().post(url, jsonInput);
		} catch (Exception exc) {
			if (failIfIndexNotFound) throw exc;
		}

		sleep();
	}

	public void defineIndexIfNotExists() throws ElasticSearchException {
		if (!exists()) {
			define(true);
		}
	}

	public <T extends Document> SearchResults<T> listAll(T docProto)
		throws ElasticSearchException {
		return listAll(null, docProto, new RequestBodyElement[0]);
	}

	public <T extends Document> SearchResults<T> listAll(
		String esDocTypeName, T docProto, RequestBodyElement... options)
		throws ElasticSearchException {

		esDocTypeName = Document.determineType(esDocTypeName, docProto);

		SearchResults<T> results =
			esFactory.searchAPI().search("+type:"+esDocTypeName, esDocTypeName, docProto);
		return results;
	}

	public <T extends Document> List<T> listFirstNDocuments(T docPrototype, Integer maxN) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.index.IndexAPI.listFirstNDocuments");
		SearchResults results = listAll(docPrototype);

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


	public String refresh(String type) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.index.IndexAPI.refreshIndex");
		URL url = urlBuilder().forDocType(type).forEndPoint("_refresh").build();
		String jsonResponse = transport().post(url);
		tLogger.trace("url=" + url + ", jsonResponse=" + jsonResponse);

		return jsonResponse;
	}

	public void bulk(String jsonContent, String docTypeName) throws ElasticSearchException {
		jsonContent += "\n\n";
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es5.StreamlinedClient.bulk");
		URL url = url4bulk(docTypeName);
		tLogger.trace("url=" + url);
		transport().put(url, jsonContent);

		// A bulk operation may have changed the properties of different document types in different indices
		clearFieldTypesCache();
	}

	public void bulk(File jsonFile, Class<? extends Document> docClass)
		throws ElasticSearchException {
		String docTypeName = Document.determineType(docClass);
		bulk(jsonFile, docTypeName);
	}

	public void bulk(File jsonFile, String docTypeName) throws ElasticSearchException {
		List<String> jsonLines = null;
		try {
			jsonLines = Files.readAllLines(jsonFile.toPath());
		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}
		String json = String.join("\n", jsonLines);
		bulk(json, docTypeName);
	}

	public Document bulkIndex(String dataFPath, String defDocTypeName) throws ElasticSearchException {
		return bulkIndex(dataFPath, defDocTypeName, -1, new ESOptions[0]);
	}

	public Document bulkIndex(String dataFPath, ESOptions... options) throws ElasticSearchException {
		return bulkIndex(dataFPath, null, -1, options);
	}

	public Document bulkIndex(String dataFPath, String defDocTypeName,
		ESOptions... options) throws ElasticSearchException {
		return bulkIndex(dataFPath, defDocTypeName, -1, options);
	}

	public Document bulkIndex(String dataFPath, String defDocTypeName,
		Integer batchSize, ESOptions... esOptions) throws ElasticSearchException {
		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.index.IndexAPI.bulkIndex");
		BulkIndexOptions bulkOptions = new BulkIndexOptions(esOptions);
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
			if (batchSize == null || batchSize < 0) batchSize = 100;
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
							"\nIndexDef object did not precede the first Document object in the json file: " + dataFPath + "\n" +
							"Error was found at line " + reader.lineCount + " of json data file.\n";
						System.err.println(errMess);
						throw new ElasticSearchException(errMess);
					} else if (!bulkOptions.append){
						esFactory.indexAPI().define((IndexDef) obj, bulkOptions.createIfNotExists);
					}
				} else if (obj instanceof CurrentDocType) {
					currDocTypeName = ((CurrentDocType) obj).name;
				} else if (obj instanceof Document) {
					firstDocumentWasRead = true;
					Document doc = (Document) obj;
					if (currDocTypeName != null) {
						doc.type = currDocTypeName;
					}
					docCounter++;

					echo("Indexing document #" + docCounter + ": " + doc.getId(), UserIO.Verbosity.Level1);

					// Keep the first document read as a prototype.
					if (docPrototype == null) docPrototype = doc;
					docNum++;
					String id = doc.getId();
					if (bulkOptions.verbose) {
						System.out.println("Loading document #" + docNum + ": " + id);
					}
					jsonLine = mapper.writeValueAsString(doc);
					jsonBatch += "\n" +
						bulkLinePrefix(currDocTypeName, id) +
						"\n" + jsonLine;

					if (currBatchSize > batchSize) {
						for (ESObserver obs : esFactory.observers) {
							obs.observeBulkIndex(batchStart, batchStart + currBatchSize, indexName(), currDocTypeName);
						}
						bulk(jsonBatch, defDocTypeName);
						batchStart += currBatchSize;
						currBatchSize = 0;
						jsonBatch = "";
					} else {
						currBatchSize++;
					}
				} else {
					throw new ElasticSearchException("JSON file " + dataFPath + " contained an object of unsupoorted type: " + obj.getClass().getName());
				}
				obj = reader.readObject();
			}

			if (!jsonBatch.isEmpty()) {
				// Process the very last partial batch
				bulk(jsonBatch, defDocTypeName);
			}
		} catch (FileNotFoundException e) {
			throw new ElasticSearchException("Could not open file " + dataFPath + " for bulk indexing.");
		} catch (IOException e) {
			throw new ElasticSearchException("Could not read from data file " + dataFPath, e);
		} catch (ElasticSearchException e) {
			throw (e);
		} catch (ClassNotFoundException e) {
			throw new ElasticSearchException(e);
		} catch (ObjectStreamReaderException e) {
			throw new ElasticSearchException(e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				throw new ElasticSearchException("Problem closing the JSON object reader for file: " + dataFPath, e);
			}
		}


		// Sleep a bit to give time for ESFactory to incorporate the documents.
		try {
			Thread.sleep(2 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return docPrototype;
	}

	public void bulkIndex(BufferedReader br, String docTypeName, Integer batchSize,
		ESOptions... esOptions) throws IOException, ElasticSearchException {
		if (batchSize == null || batchSize < 0) batchSize = 100;
		BulkIndexOptions options = new BulkIndexOptions(esOptions);
		int batchStart = 1;
		int currBatchSize = 0;
		String jsonBatch = "";
		String jsonLine = null;
		while (true) {
			jsonLine = br.readLine();
			if (jsonLine == null) break;
			if (jsonLine.matches("^(class|bodyEndMarker)=.*$")) continue;
			String id = getLineID(jsonLine, options.verbose);
			jsonBatch +=
			"\n{\"index\": {\"_index\": \"" + indexName() + "\", \"_type\" : \"" + docTypeName + "\", \"_id\": \"" + id + "\"}}" +
			"\n" + jsonLine;

			if (currBatchSize > batchSize) {
				for (ESObserver obs : esFactory.observers) {
					obs.observeBulkIndex(batchStart, batchStart + currBatchSize, indexName(), docTypeName);
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
		JSONObject jsonObject = new JSONObject(jsonLine);
		if (!jsonObject.has("id")) {
			throw new ElasticSearchException("JSON line did not specify the doc ID: "+jsonLine);
		}
		String id = jsonObject.getString("id");
		if (verbose) {
			System.out.println("Indexing doc with ID " + id);
		}

		return id;
	}


	public void dumpToFile(File outputFile, Class<? extends Document> docClass,
		String freeformQuery, Set fieldsToIgnore) throws ElasticSearchException {
		dumpToFile(outputFile, docClass, null, freeformQuery, fieldsToIgnore);
	}

	public <T extends Document> void dumpToFile(File outputFile, String freeformQuery,
		String docTypeName, T docPrototype, Boolean intoSingleJsonFile) throws ElasticSearchException {
		try {
			SearchResults<T> results = esFactory.searchAPI().search(freeformQuery, docTypeName, docPrototype);
			dumpToFile(outputFile, results, intoSingleJsonFile);
		} catch (Exception e) {
			throw new ElasticSearchException(e);
		}
	}

	public <T extends Document> void dumpToFile(File outputFile, Class<T> docClass) throws ElasticSearchException {
		T docPrototype = (T) Document.prototype(docClass);
		String esTypeName = Document.determineType(docPrototype);
		SearchResults<T> allDocs = (SearchResults<T>) listAll(esTypeName, docPrototype);
		dumpToFile(outputFile, allDocs, true);
	}

	public <T extends Document> void dumpToFile(File outputFile, Class<T> docClass, String esTypeName) throws ElasticSearchException {
		Document docPrototype = Document.prototype(docClass);
		SearchResults<T> allDocs = (SearchResults<T>) listAll(esTypeName, docPrototype);
		dumpToFile(outputFile, allDocs, true);
	}

	public <T extends Document> void dumpToFile(
		File file, Class<? extends Document> docClass,
		String esDocType, String query, Set<String> fieldsToIgnore)
		throws ElasticSearchException {

		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.index.IndexAPI.dumpToFile");
		Document docPrototype = Document.prototype(docClass);
		esDocType = Document.determineType(esDocType, docPrototype);
		tLogger.trace("retrieving docs that fit query=" + query);
		SearchResults<T> allDocs =
			(SearchResults<T>) esFactory.searchAPI().search(query, esDocType, docPrototype);
		tLogger.trace("GOT docs that fit query=" + query + ". total hits=" + allDocs.getTotalHits());

		dumpToFile(file, allDocs, true, fieldsToIgnore);
	}

	public void dumpToFile(File outputFile, SearchResults<? extends Document> results,
		Boolean intoSingleJsonFile) throws ElasticSearchException {
		dumpToFile(outputFile, results, intoSingleJsonFile, (Set)null);
	}

	public void dumpToFile(File outputFile, SearchResults<? extends Document> results,
		Boolean intoSingleJsonFile, Set<String> fieldsToIgnore) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.index.IndexAPI.dumpToFile");

		if (fieldsToIgnore == null) {
			fieldsToIgnore = new HashSet<String>();
		}

		tLogger.trace("invoked with outputFile=" + outputFile.getAbsolutePath() + ", results.getTotalHits()=" + results.getTotalHits());
		System.out.println("== dumpToFile: invoked with outputFile=" + outputFile.getAbsolutePath() + ", results.getTotalHits()=" + results.getTotalHits());
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
			Map<String, Object> docMap = new HashMap<String, Object>();
			Iterator<?> iter = results.iterator();
			while (iter.hasNext()) {
				Hit<Document> aScoredDoc = (Hit<Document>) iter.next();
				docMap = objectMapper.convertValue(aScoredDoc.getDocument(), docMap.getClass());
				Map<String, Object> additionalFields = (Map<String, Object>) docMap.get("additionalFields");
				for (String fld : fieldsToIgnore) {
					additionalFields.remove(fld);
				}
				if (intoSingleJsonFile) {
					String json = PrettyPrinter.print(docMap);
					json = new PrettyPrinter().formatAsSingleLine(json);
					fWriter.write(json + "\n");
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
		String docFilePath = outputDir + "/" + docID + ".txt";
		String docContent = doc.toString();
		FileWriter writer = new FileWriter(new File(docFilePath));
		writer.write("bodyEndMarker=NEW_LINE\n");
		writer.write(docContent);
		writer.close();
	}

	protected static class BulkIndexOptions {
		boolean verbose = false;
		boolean append = false;
		boolean createIfNotExists = false;
		public BulkIndexOptions(ESOptions... esOptions) {
			for (ESOptions option: esOptions) {
				if (option == ESOptions.VERBOSE) {
					verbose = true;
				}
				if (option == ESOptions.APPEND) {
					append = true;
				}
				if (option == ESOptions.CREATE_IF_NOT_EXISTS) {
					createIfNotExists = true;
				}
			}
		}
	}
}
