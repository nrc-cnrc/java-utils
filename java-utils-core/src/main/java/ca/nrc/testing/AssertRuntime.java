package ca.nrc.testing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Assert that the time required to run a particular operation is
 * within expectations for the current machine.
 */
public class AssertRuntime {


	public static void assertRuntimeHasNotChanged(double gotTime,
		Double percTolerance, String ofWhat, TestInfo testInfo) throws IOException {
		assertRuntimeHasNotChanged(gotTime, percTolerance, ofWhat, testInfo,
			(Boolean)null);
	}

	public static void assertRuntimeHasNotChanged(
		double gotTime, Double percTolerance,
		String ofWhat, TestInfo testInfo,
		Boolean highIsBad) throws IOException {
		if (highIsBad == null) {
			// By default, we assume that a high run time is a bad thing.
			highIsBad = true;
		}

		Double expTime = expTimeFor(ofWhat, testInfo);
		if (expTime == null) {
			// We didn't already have a time expectation for the current machine
			// Store the current time so we can use it as a point of comparison
			// for future runs of the test.
			//
			setExpTimeFor(gotTime, ofWhat, testInfo);
		} else {
			double tolerance = percTolerance * gotTime;
			String mess =
				"Runtime for '"+ofWhat+"' has changed by more than "+
				String.format("%.0f",percTolerance*100)+"%.\n\n"+
				"This can happen intermittently if your computer was more/less busy than usual when you ran the test.\n"+
				"If, on the other hand, the new runtime is the \"new normal\", you "+
				"should change the expectation for that test by changing the '"+
				ofWhat+"' attribute in file:\n\n"+
				"   "+expTimesFPath(testInfo)+"\n"
				;
		AssertNumber.performanceHasNotChanged(ofWhat, 1.0*gotTime,
			1.0*expTime, tolerance, !highIsBad, mess);
		}
	}

	protected static void clearExpTimeFor(String ofWhat, TestInfo testInfo) throws IOException {
		setExpTimeFor(null, ofWhat, testInfo);
	}

	private static Path expTimesFPath(TestInfo testInfo) throws IOException {
		Path fPath =
			new TestDirs(testInfo)
			.persistentResourcesFile("expectedRuntimes.json");
		File file = fPath.toFile();
		if (!file.exists()) {
			file.createNewFile();
		}
		return fPath;
	}

	private static Map<String,Double> expTimesHash(TestInfo testInfo) throws IOException {
		Map<String,Double> expTimes = new HashMap<String,Double>();
		File expTimesFile = expTimesFPath(testInfo).toFile();
		if (expTimesFile.exists() && expTimesFile.length() > 0) {
			expTimes =
				new ObjectMapper().readValue(expTimesFile, expTimes.getClass());
		}
		return expTimes;
	}

	@JsonIgnore
	private static void setExpTimeFor(Double time, String ofWhat,
	 	TestInfo testInfo) throws IOException {
		Map<String,Double> expTimes = expTimesHash(testInfo);
		expTimes.put(ofWhat, time);
		new ObjectMapper()
			.writeValue(expTimesFPath(testInfo).toFile(), expTimes);
	}

	private static Double expTimeFor(String ofWhat, TestInfo testInfo)
		throws IOException {
		Map<String,Double> expTimes = expTimesHash(testInfo);
		Double expTime =  expTimes.get(ofWhat);
		return expTime;
	}
}
