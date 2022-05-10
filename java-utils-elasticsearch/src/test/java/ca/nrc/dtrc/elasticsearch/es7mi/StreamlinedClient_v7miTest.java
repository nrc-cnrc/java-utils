package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;

import ca.nrc.dtrc.elasticsearch.*;
import org.junit.jupiter.api.BeforeAll;

public class StreamlinedClient_v7miTest extends StreamlinedClientTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		org.junit.Assume.assumeFalse(true);
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9200);
		return;
	}

	@Override
	public int esVersion() {
		return 7;
	}

	@Override
	public ESFactory makeESFactory(String indexName) throws ElasticSearchException {
		return new ES7miFactory(indexName);
	}
}
