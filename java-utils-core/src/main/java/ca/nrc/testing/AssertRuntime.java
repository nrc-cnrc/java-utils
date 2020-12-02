package ca.nrc.testing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Assert that the time required to run a particular operation is
 * within expectations for the current machine.
 */
public class AssertRuntime {


	public static void runtimeHasNotChanged(double gotTime,
 		Double percTolerance, String ofWhat, TestInfo testInfo) throws IOException {
		runtimeHasNotChanged(gotTime, Pair.of(percTolerance,percTolerance),
			ofWhat, testInfo, (Boolean)null);
	}

	public static void runtimeHasNotChanged(double gotTime,
		Pair<Double,Double> percTolerances, String ofWhat, TestInfo testInfo)
		throws IOException {
		runtimeHasNotChanged(gotTime, percTolerances,
			ofWhat, testInfo, (Boolean)null);
	}

	public static void runtimeHasNotChanged(
		double gotTime, Pair<Double, Double> percTolerances,
		String ofWhat, TestInfo testInfo,
		Boolean highIsBad) throws IOException {

		runtimeHasNotChanged("", gotTime, percTolerances, ofWhat, testInfo,
			highIsBad);
	}

	public static void runtimeHasNotChanged(
		String mess,
		double gotTime, Pair<Double,Double> percTolerances,
		String ofWhat, TestInfo testInfo,
		Boolean highIsBad) throws IOException {
		if (highIsBad == null) {
			// By default, we assume that a high run time is a bad thing.
			highIsBad = true;
		}

		Double percToleranceWorsened = percTolerances.getLeft();
		Double percToleranceImpro = percTolerances.getRight();

		Double expTime = expTimeFor(ofWhat, testInfo);
		if (expTime == null) {
			// We didn't already have a time expectation for the current machine
			// Store the current time so we can use it as a point of comparison
			// for future runs of the test.
			//
			setExpTimeFor(gotTime, ofWhat, testInfo);
		} else {
			double toleranceWorsened = percToleranceWorsened * expTime;
			double toleranceImpr = percToleranceImpro * expTime;
			Pair<Double,Double> absTolerances =
				Pair.of(toleranceWorsened,toleranceImpr);
			String asPercentage = "%.1f";
			mess +=
				"\nRuntime for '"+ofWhat+
				"' has changed by more than the expected tolerances (impr. < "+
				String.format(asPercentage, toleranceImpr*100)+
				"%, worsening < " +
				String.format(asPercentage, toleranceWorsened*100)+"%).\n\n"+
				"This can happen intermittently if your computer was more/less busy than usual when you ran the test.\n"+
				"If, on the other hand, the new runtime is the \"new normal\", you "+
				"should change the expectation for that test by changing the '"+
				ofWhat+"' attribute in file:\n\n"+
				"   "+ benchmarksFile(testInfo)+"\n"
				;
		AssertNumber.performanceHasNotChanged(ofWhat, 1.0*gotTime,
			1.0*expTime, absTolerances, !highIsBad, mess);
		}
	}

	protected static void clearAllExpTimes(TestInfo testInfo) throws IOException {
		Files.delete(benchmarksFile(testInfo));
	}


	protected static void clearExpTimeFor(String ofWhat, TestInfo testInfo) throws IOException {
		setExpTimeFor(null, ofWhat, testInfo);
	}

	public static Path benchmarksFile(TestInfo testInfo) throws IOException {
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
		File expTimesFile = benchmarksFile(testInfo).toFile();
		if (expTimesFile.exists() && expTimesFile.length() > 0) {
			expTimes =
				new ObjectMapper().readValue(expTimesFile, expTimes.getClass());
		}
		return expTimes;
	}

	@JsonIgnore
	public static void setExpTimeFor(Double time, String ofWhat,
	 	TestInfo testInfo) throws IOException {
		Map<String,Double> expTimes = expTimesHash(testInfo);
		expTimes.put(ofWhat, time);
		new ObjectMapper()
			.writeValue(benchmarksFile(testInfo).toFile(), expTimes);
	}

	private static Double expTimeFor(String ofWhat, TestInfo testInfo)
		throws IOException {
		Map<String,Double> expTimes = expTimesHash(testInfo);
		Double expTime =  expTimes.get(ofWhat);
		return expTime;
	}
}
