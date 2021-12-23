package ca.nrc.dtrc.elasticsearch.es6.request.query;

import ca.nrc.dtrc.elasticsearch.es6.Document;
import ca.nrc.dtrc.elasticsearch.es6.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.es6.NullDocTypeException;
import ca.nrc.dtrc.elasticsearch.es6.StreamlinedClient;
import ca.nrc.dtrc.elasticsearch.es6.request.RequestBodyElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

public class Query extends RequestBodyElement {
	String docType = null;
	QueryString queryStringClause = null;
	MoreLikeThisClause mltClause = null;
	MustClause mustClause = null;
	MustNotClause mustNotClause = null;
	ShouldClause shouldClause = null;
	FilterClause filterClause = null;

	StreamlinedClient esClient = null;
	ObjectMapper _mapper = null;

	public Query() {
		super(null);
	}

	@Override
	public JSONObject jsonObject() throws ElasticSearchException {
		JSONObject jObj = new JSONObject();

		JSONObject jsonQuery =  buildBoolQueryJson();

		if (jsonQuery != null) {
			jObj.put("query", jsonQuery);
		}

		return jObj;
	}

	private JSONObject buildBoolQueryJson() throws ElasticSearchException {
		JSONObject jsonBool = new JSONObject();
		if (docType == null) {
			throw new NullDocTypeException();
		}

		// Constrain the type of document
		must(new JSONObject()
			.put("match", new JSONObject()
				.put("type", docType)
			)
		);

		if (this.queryStringClause != null) {
			jsonBool.put("query_string", queryStringClause.mainBody());
		}

		if (mltClause != null) {
			jsonBool.put("more_like_this", mltClause.mainBody());
		}

		if (mustClause != null) {
			jsonBool.put("must", mustClause.mainBody());
		}
		if (mustNotClause != null) {
			jsonBool.put("must_not", mustNotClause.mainBody());
		}
		if (shouldClause != null) {
			jsonBool.put("should", shouldClause.mainBody());
		}
		JSONObject jObj = new JSONObject().put("bool", jsonBool);
		return jObj;
	}

	public Query queryString(String _query) throws ElasticSearchException {
		if (_query != null && !_query.matches("^\\s*$")) {
			must(new QueryString(_query).jsonObject());
		}
		return this;
	}


	public Query moreLikeThis(Document queryDoc, StreamlinedClient esClient)
		throws ElasticSearchException {
		if (queryDoc != null) {
			mltClause = new MoreLikeThisClause(queryDoc, esClient);
		}

		return this;
	}

	public Query must(MustClause clause) throws ElasticSearchException {
		JSONObject jObj = null;
		if (clause != null) {
			jObj = clause.jsonObject();
		}
		return must(jObj);
	}

	public Query must(JSONObject clause) throws ElasticSearchException {
		if (clause != null) {
			if (mustClause == null) {
				mustClause = new MustClause();
			}
			mustClause.add(clause);
		}

		return this;
	}

	public Query mustNot(MustNotClause clause) throws ElasticSearchException {
		JSONObject jObj = null;
		if (clause != null) {
			jObj = clause.jsonObject();
		}
		return mustNot(jObj);
	}

	public Query mustNot(JSONObject clause) throws ElasticSearchException {
		if (clause != null) {
			if (mustNotClause == null) {
				mustNotClause = new MustNotClause();
			}
			mustNotClause.add(clause);
		}

		return this;
	}

	public Query should(ShouldClause clause) throws ElasticSearchException {
		JSONObject jObj = null;
		if (clause != null) {
			jObj = clause.jsonObject();
		}
		return should(jObj);
	}

	public Query should(JSONObject clause) throws ElasticSearchException {
		if (clause != null) {
			if (shouldClause == null) {
				shouldClause = new ShouldClause();
			}
			shouldClause.add(clause);
		}

		return this;
	}

	public <T extends Document> Query type(String typeName) throws ElasticSearchException {
		return type(typeName, null);
	}

	public <T extends Document> Query type(T docPrototype) throws ElasticSearchException {
		return type(null, docPrototype);
	}

	public <T extends Document> Query type(String docTypeName, T docPrototype) throws ElasticSearchException {
		if (docTypeName == null) {
			docTypeName = docPrototype.type;
			if (docTypeName == null) {
				throw new NullDocTypeException();
			}
		}
		this.docType = docTypeName;
		return this;
	}


	public Query addFilterTerm(String fldName, String fldConstraint) {
		if (filterClause == null) {
			filterClause = new FilterClause();
		}
		filterClause.addTerm(fldName, fldConstraint);
		return this;
	}

	private boolean isBoolean() {
		boolean answer =
			(mustClause != null || mustNotClause != null ||
			shouldClause != null);
		return answer;
	}

	private ObjectMapper mapper() {
		if (_mapper == null) {
			_mapper = new ObjectMapper();
		}
		return _mapper;
	}
}
