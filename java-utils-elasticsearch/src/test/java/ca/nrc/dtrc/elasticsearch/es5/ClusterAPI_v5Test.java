package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPITest;
import org.junit.jupiter.api.BeforeAll;

public class ClusterAPI_v5Test extends ClusterAPITest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(5).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected int esVersion() {
		return 5;
	}
}
