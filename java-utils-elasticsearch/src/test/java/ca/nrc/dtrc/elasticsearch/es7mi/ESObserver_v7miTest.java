package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESObserverTest;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import org.junit.jupiter.api.BeforeAll;

public class ESObserver_v7miTest extends ESObserverTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9200);
		return;
	}

	@Override
	protected ESFactory makeESFactory(String indexName) throws ElasticSearchException {
		return new ES7miFactory(indexName);
	}

	@Override
	protected int esVersion() throws ElasticSearchException {
		return 7;
	}
}

