package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import ca.nrc.testing.AssertRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;


import java.util.Map;

public abstract class ESSpeedTest {
	protected TestInfo testInfo = null;
	static ESFactory esFactory = null;
	public static ObjectMapper mapper = new ObjectMapper();

	protected abstract int esVersion();

	@BeforeEach
	public void beforeEach(TestInfo _testInfo) throws Exception {
		testInfo = _testInfo;
		if (esFactory == null) {
			esFactory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
			Thread.sleep(2*1000);
		}
	}

	@Test
	public void test__loopThrough_1000docs__SpeedTest(TestInfo testInfo) throws Exception {
		long startMSecs = System.currentTimeMillis();
		PlayLine playLine = new PlayLine("nevermind");
		DocIterator<PlayLine> docIter =
			esFactory.indexAPI().listAll(playLine).docIterator();
		for (int ii=0; ii < 1000; ii++) {
			playLine = docIter.next();
		}

		double currentRuntime =
			1.0 * (System.currentTimeMillis() - startMSecs) / 1000;
		String operationName = "loop-through-1000docs-secs";
		AssertRuntime.runtimeHasNotChanged(
			currentRuntime, 0.20, operationName, testInfo);
	}

	@Test
	public void test__search__SpeedTest(TestInfo testInfo) throws Exception {
		PlayLine playLine = new PlayLine("nevermind");

		long startMSecs = System.currentTimeMillis();
		int totalTimes = 100;
		for (int ii=0; ii < totalTimes; ii++) {
			esFactory.searchAPI().search("Hamlet", playLine);
		}

		double avgRuntime =
			1.0 * (System.currentTimeMillis() - startMSecs) / 1000;
		avgRuntime = avgRuntime / totalTimes;
		String operationName = "avg-search-secs";
		AssertRuntime.runtimeHasNotChanged(
			avgRuntime, 0.20, operationName, testInfo);
	}

	@Test
	public void test__deleteThenPutDocument__SpeedTest(TestInfo testInfo) throws Exception {
		CrudAPI crudAPI = esFactory.crudAPI();
		PlayLine playLine =
			(PlayLine) crudAPI.getDocumentWithID("playline:1.1.28", PlayLine.class);

		long startMSecs = System.currentTimeMillis();
		int totalTimes = 5;
		for (int ii=0; ii < totalTimes; ii++) {
			crudAPI.deleteDocumentWithID(playLine.getId(), PlayLine.class);
			crudAPI.putDocument(playLine);
		}

		double avgRuntime =
			1.0 * (System.currentTimeMillis() - startMSecs) / 1000;
		avgRuntime = avgRuntime / totalTimes;
		String operationName = "avg-deleteThenPutDocument-secs";
		AssertRuntime.runtimeHasNotChanged(
			avgRuntime, 0.20, operationName, testInfo);
	}

	@Test
	public void test__getDocumentWithID__SpeedTest(TestInfo testInfo) throws Exception {
		long startMSecs = System.currentTimeMillis();
		int totalTimes = 100;
		for (int ii=0; ii < totalTimes; ii++) {
			esFactory.crudAPI().getDocumentWithID("playline:1.1.28", PlayLine.class);
		}

		double avgRuntime =
			1.0 * (System.currentTimeMillis() - startMSecs) / 1000;
		avgRuntime = avgRuntime / totalTimes;
		String operationName = "avg-getDocumentWithID-secs";
		AssertRuntime.runtimeHasNotChanged(
			avgRuntime, 0.20, operationName, testInfo);
	}

	@Test
	public void test__updateDocument__SpeedTest(TestInfo testInfo) throws Exception {
		CrudAPI crudAPI = esFactory.crudAPI();
		PlayLine playLine =
			(PlayLine) crudAPI.getDocumentWithID("playline:1.1.28", PlayLine.class);
		Map<String,Object> playLineMap =
			mapper.readValue(
				mapper.writeValueAsString(playLine), Map.class);

		long startMSecs = System.currentTimeMillis();
		int totalTimes = 100;
		for (int ii=0; ii < totalTimes; ii++) {
			crudAPI.updateDocument(
				PlayLine.class, playLine.getId(), playLineMap);
		}

		double avgRuntime =
			1.0 * (System.currentTimeMillis() - startMSecs) / 1000;
		avgRuntime = avgRuntime / totalTimes;
		String operationName = "avg-updaeDocument-secs";
		AssertRuntime.runtimeHasNotChanged(
			avgRuntime, 0.20, operationName, testInfo);
	}
}
