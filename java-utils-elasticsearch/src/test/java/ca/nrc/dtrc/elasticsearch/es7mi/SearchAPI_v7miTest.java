package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.search.SearchAPITest;
import org.junit.jupiter.api.BeforeAll;

public class SearchAPI_v7miTest extends SearchAPITest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		org.junit.Assume.assumeFalse("Skipping ES7 multi-index tests", true);
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected ESFactory makeESFactory(String _indexName) throws ElasticSearchException {
		ES7miFactory factory = new ES7miFactory(_indexName);
		return factory;
	}

	@Override
	protected int esVersion() {
		return 7;
	}
}
