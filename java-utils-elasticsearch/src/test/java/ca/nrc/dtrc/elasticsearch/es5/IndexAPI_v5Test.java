package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.Document;
import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.index.IndexAPITest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;

public class IndexAPI_v5Test extends IndexAPITest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(5).skipTestsUnlessESIsRunning(9207);
		return;
	}


	@Override
	protected ESFactory makeESFactory(String indexName) throws ElasticSearchException {
		return new ES5Factory(indexName);
	}

	@Override
	protected int esVersion() {
		return 5;
	}

	@Override
	protected String expMappingUrl(String typeName) {
		return "http://localhost:9200/test-index/_mapping/sometype";
	}

	@Override
	protected String expAllMappingUrl(String typeName) {
		return "http://localhost:9200/test-index";
	}

	@Override
	protected String expDeleteByQueryUrl(String docType) {
		return "http://localhost:9200/test-index/_delete_by_query";
	}

	@Override
	protected JSONObject expTypes(Class<? extends Document> docClass) throws Exception {
		JSONObject expected = new JSONObject();
		if (docClass == ESTestHelpers.ShowCharacter.class) {
			expected
			.put("_detect_language", "boolean")
			.put("age", "long")
			.put("firstName", "text")
			.put("id", "keyword")
			.put("idWithoutType", "keyword")
			.put("lang", "text")
			.put("surname", "text")
			.put("type", "text")
			;
		} else {
			throw new Exception("Do not know the expected types for class: "+docClass);
		}
		return expected;
	}


//	@Override
//	protected IndexAPI makeIndexAPI(String _indexName) throws ElasticSearchException {
//
//		return new IndexAPI_v5(new ES5Factory(_indexName));
//	}
//
//	@Override
//	protected CrudAPI makeCrudAPI(String indexName) throws ElasticSearchException {
//		return new CrudAPI_v5(new ES5Factory(indexName));
//	}
}
