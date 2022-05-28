package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPI;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import ca.nrc.dtrc.elasticsearch.request.JsonString;
import ca.nrc.dtrc.elasticsearch.request.Query;
import ca.nrc.dtrc.elasticsearch.request.RequestBodyElement;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import static ca.nrc.dtrc.elasticsearch.ESFactory.*;
import ca.nrc.ui.commandline.UserIO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Client for carrying out ALL ElasticSearch operations
 * @deprecated
 * The functionality of StreamlinedClient is now split in the following classes:
 * IndexAPI, CrudAPI, SearchAPI and ClusterAPI_v7.
 */
@Deprecated
public abstract class StreamlinedClient extends ES_API {

	protected abstract ESFactory makeESFactory(String indexName, Double sleepSecs, ESOptions[] options) throws ElasticSearchException;

	public String getIndexName() {
		return esFactory.indexName;
	}

	public void setUserIO(UserIO _userIO) {
		esFactory.setUserIO(_userIO);
	}

	public UserIO getUserIO() {
		return esFactory.userIO;
	}

	public StreamlinedClient(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
		init_StreamlinedClient(_esFactory);
	}

	protected void init_StreamlinedClient(ESFactory _esFactory) throws ElasticSearchException {
		init_StreamlinedClient(
			_esFactory.indexName, _esFactory.sleepSecs, new ESOptions[0]);
	}

	protected void init_StreamlinedClient(
		String _indexName, Double _sleepSecs, ESOptions... options)
		throws ElasticSearchException {
		esFactory = makeESFactory(_indexName, _sleepSecs, options);
	}

	@JsonIgnore
	public StreamlinedClient setErrorPolicy(ErrorHandlingPolicy policy) {
		esFactory.setErrorPolicy(policy);
		return this;
	}

	@JsonIgnore
	public ErrorHandlingPolicy getErrorPolicy() {
		return esFactory.getErrorPolicy();
	}

	@JsonIgnore
	public ResponseMapper getRespMapper() {
		return respMapper;
	}

	public StreamlinedClient setSleepSecs(double _sleepSecs) {
		this.esFactory.sleepSecs = _sleepSecs;
		return this;
	}

	public StreamlinedClient setIndexName(String _indexName) {
		this.esFactory.indexName = IndexAPI.canonicalIndexName(_indexName);

		return this;
	}

	public IndexAPI indexAPI() throws ElasticSearchException {
		IndexAPI api = esFactory.indexAPI();
		return api;
	}

	public CrudAPI crudAPI() throws ElasticSearchException {
		CrudAPI api = esFactory.crudAPI();
		return api;
	}

	public ClusterAPI clusterAPI() throws ElasticSearchException {
		ClusterAPI api = esFactory.clusterAPI();
		return api;
	}

	public SearchAPI searchAPI() throws ElasticSearchException {
		SearchAPI api = esFactory.searchAPI();
		return api;
	}

	public StreamlinedClient setServer(String _serverName) {
		this.esFactory.setServer(_serverName);
		return this;
	}

	public StreamlinedClient setPort(int _port) {
		this.esFactory.setPort(_port);
		return this;
	}

	public boolean indexExists() throws ElasticSearchException {
		return indexAPI().exists();
	}
	
	public String putDocument(Document doc) throws ElasticSearchException {
		return crudAPI().putDocument(doc);
	}

	public String putDocument(String type, Document dynDoc) throws ElasticSearchException {
		return crudAPI().putDocument(type, dynDoc);
	}

	public String putDocument(String type, String docID, String jsonDoc) throws ElasticSearchException {
		return crudAPI().putDocument(type, docID, jsonDoc);
	}

	public void deleteDocumentWithID(String docID, Class<? extends Document> docClass) throws ElasticSearchException {
		crudAPI().deleteDocumentWithID(docID, docClass);
	}

	public void deleteDocumentWithID(String docID, String esDocType) throws ElasticSearchException {
		crudAPI().deleteDocumentWithID(docID, esDocType);
	}


	public <T extends Document> List<T> listFirstNDocuments(T docPrototype, Integer maxN) throws ElasticSearchException {
		return indexAPI().listFirstNDocuments(docPrototype,maxN);
	}

	public <T extends Document> SearchResults<T> listAll(
		String esDocTypeName, T docPrototype) throws ElasticSearchException {
		return indexAPI().listAll(esDocTypeName, docPrototype);
	}

	public <T extends Document> SearchResults<T> listAll(T docPrototype) throws ElasticSearchException {
		return indexAPI().listAll(docPrototype);
	}

	public <T extends Document> SearchResults<T> listAll(
		String esDocTypeName, T docPrototype, RequestBodyElement... options)
		throws ElasticSearchException {

		return indexAPI().listAll(esDocTypeName, docPrototype, options);
	}

	public <T extends Document> SearchResults<T> search(
		String freeformQuery, String docTypeName,
		T docPrototype) throws ElasticSearchException {
		return searchAPI().search(freeformQuery, docTypeName, docPrototype);
	}

	public <T extends Document> SearchResults<T> search(
		String freeformQuery, T docPrototype,
		RequestBodyElement... xtraReqSpecs) throws ElasticSearchException {
		return searchAPI().search(freeformQuery, docPrototype, xtraReqSpecs);
	}


	public <T extends Document> SearchResults<T> search(
		String freeformQuery, String docTypeName, T docPrototype,
		RequestBodyElement... additionalSearchSpecs)
		throws ElasticSearchException {

		return searchAPI()
			.search(freeformQuery, docTypeName, docPrototype, additionalSearchSpecs);
	}

	public <T extends Document> SearchResults<T> search(Query queryBody, T docPrototype) throws ElasticSearchException {
		return searchAPI().search(queryBody, null, docPrototype);
	}

	public <T extends Document> SearchResults<T> search(
		Query query, String docTypeName, T docPrototype) throws ElasticSearchException {
		return searchAPI()
			.search(query, docTypeName, docPrototype, new RequestBodyElement[0]);
	}

	public <T extends Document> SearchResults<T> search(
		Query query, T docPrototype, RequestBodyElement... additionalSearchSpecs)
		throws ElasticSearchException {
		return searchAPI()
			.search(query, null, docPrototype, additionalSearchSpecs);
	}

	public <T extends Document> SearchResults<T> search(
		Query query, String docTypeName, T docPrototype,
		RequestBodyElement... additionalBodyElts) throws ElasticSearchException {

		return searchAPI()
			.search(query, docTypeName, docPrototype, additionalBodyElts);
	}

	private <T extends Document> SearchResults<T> search(
		JSONObject jsonQuery, String docTypeName, T docPrototype) throws ElasticSearchException {
		return searchAPI()
			.search(jsonQuery, docTypeName, docPrototype);
	}

	public <T extends Document> SearchResults<T> search(
		String query, T docPrototype) throws ElasticSearchException {
		return searchAPI()
			.search(query, docPrototype);
	}

	public <T extends Document> List<T> scroll(String scrollID, T docPrototype) throws ElasticSearchException {
		return searchAPI().scroll(scrollID, docPrototype);
	}

	public <T extends Document> List<Hit<T>> scrollScoredHits(String scrollID, T docPrototype) throws ElasticSearchException {
		return searchAPI().nextHitsPage_Scroll(scrollID, docPrototype);
	}

	public void clearIndex() throws ElasticSearchException, InterruptedException {
		indexAPI().clear(true);
	}

	public void deleteIndex() throws ElasticSearchException {
		indexAPI().delete();
	}

	public <T extends Document> SearchResults<T> moreLikeThis(T queryDoc) throws ElasticSearchException, IOException, InterruptedException {
		return searchAPI().moreLikeThis(queryDoc);
	}

	public <T extends Document> SearchResults<T> moreLikeThis(
		T queryDoc, FieldFilter fldFilter) throws ElasticSearchException, IOException, InterruptedException {
		return searchAPI().moreLikeThis(queryDoc, fldFilter);
	}

	public <T extends Document> SearchResults<T> moreLikeThis(
		T queryDoc, FieldFilter fldFilter, String esDocTypeName) throws ElasticSearchException, IOException, InterruptedException {
		return searchAPI().moreLikeThis(queryDoc, fldFilter, esDocTypeName);
	}

	public <T extends Document> SearchResults<T> moreLikeThese(
		List<T> queryDocs) throws ElasticSearchException, IOException, InterruptedException {
		return searchAPI().moreLikeThese(queryDocs);
	}

	public <T extends Document> SearchResults<T> moreLikeThese(
		List<T> queryDocs, FieldFilter fldFilter) throws ElasticSearchException, IOException, InterruptedException {
		return searchAPI().moreLikeThese(queryDocs);
	}

	public <T extends Document> SearchResults<T> moreLikeThese(
		List<T> queryDocs, FieldFilter fldFilter, String esDocTypeName) throws ElasticSearchException, IOException, InterruptedException {
		return searchAPI().moreLikeThese(queryDocs, fldFilter, esDocTypeName);
	}

	public void bulk(File jsonFile, Class<? extends Document> docClass) throws ElasticSearchException, IOException {
		indexAPI().bulk(jsonFile, docClass);
	}

	public void bulk(File jsonFile, String docTypeName) throws ElasticSearchException, IOException {
		indexAPI().bulk(jsonFile, docTypeName);
	}


	public void bulk(String jsonContent, String docTypeName) throws ElasticSearchException, IOException {
		indexAPI().bulk(jsonContent, docTypeName);
	}

	public void bulkIndex(String dataFPath, ESFactory.ESOptions... options)
		throws ElasticSearchException {
		indexAPI().bulkIndex(dataFPath, options);
	}

	public Document getDocumentWithID(String docID, Class<? extends Document> docClass) throws ElasticSearchException {
		return crudAPI().getDocumentWithID(docID, docClass);
	}

	public Document getDocumentWithID(String docID,
		Class<? extends Document> docClass, String esDocType)
	throws ElasticSearchException {
		return crudAPI().getDocumentWithID(docID, docClass, esDocType,
		(Boolean)null);
	}

	public void updateDocument(Class<? extends Document> docClass, String docID, Map<String, Object> partialDoc) throws ElasticSearchException {
		crudAPI().updateDocument(docClass, docID, partialDoc);
	}

	public void updateDocument(String esDocType, String docID, Map<String, Object> partialDoc) throws ElasticSearchException {
		crudAPI().updateDocument(esDocType, docID, partialDoc);
	}

	public void dumpToFile(File outputFile, Class<? extends Document> docClass,
		String freeformQuery, Set fieldsToIgnore) throws ElasticSearchException {
		dumpToFile(outputFile, docClass, null, freeformQuery, fieldsToIgnore);
		indexAPI().dumpToFile(outputFile, docClass, freeformQuery, fieldsToIgnore);
	}

	public <T extends Document> void dumpToFile(File outputFile, String freeformQuery,
		String docTypeName, T docPrototype, Boolean intoSingleJsonFile) throws ElasticSearchException {
		indexAPI().dumpToFile(outputFile, freeformQuery, docTypeName,
			docPrototype, intoSingleJsonFile);
	}

	public <T extends Document> void dumpToFile(File outputFile, Class<T> docClass) throws ElasticSearchException {
		indexAPI().dumpToFile(outputFile, docClass);
	}

	public <T extends Document> void dumpToFile(File outputFile, Class<T> docClass, String esTypeName) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ElasticSearchException {
		indexAPI().dumpToFile(outputFile, docClass, esTypeName);
	}

	public <T extends Document> void dumpToFile(
		File file, Class<? extends Document> docClass,
		String esDocType, String query, Set<String> fieldsToIgnore)
		throws ElasticSearchException {

		indexAPI().dumpToFile(file, docClass,esDocType, query, fieldsToIgnore);
	}

	public DocClusterSet clusterDocuments(String query, String docTypeName, String[] useFields, String algName, Integer maxDocs) throws ElasticSearchException {
		return clusterAPI().clusterDocuments(query, docTypeName, useFields, algName, maxDocs);
	}

	public String clusterDocumentJsonBody(String freeformQuery, String docTypeName, String[] useFields, String algName, Integer maxDocs) throws ElasticSearchException, JsonProcessingException {
		return clusterAPI().clusterDocumentJsonBody(freeformQuery, docTypeName, useFields, algName, maxDocs);
	}

	public void attachObserver(ESObserver _obs) {
		esFactory.attachObserver(_obs);
	}

	public void detachObservers() {
		esFactory.detachObservers();
	}

	public void clearDocType(String docType) throws ElasticSearchException {
		indexAPI().clear(docType);
	}

	public void changeIndexSetting(String settingName, Object settingValue) throws ElasticSearchException {
		indexAPI().changeIndexSetting(settingName, settingValue);
	}

	public void changeIndexSettings(Map<String, Object> settings) throws ElasticSearchException {
		indexAPI().changeIndexSettings(settings);
	}
}
