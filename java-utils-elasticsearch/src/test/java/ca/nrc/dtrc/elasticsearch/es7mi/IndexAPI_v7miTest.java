package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.Document;
import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.index.IndexAPITest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IndexAPI_v7miTest extends IndexAPITest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected ESFactory makeESFactory(String indexName) throws ElasticSearchException {
		return new ES7miFactory(indexName);
	}

	@Override
	protected int esVersion() {
		return 7;
	}

	@Override
	protected String expMappingUrl(String typeName) {
		return "http://localhost:9200/test-index__"+typeName+"/_mapping";
	}

	@Override
	protected String expAllMappingUrl(String typeName) {
		return "http://localhost:9200/test-index__idefs";
	}

	@Override
	protected String expDeleteByQueryUrl(String docType) {
		return "http://localhost:9200/test-index__"+docType+"/_doc/_delete_by_query";
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

	@Test
	public void test__typeIndices_and_types__HappyPath() throws Exception {
		String dbName = "test-index";
		ES7miFactory factory = new ES7miFactory(dbName);
		IndexAPI_v7mi indexAPI = new ES7miFactory(dbName).indexAPI();

		indexAPI.delete();

		// Check that when the index does not exist, we have an empty set of types
		// and type indices
		//
		new AssertIndex_v7mi(indexAPI)
			.typesAre(new String[0]);

		// Add a document of a first type to the index
		factory.crudAPI().putDocument(new ESTestHelpers.ShowCharacter());
		new AssertIndex_v7mi(indexAPI)
			.typesAre(new String[]  {"character"});

		// Add a document of a second type to the index
		factory.crudAPI().putDocument(new ESTestHelpers.TVShow());
		new AssertIndex_v7mi(indexAPI)
			.typesAre(new String[]  {"character", "show"});
	}
}
