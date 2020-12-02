package ca.nrc.testing;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

public class AssertNumberTest {

	//////////////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////////////

	@Test
	public void test__AssertNumber__Synopsis() {
		// Use this class to check that a number is within a particular
		// range of values
		//
		AssertNumber.isGreaterOrEqualTo(2, 1);
		AssertNumber.isLessOrEqualTo(1.0, 2.0);

		// You can mix different types of numbers...
		AssertNumber.isGreaterOrEqualTo(2.0, new Long(1));
		AssertNumber.isLessOrEqualTo(1.0, new Long(2));

		// When a number corresponds to a particular performance metric, you can
		// compare it to a baseline value and check that it hasn't changed
		// significantly.
		//
		String metricName = "avg precision";
		Double currentPerformance = 0.95;
		Double baselinePerformance = 0.92;
		Double tolerance = 0.05;
		AssertNumber.performanceHasNotChanged(
			metricName, currentPerformance,
			baselinePerformance, tolerance);

		// You can provide a different tolerance for improvements
		// versus worsenings of the metric.
		//
		// For example, here we are more tolerant of improvements
		// than we are of worsenings.
		//
		Double toleranceWorsening = 0.01;
		Double toleranceImprov = 0.05;
		Pair<Double,Double> tolerances = Pair.of(toleranceWorsening, toleranceImprov);
		AssertNumber.performanceHasNotChanged(
			metricName, currentPerformance,
			baselinePerformance, tolerances);

		// By default, performanceHasNotChanged() assumes that a high value of
		// is good and a low value is bad. But you can override that.
		//
		boolean highIsGood = false;
		metricName = "error rate";
		currentPerformance = 0.12;
		baselinePerformance = 0.13;
		AssertNumber.performanceHasNotChanged(
			metricName, currentPerformance,
			baselinePerformance, tolerances, highIsGood);

	}

	//////////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////////

	@Test
	public void test__performanceHasNotChanged__SingleTolerance() {
		Double currentPerformance;
		Double baselinePerformance;
		Double tolerance;

		currentPerformance = 32.0;
		baselinePerformance = currentPerformance;
		tolerance = 0.1;
		doPerformanceHasNotChanged(
			"Should NOT fail because current is EQUAL to baseline",
			currentPerformance, baselinePerformance, tolerance);

		currentPerformance = 32.0;
		baselinePerformance = 31.2;
		tolerance = 1.0;
		doPerformanceHasNotChanged(
			"Should NOT fail because IMPROVEMENT is NOT significant",
			currentPerformance, baselinePerformance, tolerance);

		currentPerformance = 31.0;
		baselinePerformance = 31.2;
		tolerance = 1.0;
		doPerformanceHasNotChanged(
			"Should NOT fail because WORSENING is NOT significant",
			currentPerformance, baselinePerformance, tolerance);

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 32.0;
			Double basePerformance = 31.2;
			Double tol = 0.01;
			doPerformanceHasNotChanged(
				"SHOULD fail because of significant IMPROVEMENT",
				currPerformance, basePerformance, tol);
		});

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 30.0;
			Double basePerformance = 31.2;
			Double tol = 0.01;
			doPerformanceHasNotChanged(
				"Should fail because of significant WORSENING",
				currPerformance, basePerformance, tol);
		});
	}

	@Test
	public void test__performanceHasNotChanged__ImprAndWorseTolerances() {
		Double currentPerformance;
		Double baselinePerformance;
		Pair<Double,Double> tolerances;

		currentPerformance = 32.0;
		baselinePerformance = currentPerformance;
		tolerances = Pair.of(0.1, 0.1);
		doPerformanceHasNotChanged(
			"Should NOT fail because current is EQUAL to baseline",
			currentPerformance, baselinePerformance, tolerances);

		currentPerformance = 32.0;
		baselinePerformance = 31.2;
		tolerances = Pair.of(0.1, 1.0);
		doPerformanceHasNotChanged(
			"Should NOT fail because IMPROVEMENT is NOT significant",
			currentPerformance, baselinePerformance, tolerances);

		currentPerformance = 31.0;
		baselinePerformance = 31.2;
		tolerances = Pair.of(1.0, 0.1);
		doPerformanceHasNotChanged(
			"Should NOT fail because WORSENING is NOT significant",
			currentPerformance, baselinePerformance, tolerances);

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 32.0;
			Double basePerformance = 31.2;
			Pair<Double,Double> tol = Pair.of(2.0,0.01);
			doPerformanceHasNotChanged(
				"SHOULD fail because of significant IMPROVEMENT",
				currPerformance, basePerformance, tol);
		});

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 30.0;
			Double basePerformance = 31.2;
			Pair<Double,Double> tol = Pair.of(0.01,2.0);
			doPerformanceHasNotChanged(
				"Should fail because of significant WORSENING",
				currPerformance, basePerformance, tol);
		});
	}

	@Test
	public void test__performanceHasNotChanged__HighIsBad() {
		Double currentPerformance;
		Double baselinePerformance;
		Pair<Double,Double> tolerances;

		currentPerformance = 32.0;
		baselinePerformance = currentPerformance;
		tolerances = Pair.of(0.1, 0.1);
		doPerformanceHasNotChanged(
			"Should NOT fail because current is EQUAL to baseline",
			currentPerformance, baselinePerformance, tolerances, false);

		currentPerformance = 31.2;
		baselinePerformance = 32.0;
		tolerances = Pair.of(0.1, 1.0);
		doPerformanceHasNotChanged(
			"Should NOT fail because IMPROVEMENT is NOT significant",
			currentPerformance, baselinePerformance, tolerances, false);

		currentPerformance = 31.2;
		baselinePerformance = 31.0;
		tolerances = Pair.of(1.0, 0.1);
		doPerformanceHasNotChanged(
			"Should NOT fail because WORSENING is NOT significant",
			currentPerformance, baselinePerformance, tolerances, false);

		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 31.2;
			Double basePerformance = 32.0;
			Pair<Double,Double> tol = Pair.of(2.0,0.01);
			doPerformanceHasNotChanged(
				"SHOULD fail because of significant IMPROVEMENT",
				currPerformance, basePerformance, tol, false);
		});

		// ICI
		Assertions.assertThrows(AssertionError.class, () -> {
			Double currPerformance = 31.2;
			Double basePerformance = 30.0;
			Pair<Double,Double> tol = Pair.of(0.01,2.0);
			doPerformanceHasNotChanged(
				"Should fail because of significant WORSENING",
				currPerformance, basePerformance, tol, false);
		});
	}

	@Test
	public void test__performanceHasNotChanged__NullImprTolerance() {
		Pair<Double,Double> tolerances = Pair.of(null, 0.05);
		doPerformanceHasNotChanged(
		"The fact that we are passing a null impr. tolerance should not cause a crash",
		10.0, 10.001, tolerances);
	}

	@Test
	public void test__performanceHasNotChanged__NullWorsenedTolerance() {
		Pair<Double,Double> tolerances = Pair.of(0.05, null);
		doPerformanceHasNotChanged(
		"The fact that we are passing a null impr. tolerance should not cause a crash",
		10.0, 10.001, tolerances);
	}

	//////////////////////////////////////
	// TEST HELPERS
	//////////////////////////////////////

	private void doPerformanceHasNotChanged(
		Double currentPerformance, Double baselinePerformance,
		Double tolerance) {
		doPerformanceHasNotChanged("", currentPerformance, baselinePerformance,
			Pair.of(tolerance,tolerance), (Boolean)null);

	}

	private void doPerformanceHasNotChanged(
		String mess, Double currentPerformance,
		Double baselinePerformance, Double tolerance) {

		doPerformanceHasNotChanged(mess, currentPerformance, baselinePerformance,
			Pair.of(tolerance,tolerance), (Boolean)null);
	}

	private void doPerformanceHasNotChanged(String mess,
		Double currentPerformance, Double baselinePerformance,
 		Pair<Double,Double> tolerances) {

		doPerformanceHasNotChanged(mess, currentPerformance, baselinePerformance,
			tolerances, (Boolean)null);

	}

	private void doPerformanceHasNotChanged(String mess,
		Double currentPerformance, Double baselinePerformance,
 		Pair<Double,Double> tolerances, Boolean highIsGood) {
		AssertNumber.performanceHasNotChanged(
			"some metric", currentPerformance, baselinePerformance,
			tolerances, highIsGood, mess);
	}
}
