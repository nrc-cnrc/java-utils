package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.search.SearchAPITest;
import org.junit.jupiter.api.BeforeAll;

public class SearchAPI_v5Test extends SearchAPITest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(5).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected ESFactory makeESFactory(String _indexname) throws ElasticSearchException {
		return new ES5Factory(_indexname);
	}

	@Override
	protected int esVersion() {
		return 5;
	}
}
