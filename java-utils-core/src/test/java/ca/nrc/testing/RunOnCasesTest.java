package ca.nrc.testing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import ca.nrc.testing.RunOnCases.*;
import org.opentest4j.AssertionFailedError;

public class RunOnCasesTest {

	////////////////////////////////////////
	// DOCUMENTATION TESTS
	////////////////////////////////////////

	@Test
	public void test__RunOnCases__Synopsis() throws Exception {
		// Use this class to run a test on a series of cases.

		// First you must define the cases...
		//
		Case[] cases = new Case[] {
			new Case(
				// First argument is a human readable description
				"2x3",
				// Remaining arguments is the data for the case.
				// It is a list of object of any type, and they can
				// be of any type. However, the data for different cases
				// must be of the same types and lenghth.
				2,3,6),
			new Case("4x7", 4, 7, 28),
			new Case("3x9", 3, 9, 27),
		};

		// Next, you define a case runner that will be used to
		// run the test on one case
		Consumer<Case> runner =
			(aCase) -> {
				Integer num1 = (Integer)(aCase.data[0]);
				Integer num2 = (Integer)(aCase.data[1]);
				Integer expRes = (Integer)(aCase.data[2]);
				Assertions.assertEquals(
					expRes, num1*num2,
					aCase.descr);
			};

		// You feed the cases and the runner to the constructor
		RunOnCases tests = new RunOnCases(cases, runner);

		// Finally, you run the cases
		tests.run();

		// You can filter the cases that will be run
		{
			// For example, this will only run the first and third tests
			// Note: For the purpose of this test, we wrap the run() call
			//   inside an assertThrows() statement.
			// This is because asking the runner to skip some cases will raise
			// a failure that is meant to reminder the developer to eventually
			// re-enable all cases. The failure is raised only if all non-skipped
			// tests have passed.
			Assertions.assertThrows(SkippedCasesException.class, () -> {
				tests
					.onlyCaseNums(1, 3)
					.run();
			});

			Assertions.assertThrows(SkippedCasesException.class, () -> {
				tests
				.onlyCaseNums(1, 3)
				.run();
			});

			// This will only run all the tests except the second
			Assertions.assertThrows(SkippedCasesException.class, () -> {
				tests
					.allButCaseNums(2)
					.run();
			});

			// This will only run tests whose descriptors match the
			// regexp. In this case, it will match any test that the product
			// of numbers 3 or 5 with numbers 7 or 9
			Assertions.assertThrows(SkippedCasesException.class, () -> {
				tests
				.onlyCasesWithDescr("[35]x[79]")
				.run();
			});

			// If you expect a particular case to result in a "null" result, then
			// you can specify that as follows
			Case aCase = new Case("Case with null result", "data1", "data2").setExpectsNull(true);
		}
	}

	////////////////////////////////////////
	// VERIFICATION TESTS
	////////////////////////////////////////

	@Test
	public void test__run__SomeTestsIncludingOneThaFails() throws Exception {

		Case[] cases = new Case[] {
			new Case("2x3", 2,3,6),
			// This one fails
			new Case("4x7", 4, 7, -28),
			new Case("3x9", 3, 9, 27),
		};

		Consumer<Case> runner =
			(aCase) -> {
				Integer num1 = (Integer)(aCase.data[0]);
				Integer num2 = (Integer)(aCase.data[1]);
				Integer expRes = (Integer)(aCase.data[2]);
				Assertions.assertEquals(
					expRes, num1*num2,
					aCase.descr);
			};

		Assertions.assertThrows(AssertionFailedError.class, () -> {
			new RunOnCases(cases, runner).run();
		});
	}

	@Test
	public void test__run__TestsThatRaiseException() throws Exception {

		Case[] cases = new Case[] {
			new Case("4/4", 4,2,2),
			// This raises a division by zero exception
			new Case("1/0", 1, 0, null),
		};

		Consumer<Case> runner =
			(aCase) -> {
				Integer num1 = (Integer)(aCase.data[0]);
				Integer num2 = (Integer)(aCase.data[1]);
				Integer expRes = (Integer)(aCase.data[2]);
				Assertions.assertEquals(
					expRes, num1/num2,
					aCase.descr);
			};

		Assertions.assertThrows(ArithmeticException.class, () -> {
			new RunOnCases(cases, runner).run();
		});
	}

	@Test
	public void test__run__SkippingSomeTestsRaisesWarning() throws Exception {
		Case[] cases = new Case[] {
			new Case("2x3", 2,3,6),
			new Case("4x7", 4, 7, 28),
			new Case("3x9", 3, 9, 27),
		};

		Consumer<Case> runner =
			(aCase) -> {
				Integer num1 = (Integer)(aCase.data[0]);
				Integer num2 = (Integer)(aCase.data[1]);
				Integer expRes = (Integer)(aCase.data[2]);
				Assertions.assertEquals(
					expRes, num1*num2,
					aCase.descr);
			};

		RunOnCases tests = new RunOnCases(cases, runner);

		Assertions.assertThrows(SkippedCasesException.class, () -> {
			tests.onlyCaseNums(1, 3).run();
		});

		Assertions.assertThrows(SkippedCasesException.class, () -> {
			tests.allButCaseNums(2).run();
		});

		Assertions.assertThrows(SkippedCasesException.class, () -> {
			tests.onlyCasesWithDescr("[35]x[79]").run();
		});
	}
}