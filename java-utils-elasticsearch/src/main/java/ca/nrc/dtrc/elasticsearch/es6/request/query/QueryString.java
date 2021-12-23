package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.request.RequestBodyElement;
import org.json.JSONObject;

public class QueryString extends BooleanClause {
	private String queryString = null;
	private String type = null;

	public QueryString(String _queryString) {
		super();
		this.queryString = _queryString;
	}

	@Override
	public JSONObject jsonObject() {
		String expandedQueryString = queryString;
		if (type != null) {
			expandedQueryString += " type:\""+type+"\"";
		}

		JSONObject json =
			new JSONObject()
				.put("query_string", new JSONObject()
					.put("query", expandedQueryString)
				)
			;
		return json;
	}

	public QueryString typeContraint(String docTypeName) {

		if (docTypeName != null) {
			if (queryString == null) {
				queryString = "";
			}
			if (!queryString.matches(".* \\+type\\s*:.*")) {
				queryString += " +type:\"" + docTypeName + "\"";
			}
		}
		return this;
	}
}
