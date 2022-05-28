package ca.nrc.dtrc.elasticsearch.search;

import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.dtrc.elasticsearch.request.*;
import ca.nrc.introspection.Introspection;
import ca.nrc.introspection.IntrospectionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API for carrying out different types of ElasticSearch searches.
 */
public abstract class SearchAPI extends ES_API {
	/**
	 * Strategies for looping through lists of hits.
	 *
	 * SCROLL: Uses ES 'scroll' approach. This can be faster, but it may raise
	 *   exceptions if we carry out more than 500 searches in the space of
	 *   1 minute.
	 *
	 * SEARCH_AFTER: uses ES 'search after' approach. This may be faster, but
	 *   it will NOT raise exceptions even if you issue more than 500 searches
	 *   in the space of 1 minute.
	 *
	 * NONE: Does not use any pagination strategy. This means you will only be
	 *    able to retrieve hits that are contained on the very first page of hits.
	 *    This can be faster in situations where you only care about things like:
	 *    - Finding out the total number of available hits
	 *    - Getting a hit (any hit)
	 *    - Getting the very first hit
	 */
	public static enum PaginationStrategy {SCROLL, SEARCH_AFTER, NONE}

	public PaginationStrategy paginateWith = PaginationStrategy.SEARCH_AFTER;

	protected abstract URL searchURL(String docTypeName) throws ElasticSearchException;
	protected abstract void addType2mltBody(String esDocType, JSONObject mltBody) throws ElasticSearchException;

	// Those fields should NOT be included in the list of searchable fields for
	// a more like this query, because they are generic to all Document classes,
	// and this can lead to retrieveing documents of the wrong class (at least with
	// ES7 where there are no type mappings anymore)
	//
	protected static final Set<String> mltExcludedFields = new HashSet<String>();
	static {
		Collections.addAll(mltExcludedFields,
			new String[] {
				"lang", "id", "idWithoutType", "_detect_language"
			}
		);
	};

	public SearchAPI(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
		init__SearchAPI((PaginationStrategy)null);
	}

	public SearchAPI(ESFactory _esFactory, PaginationStrategy _paginateWith) throws ElasticSearchException {
		super(_esFactory);
		init__SearchAPI(_paginateWith);
	}

	private void init__SearchAPI(PaginationStrategy _paginationStrategy) {
		if (_paginationStrategy != null) {
			this.paginateWith = _paginationStrategy;
		}
	}

	/**
	 * Name of the field to use as a tie breaker for sorting criteria
	 */
	protected String sortTieBreakerField() {
		return "id";
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
		return search(freeformQuery, null, docPrototype, xtraReqSpecs);
	}

	public <T extends Document> SearchResults<T> search(
		String freeformQuery, String docTypeName, T docPrototype,
		RequestBodyElement... additionalSearchSpecs)
		throws ElasticSearchException {

		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es5.StreamlinedClient.searchFreeform");

		Query queryBody = null;

		if (freeformQuery != null && !freeformQuery.matches("^\\s*$")) {
			queryBody = new Query(
				new JSONObject()
					.put("query_string", new JSONObject()
					.put("query", freeformQuery)
				));
		} else {
			queryBody = new Query(
				new JSONObject()
					.put("exists", new JSONObject()
					.put("field", "id")
				));
		}

		SearchResults<T> hits =
			search(queryBody, docTypeName, docPrototype,
				additionalSearchSpecs);

		tLogger.trace("Returning results with #hits=" + hits.getTotalHits());

		return hits;
	}

	public <T extends Document> SearchResults<T> search(
		Query queryBody, T docPrototype) throws ElasticSearchException {
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

		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.search.SearchAPI.search_4");

		RequestBodyElement[] bodyElements =
			new RequestBodyElement[additionalBodyElts.length + 1];
			bodyElements[0] = query;
		for (int ii = 1; ii < bodyElements.length; ii++) {
			bodyElements[ii] = additionalBodyElts[ii - 1];
		}

		bodyElements = ensureSortingForSearchAfter(bodyElements);

		RequestBodyElement mergedElt = RequestBodyElement.mergeElements(bodyElements);
		if (!mergedElt.containsKey("highlight")) {
			Highlight highlight = new Highlight().hihglightField("longDescription");
			mergedElt = RequestBodyElement.mergeElements(mergedElt, highlight);
		}


		JSONObject jsonObj = mergedElt.jsonObject();
		if (esFactory.version() >= 7) {
			// For ES7, we need to add track_total_hits, otherwise the total nubmer
			// of hits will max out at 10K
			jsonObj.put("track_total_hits", true);
		}

		SearchResults<T> results = search(jsonObj, docTypeName, docPrototype);
		logger.trace("returning results with #hits=" + results.getTotalHits());
		return results;
	}

	/**
	 * When using 'search with' for paginating search results, this method
	 * ensures that the array of request body elements contains at least one
	 * sort element and this sort criteria will produce a unique value for each
	 * document.
	 */
	private RequestBodyElement[] ensureSortingForSearchAfter(RequestBodyElement[] elements) {
		RequestBodyElement[] augmentedElements = elements;
		if (paginateWith == PaginationStrategy.SEARCH_AFTER) {
			boolean hasSort = false;
			for (RequestBodyElement elt : elements) {
				if (elt instanceof Sort) {
					// Make sure that the existing sort element uses the id field as
					// a tie breaker
					Sort sort = (Sort) elt;
					if (!sort.hasCriteria("id")) {
						sort.sortBy("id", Sort.Order.asc);
					}
					hasSort = true;
				}
			}
			if (!hasSort) {
				augmentedElements = new RequestBodyElement[elements.length + 1];
				for (int ii = 0; ii < elements.length; ii++) {
					augmentedElements[ii] = elements[ii];
				}
				Sort sort = new Sort().sortBy(sortTieBreakerField(), Sort.Order.asc);
				augmentedElements[augmentedElements.length - 1] = sort;
			}
		}
		return augmentedElements;
	}

	/**
	 * When using 'search after' to paginate search results, this method ensures
	 * that the JSON request object includes a sort criteria and that this sort
	 * criteria will produce a unique value for each document.
	 *
	 * This is to ensure that we can loop through documents using search_after.
	 */
	private JSONObject ensureSortingForSearchAfter(JSONObject request) {
		if (paginateWith == PaginationStrategy.SEARCH_AFTER) {
			if (!request.has("sort")) {
				request.put("sort", new JSONArray()
					.put(new JSONObject()
						.put("_score", new JSONObject()
							.put("order", "desc")
						)
					)
					.put(new JSONObject()
						.put(sortTieBreakerField(), new JSONObject()
							.put("order", "asc")
						)
					)
				);
			}
		}
		return request;
	}

	public <T extends Document> SearchResults<T> search(
		JSONObject jsonQuery,
		String docTypeName, T docPrototype) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.search.SearchAPI.search__3");

		docTypeName = Document.determineType(docTypeName, docPrototype);

		URL url = searchURL(docTypeName);
		tLogger.trace("url=" + url + ", jsonQuery=" + jsonQuery);
		String jsonResponse = transport().post(url, jsonQuery.toString());

		tLogger.trace("post returned jsonResponse=" + jsonResponse);

		SearchResults<T> results = null;
		if (paginateWith == PaginationStrategy.SCROLL) {
			results = new SearchResults_Scroll<T>(jsonResponse,
				docPrototype, esFactory, url);
		}  else {
			results = new SearchResults_SearchAfter<T>(
				jsonQuery, jsonResponse, docPrototype, esFactory, url);
		}

		tLogger.trace("returning results with #hits=" + results.getTotalHits());

		return results;
	}

	public String escapeQuotes(String query) {
		String escQuery = query;
		if (query != null) {
			Matcher matcher = Pattern.compile("\"").matcher(query);
			escQuery = matcher.replaceAll("\\\\\"");
		}

		return escQuery;
	}

	public <T extends Document> SearchResults<T> search(String query, T docPrototype) throws ElasticSearchException {
		String docTypeName = Document.determineType(docPrototype);
		SearchResults<T> hits = search(query, docTypeName, docPrototype);

		return hits;
	}

	public <T extends Document> SearchResults<T> moreLikeThis(T queryDoc)
		throws ElasticSearchException, InterruptedException {
		SearchResults<T> results = moreLikeThis(queryDoc, null);
		return results;
	}

	public <T extends Document> SearchResults<T> moreLikeThis(
		T queryDoc, FieldFilter fldFilter)
		throws ElasticSearchException, InterruptedException {
		return moreLikeThis(queryDoc, fldFilter, null);
	}

	public <T extends Document> SearchResults<T> moreLikeThis(
		T queryDoc, FieldFilter fldFilter, String esDocTypeName)
		throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es5.StreamlinedClient.moreLikeThis");

		Map<String, Object> queryDocMap = null;
		queryDocMap = filterFields(queryDoc, esDocTypeName, fldFilter);

		String esType = Document.determineType(esDocTypeName, queryDoc);
		JSONObject mltBody = moreLikeThisJsonBody(esType, queryDocMap);

		SearchResults<T> results =
			search(mltBody, esType, queryDoc);

		return results;
	}

	public JSONObject moreLikeThisJsonBody(
	String type, Map<String, Object> queryDoc) throws ElasticSearchException {
		ObjectMapper mapper = new ObjectMapper();

		mltRemoveGenericFields(queryDoc);

		// First, generate the list of searchable fields
		Set<String> searchableFields = mltSearchableFields(queryDoc, type);

		// Create a JSON representation of the searchable fields
		JSONArray jsonSearchableFields = new JSONArray();
		List<String> sortedSearchableFields = new ArrayList<String>(searchableFields);
		Collections.sort(sortedSearchableFields);
		for (String fieldName: sortedSearchableFields) {
			jsonSearchableFields.put(jsonSearchableFields.length(), fieldName);
		}

		// Create a JSON representation of the query document (searchable fields
		// only)
		JSONObject jsonQueryDoc = new JSONObject();
		for (String fieldName : searchableFields) {
			Object fieldValue = queryDoc.get(fieldName);
			jsonQueryDoc.put(fieldName, fieldValue);
		}

		JSONObject jsonQuery = composeMLTQuery(type, jsonQueryDoc, jsonSearchableFields);


		return jsonQuery;
	}

	protected JSONObject mltClause(JSONObject mltQuery) {
		JSONObject mlt = null;

		JSONArray must = mltQuery
			.getJSONObject("query")
				.getJSONObject("bool")
					.getJSONArray("must")
			;

		for (int ii=0; ii < must.length(); ii++) {
			JSONObject mustClause = must.getJSONObject(ii);
			if (mustClause.has("more_like_this")) {
				mlt = mustClause
					.getJSONObject("more_like_this");
				break;
			}
		}
		return mlt;
	}


	private JSONObject composeMLTQuery(String esDocType, JSONObject jsonQueryDoc,
		JSONArray jsonSearchableFields) throws ElasticSearchException {
		JSONObject jsonQuery = new JSONObject()
			.put("query", new JSONObject()
				.put("bool", new JSONObject()
					.put("must", new JSONArray()
						.put(new JSONObject()
							.put("match", new JSONObject()
								.put("type", esDocType)
							)
						)
						.put(new JSONObject()
							.put("more_like_this", new JSONObject()
								.put("min_term_freq", 1)
								.put("min_doc_freq", 1)
								.put("max_query_terms", 12)
								.put("fields", jsonSearchableFields)
								.put("like", new JSONObject()
									.put("_index", indexName())
									.put("doc", jsonQueryDoc)
								)
							)
						)
					)
				)
			)
		;
		jsonQuery
			.put("highlight", new JSONObject()
				.put("order", "score")
				.put("fields", new JSONObject()
					.put("content", new JSONObject()
						.put("type", "plain")
					)
					.put("shortDescription", new JSONObject()
						.put("type", "plain")
					)
				)
			)
		;

		jsonQuery = ensureSortingForSearchAfter(jsonQuery);

		addType2mltBody(esDocType, jsonQuery);

		return jsonQuery;
	}


	private Set<String> mltSearchableFields(Collection<Map<String, Object>> queryDocs,
		String esDocType) throws ElasticSearchException {
		Set<String> allDocsFields = new HashSet<String>();
		for (Map<String,Object> aDoc: queryDocs) {
			allDocsFields.addAll(mltSearchableFields(aDoc, esDocType));
		}
		return allDocsFields;
	}


	private Set<String> mltSearchableFields(Map<String, Object> queryDoc,
														 String esDocType) throws ElasticSearchException {
		Set<String> searchableFields = new HashSet<String>();
		{
			for (String fieldName : queryDoc.keySet()) {
				if (mltExcludedFields.contains(fieldName)) {
					continue;
				}
				// Ignore all but the 'text' fields
				String fieldType = esFactory.indexAPI().fieldType(fieldName, esDocType);
				if (fieldType != null && fieldType.equals("text")) {
					searchableFields.add(fieldName);
				}
			}
		}
		return searchableFields;
	}

	private void mltRemoveGenericFields(Map<String, Object> queryDoc) {
		for (String field: mltExcludedFields) {
			queryDoc.remove(field);
		}
	}


	public <T extends Document> SearchResults<T> moreLikeThese(List<T> queryDocs)
		throws ElasticSearchException {
		return moreLikeThese(queryDocs, null, null);
	}

	public <T extends Document> SearchResults<T> moreLikeThese(
		List<T> queryDocs, FieldFilter fldFilter)
		throws ElasticSearchException {
		return moreLikeThese(queryDocs, fldFilter, null);
	}

	public <T extends Document> SearchResults<T> moreLikeThese(List<T> queryDocs, FieldFilter fldFilter, String esDocTypeName) throws ElasticSearchException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.es5.StreamlinedClient.moreLikeThisese");

		List<Map<String, Object>> queryDocMaps = null;
		queryDocMaps = filterFields(queryDocs, esDocTypeName, fldFilter);

		String esType = Document.determineType(esDocTypeName, queryDocs.get(0));
		JSONObject mltBody = moreLikeTheseJsonBody(esType, queryDocMaps);


		SearchResults results = null;
		results = search(mltBody, esDocTypeName, queryDocs.get(0));

		return results;
	}


	private String moreLikeTheseJsonBody__OLD(String type, List<Map<String, Object>> queryDocMaps) throws ElasticSearchException {
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
					mlt.put("max_query_terms", 12);

					ArrayNode fields = nodeFactory.arrayNode();
					mlt.set("fields", fields);

					ArrayNode like = nodeFactory.arrayNode();
					mlt.set("like", like);
					{
						for (Map<String, Object> aQueryDoc : queryDocMaps) {
							ObjectNode queryDocDef = nodeFactory.objectNode();
							like.add(queryDocDef);
							queryDocDef.put("_index", indexName());
							ObjectNode doc = nodeFactory.objectNode();
							queryDocDef.set("doc", doc);
							for (String fieldName : aQueryDoc.keySet()) {
								// Ignore all but the 'text' fields
								String fieldType = esFactory.indexAPI().fieldType(fieldName, type);
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

		return jsonBody;
	}

	private JSONObject moreLikeTheseJsonBody(String type, List<Map<String, Object>> queryDocMaps) throws ElasticSearchException {
		ObjectMapper mapper = new ObjectMapper();

		Set<String> searchableFields = this.mltSearchableFields(queryDocMaps, type);
		JSONArray jsonFields = new JSONArray();
		for (String aField: searchableFields) {
			jsonFields.put(aField);
		}

		JSONArray jsonLike = new JSONArray();
		for (Map<String,Object> aQueryDoc: queryDocMaps) {
			jsonLike.put(new JSONObject()
				.put("_index", indexName())
				.put("doc", aQueryDoc)
			);
		}

		JSONObject root = new JSONObject()
			.put("query", new JSONObject()
				.put("bool", new JSONObject()
					.put("must", new JSONArray()
						.put(new JSONObject()
							.put("match", new JSONObject()
								.put("type", type)
							)
						)
						.put(new JSONObject()
							.put("more_like_this", new JSONObject()
								.put("min_term_freq", 1)
								.put("max_query_terms", 12)
								.put("min_doc_freq", 1)
								.put("fields", jsonFields)
								.put("like", jsonLike)
							)
						)
					)
				)
			)
		;

		root = ensureSortingForSearchAfter(root);

		return root;
	}

	public Map<String, Object> filterFields(Document queryDoc) throws ElasticSearchException {
		return filterFields(queryDoc, null, null);
	}


	public Map<String, Object> filterFields(Document queryDoc, FieldFilter filter) throws ElasticSearchException, DocumentException {
		return filterFields(queryDoc, null, filter);
	}

	public <T extends Document> Map<String, Object> filterFields(
		T queryDoc, String esDocType, FieldFilter filter)
		throws ElasticSearchException {
		Map<String, Object> objMap = new HashMap<String, Object>();
		esDocType = Document.determineType(esDocType, queryDoc);

		Map<String, Object> unfilteredMemberAttibutes = null;
		try {
			unfilteredMemberAttibutes = Introspection.fieldValues(queryDoc);
		} catch (IntrospectionException e) {
			throw new ElasticSearchException(e);
		}

		// Filter member attributes
		for (String fieldName : unfilteredMemberAttibutes.keySet()) {
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
		for (String fieldName : queryDoc.getAdditionalFields().keySet()) {
			fieldName = "additionalFields." + fieldName;
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

	public <T extends Document> List<Map<String, Object>> filterFields(List<T> queryDocs, String esDocType, FieldFilter filter) throws ElasticSearchException {
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (Document aDoc : queryDocs) {
			Map<String, Object> aMap = filterFields(aDoc, esDocType, filter);
			maps.add(aMap);
		}

		return maps;
	}

	protected boolean isTextField(String esDocType, String fieldName) throws ElasticSearchException {
		boolean isText = false;
		String fieldType = esFactory.indexAPI().fieldType(fieldName, esDocType);
		if (fieldType != null && fieldType.equals("text")) {
			isText = true;
		}
		return isText;
	}

	public <T extends Document> List<T> scroll(String scrollID, T docPrototype)
		throws ElasticSearchException {

		Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.search.SearchAPI.scroll");
		List<Hit<T>> scoredHits = nextHitsPage_Scroll(scrollID, docPrototype);
		List<T> unscoredHits = new ArrayList<T>();
		for (Hit<T> aScoredHit : scoredHits) {
			unscoredHits.add(aScoredHit.getDocument());
		}

		return unscoredHits;
	}

	public <T extends Document> List<Hit<T>> nextHitsPage_Scroll(
		String scrollID, T docPrototype) throws ElasticSearchException {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.search.SearchAPI.nextHitsPage_Scroll");
		URL url = urlBuilder().forEndPoint("_search/scroll").build();

		JSONObject postJson = new JSONObject()
			.put("scroll_id", scrollID)
			.put("scroll", "1m")
			;
		String jsonResponse = null;
		jsonResponse = transport().post(url, postJson.toString());

		Pair<Pair<Long, String>, List<Hit<T>>> parsedResults = null;
		try {
			parsedResults = respMapper.parseJsonSearchResponse(jsonResponse, docPrototype);
		} catch (ElasticSearchException e) {
			logger.error("scrollID="+scrollID+": parseJsonSearchResponse raised exception!");
			throw e;
		}

		return parsedResults.getRight();
	}

	public <T extends Document> List<Hit<T>> nexHitsPage__SearchAfter(
		JSONObject query, Object searchAfterValue, T docPrototype) throws ElasticSearchException {
		Logger logger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.search.SearchAPI.nexHitsPage__SearchAfter");
		URL url = urlBuilder().forEndPoint("_search").build();

		JSONObject postJson = new JSONObject(query)
			.put("search_after", new JSONArray()
				.put(searchAfterValue)
			);
		String jsonResponse = null;
		jsonResponse = transport().post(url, postJson.toString());

		Pair<Pair<Long, String>, List<Hit<T>>> parsedResults = null;
		try {
			parsedResults = respMapper.parseJsonSearchResponse(jsonResponse, docPrototype);
		} catch (ElasticSearchException e) {
//			logger.error("scrollID="+scrollID+": parseJsonSearchResponse raised exception!");
			throw e;
		}

		return parsedResults.getRight();
	}
}
