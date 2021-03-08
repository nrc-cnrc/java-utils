package ca.nrc.ui.commandline;

import ca.nrc.testing.AssertString;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;

public class RunWithStdinInputsTest {

	////////////////////////////////
	// DOCUMENTATION TEST
	////////////////////////////////

	@Test
	public void test__RunWithStdinInputs__Synopsis() {
		// Say you have you want to test a method call, and that
		// call involves reading something from STDIN. How do you run
		// that test without requiring a human to enter something in STDIN?
		// This is what RunWithStdinInputs is designed for...
		//
		// First you define a Supplier Lambda that makes the method call
		// For example, this lambda just reads a line from STDIN and returns it.
		//
		Supplier<String> lambda = () -> {
			String readFromStdin = null;
			BufferedReader reader = new BufferedReader(
			new InputStreamReader(System.in));
			try {
				readFromStdin = reader.readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return readFromStdin;
		};

		// You can then run the lambda with a String that will be put on STDIN
		// Note that the string will remain on STDIN only for the duration of the
		// lambda call.
		//
		String stdinInputs = "Hello world\n";
		String lambdaResult =
			RunWithStdinInputs.run(lambda, stdinInputs);

		// The run() method returns the value returned by the lambda.
		// In this case, it should just be the line that we put on the STDIN
		AssertString.assertStringEquals("Hello world", lambdaResult);
	}
}
