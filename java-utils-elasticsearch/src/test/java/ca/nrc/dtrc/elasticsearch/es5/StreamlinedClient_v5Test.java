package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.*;
import org.junit.jupiter.api.BeforeAll;

public class StreamlinedClient_v5Test extends StreamlinedClientTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(5).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	public int esVersion() {
		return 5;
	}

//	@Override
//	public StreamlinedClient makeClient() throws ElasticSearchException {
//		return new StreamlinedClient_v5();
//	}
//
//	@Override
//	public StreamlinedClient makeClient(String indexName) throws ElasticSearchException {
//		return new StreamlinedClient_v5(indexName);
//	}

	@Override
	public ESFactory makeESFactory(String indexName) throws ElasticSearchException {
		return new ES5Factory(indexName);
	}
}
