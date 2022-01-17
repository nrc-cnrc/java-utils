package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.search.SearchAPITest;
import org.junit.jupiter.api.BeforeAll;

public class SearchAPI_v7Test extends SearchAPITest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected ESFactory makeESFactory(String _indexName) throws ElasticSearchException {
		return new ES7Factory(_indexName);
	}

	@Override
	protected int esVersion() {
		return 7;
	}
}
