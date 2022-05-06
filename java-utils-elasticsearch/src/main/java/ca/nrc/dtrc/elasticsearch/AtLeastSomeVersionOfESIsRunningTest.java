package ca.nrc.dtrc.elasticsearch;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class AtLeastSomeVersionOfESIsRunningTest {
	@Test
	public void test__ensureAtLeastSomeVersionOfESIsRunning() throws ElasticSearchException, ca.nrc.dtrc.elasticsearch.ElasticSearchException {
		int[][] possibleVersions = new int [][] {
			new int[] {5, 9200},
			new int[] {7, 9200}
		};
		boolean ok = false;
		for (int[] version: possibleVersions) {
			ok = new ESVersionChecker("localhost", version[1])
				.isRunningVersion(version[0]);
			if (ok) {
				break;
			}
		}
		Assertions.assertTrue(ok,
			"No version of ElasticSearch server is running.\n"+
			"If you want to test the ElasticSearch utilities, make sure some version of ESFactory server is running");

	}
}
