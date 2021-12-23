package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.Document;
import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.index.IndexAPITest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;

public class IndexAPI_v7Test extends IndexAPITest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected ESFactory makeESFactory(String indexName) throws ElasticSearchException {
		return new ES7Factory(indexName);
	}

	@Override
	protected int esVersion() {
		return 7;
	}

	@Override
	protected String expMappingUrl(String typeName) {
		return "http://localhost:9207/test-index/_mapping";
	}

	@Override
	protected String expAllMappingUrl(String typeName) {
		return "http://localhost:9207/test-index";
	}

	@Override
	protected String expDeleteByQueryUrl(String docType) {
		return "http://localhost:9207/test-index/_doc/_delete_by_query";
	}

	@Override
	protected JSONObject expTypes(Class<? extends Document> docClass) throws Exception {
		JSONObject exp = new JSONObject()
			.put("_detect_language", "boolean")
			.put("age", "long")
			.put("firstName", "text")
			.put("id", "text")
			.put("idWithoutType", "text")
			.put("lang", "text")
			.put("name", "text")
			.put("surname", "text")
			.put("type", "text")
			;
		return exp;
	}
}
