package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPITest;
import org.junit.jupiter.api.BeforeAll;

public class ClusterAPI_v7Test extends ClusterAPITest {
	@BeforeAll
	public static void beforeAll() throws Exception {
		ESTestHelpers helpers = new ESTestHelpers(7);
		boolean skip = helpers.skipTestsUnlessESIsRunning(7);
		if (!skip) {
			helpers.skipTestsUnlessPluginIsInstalled("elasticsearch-carrot2");
		}

		return;
	}

	@Override
	protected int esVersion() {
		return 7;
	}
}


