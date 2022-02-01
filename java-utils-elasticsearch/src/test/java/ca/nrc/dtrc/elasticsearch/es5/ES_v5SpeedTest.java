package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESSpeedTest;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import org.junit.jupiter.api.BeforeAll;

public class ES_v5SpeedTest extends ESSpeedTest {

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
