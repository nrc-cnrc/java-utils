package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

public class SearchAPI_v5 extends SearchAPI {

	public SearchAPI_v5(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}

	@Override
	protected URL searchURL(String docTypeName) throws ElasticSearchException {
		URL url = urlBuilder()
			.forDocType(docTypeName).forEndPoint("_search")
			.scroll().build();
		return url;
	}

	@Override
	protected void addType2mltBody(String esDocType, JSONObject mltQuery) throws ElasticSearchException {
		JSONObject mlt = mltClause(mltQuery);
		mlt.getJSONObject("like").put("_type", esDocType);

		return;
	}
}
