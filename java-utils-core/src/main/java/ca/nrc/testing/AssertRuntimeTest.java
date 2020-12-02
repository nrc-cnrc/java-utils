package ca.nrc.testing;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;

public class AssertRuntimeTest {

	private TestInfo testInfo;

	@BeforeEach
	public void setUp(TestInfo info) throws IOException {
		this.testInfo = info;
		AssertRuntime.clearAllExpTimes(testInfo);
	}

	//////////////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////////////

	@Test
	public void test__AssertRuntime__Synopsis() throws IOException {
		// Use this class to check that a the runtime of a particular
		// operation has not changed significantly compared with a
		// previous generated baseline.
		//
		// In the example below, we assert that the previously saved benchmark
		// for operation 'some operation' is withing 1% of the current runtime
		// of 3.0.
		//
		String operationName = "some operation";
		Double currentRuntime = 3.0;
		Double percTolerance = 0.01;
		AssertRuntime.runtimeHasNotChanged(
			currentRuntime, 0.01, operationName, testInfo);

		// The first time you run an runtime assertion for a given operation in
		// a given test, the assertion will ALWAYS succeed. That's because at that
		// point, there is no previously saved benchmark.
		//
		// The system will automatically save the current runtime as the benchmark
		// for this operation in this test.
		//
		// The benchmarks for a given test are provided by the following file:
		//
		Path benchmarksFile = AssertRuntime.benchmarksFile(testInfo);

		// Afterwards, if you run assert the runtime for the same operation in
		// the same test method, you will experience a failure if the new runtime
		// is significantly different from the one that was previously saved.
		//
		Assertions.assertThrows(AssertionError.class, () -> {
			Double muchLowerRuntime = currentRuntime - 1.0;
			String opname = "some operation";
			AssertRuntime.runtimeHasNotChanged(
				muchLowerRuntime, 0.01, opname, testInfo);
		});

		// You can provide a different tolerance for improvements
		// versus worsenings of the rutime.
		//
		// For example, here we are more tolerant of improvements
		// than we are of worsenings.
		//
		Double toleranceWorsening = 0.01;
		Double toleranceImprov = 0.05;
		Pair<Double,Double> tolerances = Pair.of(toleranceWorsening, toleranceImprov);
		AssertRuntime.runtimeHasNotChanged(
			currentRuntime, tolerances, operationName, testInfo);

		// By default, runtimeHasNotChanged() assumes that a high value of
		// is bad and a low value is good. But you can override that.
		//
		boolean highIsBad = false;
		operationName = "time to failure";
		AssertRuntime.runtimeHasNotChanged(
			currentRuntime, tolerances, operationName, testInfo, highIsBad);
	}

	//////////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////////

	@Test
	public void test__AssertRuntime__NoExpectedTimeToStartWith(TestInfo testInfo)
		throws Exception {
		String ofWhat = "some method";
		Long gotTime = new Long(1000);

		TestDirs testDirs = new TestDirs(testInfo);
		AssertRuntime.clearExpTimeFor(ofWhat, testInfo);

		// First time we check run time, it shoud be fine because
		// no time expectations have been logged for that operation yet.
		//
		AssertRuntime.runtimeHasNotChanged(
			gotTime, 0.01, ofWhat, testInfo);

		// If the runtime has increased more than the tolerance,
		// the assertion should fail.
		//
		Assertions.assertThrows(
			AssertionError.class, () -> {
				AssertRuntime.runtimeHasNotChanged(
					2000, 0.01, ofWhat, testInfo);
		});

		// If the runtime has DECREASED more than the tolerance,
		// the assertion should fail.
		//
		Assertions.assertThrows(
			AssertionError.class, () -> {
				AssertRuntime.runtimeHasNotChanged(
				100, 0.01, ofWhat, testInfo);
			});

		// Assertion should succeed if the new runtime is withing the
		// tolerance.
		//
		AssertRuntime.runtimeHasNotChanged(
			1000+2, 0.01, ofWhat, testInfo);
		AssertRuntime.runtimeHasNotChanged(
			1000-2, 0.01, ofWhat, testInfo);

		return;
	}


	@Test
	public void test__runtimeHasNotChanged__SingleTolerance() throws Exception {

		Double currentRuntime;
		Double baselineRuntime;
		Double percTolerance;

		currentRuntime = 32.0;
		baselineRuntime = currentRuntime;
		percTolerance = 0.1;

		doRuntimeHasNotChanged(
			"Should NOT fail because current is EQUAL to baseline",
			currentRuntime, baselineRuntime, percTolerance);

		currentRuntime = 31.0;
		baselineRuntime = 31.2;
		percTolerance = 0.1;
		doRuntimeHasNotChanged(
			"Should NOT fail because IMPROVEMENT is NOT significant",
			currentRuntime, baselineRuntime, percTolerance);

		currentRuntime = 32.0;
		baselineRuntime = 31.2;
		percTolerance = 0.1;
		doRuntimeHasNotChanged(
			"Should NOT fail because WORSENING is NOT significant",
			currentRuntime, baselineRuntime, percTolerance);

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 30.0;
			Double basePerformance = 31.2;
			Double tol = 0.01;
			doRuntimeHasNotChanged(
				"SHOULD fail because of significant IMPROVEMENT",
				currPerformance, basePerformance, tol);
		});

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 34.0;
			Double basePerformance = 31.2;
			Double tol = 0.01;
			doRuntimeHasNotChanged(
				"Should fail because of significant WORSENING",
				currPerformance, basePerformance, tol);
		});
	}

	@Test
	public void test__performanceHasNotChanged__ImprAndWorseTolerances()
		throws Exception {
		Double currentPerformance;
		Double baselinePerformance;
		Pair<Double,Double> tolerances;

		currentPerformance = 32.0;
		baselinePerformance = currentPerformance;
		tolerances = Pair.of(0.05, 0.05);
		doRuntimeHasNotChanged(
			"Should NOT fail because current is EQUAL to baseline",
			currentPerformance, baselinePerformance, tolerances);

		currentPerformance = 31.1;
		baselinePerformance = 31.2;
		tolerances = Pair.of(0.05, 0.05);
		doRuntimeHasNotChanged(
			"Should NOT fail because IMPROVEMENT is NOT significant",
			currentPerformance, baselinePerformance, tolerances);

		currentPerformance = 31.0;
		baselinePerformance = 31.2;
		tolerances = Pair.of(0.05, 0.05);
		doRuntimeHasNotChanged(
			"Should NOT fail because WORSENING is NOT significant",
			currentPerformance, baselinePerformance, tolerances);

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 29.0;
			Double basePerformance = 31.2;
			Pair<Double,Double> tol = Pair.of(0.05, 0.05);
			doRuntimeHasNotChanged(
				"SHOULD fail because of significant IMPROVEMENT",
				currPerformance, basePerformance, tol);
		});

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 34.0;
			Double basePerformance = 31.2;
			Pair<Double,Double> tol = Pair.of(0.05,0.05);
			doRuntimeHasNotChanged(
				"Should fail because of significant WORSENING",
				currPerformance, basePerformance, tol);
		});
	}

	@Test
	public void test__performanceHasNotChanged__HighIsBad() throws Exception {
		Double currentPerformance;
		Double baselinePerformance;
		Pair<Double,Double> tolerances;

		currentPerformance = 32.0;
		baselinePerformance = currentPerformance;
		tolerances = Pair.of(0.05, 0.05);
		doRuntimeHasNotChanged(
			"Should NOT fail because current is EQUAL to baseline",
			currentPerformance, baselinePerformance, tolerances, false);

		currentPerformance = 31.2;
		baselinePerformance = 32.0;
		tolerances = Pair.of(0.05, 0.05);
		doRuntimeHasNotChanged(
			"Should NOT fail because IMPROVEMENT is NOT significant",
			currentPerformance, baselinePerformance, tolerances, false);

		currentPerformance = 31.2;
		baselinePerformance = 31.0;
		tolerances = Pair.of(0.05, 0.05);
		doRuntimeHasNotChanged(
			"Should NOT fail because WORSENING is NOT significant",
			currentPerformance, baselinePerformance, tolerances, false);

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 29.0;
			Double basePerformance = 32.0;
			Pair<Double,Double> tol = Pair.of(0.05,0.05);
			doRuntimeHasNotChanged(
				"SHOULD fail because of significant IMPROVEMENT",
				currPerformance, basePerformance, tol, false);
		});

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 33.2;
			Double basePerformance = 30.0;
			Pair<Double,Double> tol = Pair.of(0.05,0.05);
			doRuntimeHasNotChanged(
				"Should fail because of significant WORSENING",
				currPerformance, basePerformance, tol, false);
		});
	}

	@Test
	public void test__performanceHasNotChanged__NullImprTolerance()
		throws Exception {
		Pair<Double,Double> tolerances = Pair.of(null, 0.05);
		doRuntimeHasNotChanged(
		"The fact that we are passing a null impr. tolerance should not cause a crash",
		10.0, 10.001, tolerances);
	}

	@Test
	public void test__performanceHasNotChanged__NullWorsenedTolerance()
		throws Exception {
		Pair<Double,Double> tolerances = Pair.of(0.05, null);
		doRuntimeHasNotChanged(
			"The fact that we are passing a null impr. tolerance should not cause a crash",
			10.0, 10.001, tolerances);
	}


	//////////////////////////////////////
	// TEST HELPERS
	//////////////////////////////////////


	private void doRuntimeHasNotChanged(
			String mess, Double currentRuntime, Double baselineRuntime,
			Double percTolerance) throws Exception {
		doRuntimeHasNotChanged(mess, currentRuntime, baselineRuntime,
			Pair.of(percTolerance,percTolerance), "some operation",
			(Boolean)null);
	}

	private void doRuntimeHasNotChanged(
		String mess, Double currentRuntime, Double baselineRuntime,
		Pair<Double,Double> percTolerances) throws Exception {
		doRuntimeHasNotChanged(
			mess, currentRuntime, baselineRuntime, percTolerances,
		"some operation", (Boolean)null);
	}

	private void doRuntimeHasNotChanged(
		String mess,
		Double currRuntime, Double baselineRuntime, Double percTolerance,
		String operationName, Boolean highIsBad) throws Exception {
		doRuntimeHasNotChanged(mess, currRuntime, baselineRuntime,
			Pair.of(percTolerance, percTolerance), operationName, highIsBad);
	}

	private void doRuntimeHasNotChanged(
		String mess,
		Double currRuntime, Double baselineRuntime, Pair<Double,Double> percTolerances,
		Boolean highIsBad) throws Exception {

		doRuntimeHasNotChanged(mess, currRuntime, baselineRuntime,
			percTolerances, "some operation", highIsBad);
	}

	private void doRuntimeHasNotChanged(
		String mess,
		Double currRuntime, Double baselineRuntime, Pair<Double,Double> percTolerances,
		String operationName, Boolean highIsBad) throws Exception {

		AssertRuntime.setExpTimeFor(baselineRuntime, operationName, testInfo);
		AssertRuntime.runtimeHasNotChanged(
			currRuntime, percTolerances, operationName, testInfo, highIsBad);
	}
}
