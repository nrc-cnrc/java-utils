package ca.nrc.dtrc.elasticsearch.es7mi;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESFactoryTest;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.es7.ES7Factory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ESFactory_v7miTest extends ESFactoryTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected ESFactory makeESFactory(String indexName) throws ElasticSearchException {
		return new ES7Factory(indexName);
	}

	///////////////////////////////////////////////////////////////////////////
	// DOCUMENTATION TESTS
	///////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////
	// VERIFICATION TESTS
	///////////////////////////////////////////////////////////////////////////

	@Test
	public void test__index4type__HappyPath() throws Exception {
		String baseName = "some-index";
		String type = "sometype";
		String gotIndex = new ES7miFactory(baseName).index4type(type);
		Assertions.assertEquals("some-index__sometype", gotIndex,
			"Wrong index for type "+type);
	}
}
