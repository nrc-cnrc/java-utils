package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESObserverTest;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import org.junit.jupiter.api.BeforeAll;

public class ESObserver_v7Test extends ESObserverTest {

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
	protected int esVersion() throws ElasticSearchException {
		return 7;
	}


}

