package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.dtrc.elasticsearch.request.JsonString;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import org.json.JSONObject;

import java.net.URL;

public class SearchAPI_v7mi extends SearchAPI {
	public SearchAPI_v7mi(ESFactory esFactory) throws ElasticSearchException {
		super(esFactory);
	}

	ES7miFactory esFactory() {
		return (ES7miFactory)esFactory;
	}

	@Override
	protected URL searchURL(String docTypeName) throws ElasticSearchException {
		ESUrlBuilder builder = esFactory().urlBuilder(docTypeName)
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
		JSONObject jsonQuery, String docTypeName, T docPrototype)
		throws ElasticSearchException {

		// Initialize results to an empty results set.
		SearchResults<T> results = new SearchResults_Scroll<T>(esFactory());
		IndexAPI_v7mi indexAPI = esFactory().indexAPI();
		if (!indexAPI.exists()) {
			// If the base index does not exist, we raise an exception.
			throw new NoSuchIndexException("Index "+esFactory().indexName+" does not exist");
		}
		try {
			results = super.search(jsonQuery, docTypeName, docPrototype);
		} catch (NoSuchIndexException e) {
			// Because we checked earlier that the base index exists, if NoSuchIndexException
			// is raised here, it means that the index for that particular type does
			// not exist. In that case we just leave results to an empty results set
		}

		return results;
	}

}
