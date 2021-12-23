package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import org.json.JSONObject;

import java.net.URL;

public class SearchAPI_v7 extends SearchAPI {
	public SearchAPI_v7(ESFactory esFactory) throws ElasticSearchException {
		super(esFactory);
	}

	@Override
	protected URL searchURL(String docTypeName) throws ElasticSearchException {
		URL url = urlBuilder()
			.forDocType(docTypeName).forEndPoint("_search")
			.includeTypeInUrl(false)
			.scroll().build();
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
}
