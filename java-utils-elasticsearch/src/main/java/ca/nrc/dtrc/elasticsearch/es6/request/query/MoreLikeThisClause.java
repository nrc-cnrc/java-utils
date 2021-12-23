package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.Document;
import ca.nrc.dtrc.elasticsearch.es6.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.es6.StreamlinedClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class MoreLikeThisClause extends BooleanClause {

	private Document likeDoc;
	StreamlinedClient esClient = null;

	public MoreLikeThisClause() throws ElasticSearchException {
		super();
		init__MoreLikeThisClause(null, null);
	}

	public MoreLikeThisClause(Document doc, StreamlinedClient _esClient) throws ElasticSearchException {
		super(null);
		init__MoreLikeThisClause(doc, _esClient);
	}

	protected void init__MoreLikeThisClause(Document doc, StreamlinedClient _esClient) throws ElasticSearchException {
		clauseName = "more_like_this";
		hasSingleElement = true;
		esClient = _esClient;
		likeDoc(doc);
	}


	private MoreLikeThisClause likeDoc(Document doc) throws ElasticSearchException {
		likeDoc = doc;
		JSONObject body = mltBody(doc);
		add(body);

		return this;
	}

	private JSONObject mltBody(Document doc) throws ElasticSearchException {
		JSONObject body = null;
		if (doc != null) {
			doc.ensureNonNulType();
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> docMap =
				mapper.convertValue(doc, Map.class);


			// First, generate the list of searchable fields
			Set<String> searchableFields = new HashSet<String>();
			{
				for (String fieldName : docMap.keySet()) {
					// Ignore all but the 'text' fields
					String fieldType =
					esClient.getFieldType(fieldName, doc.type);
					if (fieldType != null && fieldType.equals("text") &&
					!fieldName.equals("id")) {
						searchableFields.add(fieldName);
					}
				}
			}

			// Create a JSON representation of the searchable fields
			JSONArray jsonSearchableFields = new JSONArray();
			List<String> sortedSearchableFields = new ArrayList<String>(searchableFields);
			Collections.sort(sortedSearchableFields);
			for (String fieldName: sortedSearchableFields) {
				jsonSearchableFields.put(
				jsonSearchableFields.length(), fieldName);
			}

			// Create a JSON representation of the query document (searchable fields
			// only)
			JSONObject jsonQueryDoc = new JSONObject();
			for (String fieldName : searchableFields) {
				Object fieldValue = docMap.get(fieldName);
				jsonQueryDoc.put(fieldName, fieldValue);
			}

			docMap.keySet();
			body = new JSONObject()
				.put("query", new JSONObject()
					.put("more_like_this", new JSONObject()
						.put("min_term_freq", 1)
						.put("min_doc_freq", 1)
						.put("max_query_terms", 12)
						.put("fields", jsonSearchableFields)
						.put("like", new JSONObject()
							.put("_index", esClient.indexName)
							.put("doc", jsonQueryDoc)
						)
					)
				)
				.put("highlight", new JSONObject()
					.put("fields", new JSONObject()
						.put("content", new JSONObject()
							.put("type", "plain")
						)
					.put("shortDescription", new JSONObject()
						.put("type", "plain")
					)
				)
				.put("order", "score")
			)
			;
		}
		return body;
	}

}
