package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESSpeedTest;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import org.junit.jupiter.api.BeforeAll;

public class ES_v7SpeedTest extends ESSpeedTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected int esVersion() {
		return 7;
	}
}
