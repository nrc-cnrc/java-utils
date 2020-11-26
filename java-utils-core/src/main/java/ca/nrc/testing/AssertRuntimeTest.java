package ca.nrc.testing;

import org.junit.jupiter.api.*;

public class AssertRuntimeTest {

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
		AssertRuntime.assertRuntimeHasNotChanged(
			gotTime, 0.01, ofWhat, testInfo);

		// If the runtime has increased more than the tolerance,
		// the assertion should fail.
		//
		Assertions.assertThrows(
			AssertionError.class, () -> {
				AssertRuntime.assertRuntimeHasNotChanged(
					2000, 0.01, ofWhat, testInfo);
		});

		// If the runtime has DECREASED more than the tolerance,
		// the assertion should fail.
		//
		Assertions.assertThrows(
			AssertionError.class, () -> {
				AssertRuntime.assertRuntimeHasNotChanged(
				100, 0.01, ofWhat, testInfo);
			});

		// Assertion should succeed if the new runtime is withing the
		// tolerance.
		//
		AssertRuntime.assertRuntimeHasNotChanged(
		1000+2, 0.01, ofWhat, testInfo);
		AssertRuntime.assertRuntimeHasNotChanged(
		1000-2, 0.01, ofWhat, testInfo);

		return;
	}
}
