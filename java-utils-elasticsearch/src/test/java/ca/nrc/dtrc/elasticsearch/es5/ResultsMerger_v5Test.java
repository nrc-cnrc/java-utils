package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.ResultsMergerTest;
import org.junit.jupiter.api.BeforeAll;

public class ResultsMerger_v5Test extends ResultsMergerTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(5).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected ESFactory makeESFactory(String indexName) throws ElasticSearchException {
		return new ES5Factory(indexName);
	}
}
