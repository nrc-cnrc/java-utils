package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

public class SearchAPI_v7 extends SearchAPI {
	public SearchAPI_v7(ESFactory esFactory) throws ElasticSearchException {
		super(esFactory);
	}

	@Override
	protected URL searchURL(String docTypeName) throws ElasticSearchException {
		ESUrlBuilder builder = urlBuilder()
			.forDocType(docTypeName).forEndPoint("_search")
			.includeTypeInUrl(false);
		if (paginateWith == SearchAPI.PaginationStrategy.SCROLL) {
			builder.scroll();
		}
		URL url = builder.build();
		return url;
	}

	@Override
	protected void addType2mltBody(String esDocType, JSONObject mltQuery) throws ElasticSearchException {
		JSONObject mlt = mltClause(mltQuery);
		mlt
			.getJSONObject("like")
				.getJSONObject("doc")
					.put("type", esDocType);
	}


	@Override
	public <T extends Document> SearchResults<T> search(
		JSONObject jsonQuery, String docTypeName, T docPrototype,
		Integer batchSize) throws ElasticSearchException {

		jsonQuery = ensureHasTypeRestriction(jsonQuery, docTypeName, docPrototype);

		return super.search(jsonQuery, docTypeName, docPrototype, batchSize);
	}

	/**
	 * Make sure that the JSON body object contains a clause that restricts
	 * search to the desired type.
	 *
	 * This may require some reformatting of the original body.
	 */
	private <T extends Document> JSONObject ensureHasTypeRestriction(
		JSONObject jsonBody, String docTypeName, T docPrototype) throws ElasticSearchException {

		String type = Document.determineType(docTypeName, docPrototype);

		// Make sure the body has a query clause
		if (!jsonBody.has("query")) {
			jsonBody.put("query", new JSONObject());
		}
		JSONObject query = jsonBody.getJSONObject("query");

		JSONObject query_string = null;
		if (query.has("query_string")) {
			// The "query" clause has a "query_string" clause at its root.
			// Move this field inside the "must" clause below...
			//
			query_string = query.getJSONObject("query_string");
			query.remove("query_string");
		}

		// Make sure the query clause has a bool clause
		if (!query.has("bool")) {
			query.put("bool", new JSONObject());
		}
		JSONObject bool = query.getJSONObject("bool");

		// Make sure the bool clause has a must clause and that its value is
		// an array
		if (!bool.has("must")) {
			bool.put("must", new JSONArray());
		}
		JSONArray must = null;
		try {
			must = bool.getJSONArray("must");
		} catch (Exception e) {
			// bool already had a "must" clause, but it was not an array.
			// Replace it with an array
			JSONObject oldMust = bool.getJSONObject("must");
			must = new JSONArray();
			bool.put("must", must);
			for (String fldName: oldMust.keySet()) {
				Object fldVal = oldMust.get(fldName);
				must.put(new JSONObject()
					.put(fldName, fldVal)
				);
			}
		}
		if (query_string != null) {
			// Earlier, we found a "query_string" clause at the root of the
			// "query" clause. Move this "query_string" clause inside the
			// "must" clauuse.
			must.put(new JSONObject()
				.put("query_string", query_string)
			);
		}

		// Now check if the body has a clause that restricts search to the
		// desired type.
		boolean hasTypeRestr = false;
		JSONObject match = null;
		for (int ii=0; ii < must.length(); ii++) {
			JSONObject mustCriterion = must.getJSONObject(ii);
			if (!mustCriterion.has("match")) {
				continue;
			}
			match = mustCriterion.getJSONObject("match");
			if (!match.has("type")) {
				continue;
			}
			if (match.getString("type").equals(type)) {
				// We found a clause in the "must" clause which DOES restrict
				// search to the desired type.
				hasTypeRestr = true;
			}
		}
		if (!hasTypeRestr) {
			// Did not find a clause that restricts search to the desired type.
			// So add one.
			must.put(new JSONObject()
				.put("match", new JSONObject()
					.put("type", type)
				)
			);
		}

		return jsonBody;
	}
}
