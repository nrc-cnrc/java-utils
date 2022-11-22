package ca.nrc.io;

import ca.nrc.io.StdoutCapture;
import org.junit.jupiter.api.*;

import ca.nrc.testing.AssertHelpers;

public class StdoutCaptureTest {

	/*********************************
	 * DOCUMENTATION TESTS
	 *********************************/
	
	@Test
	public void test__StdoutCapture__Synopsis() {
		//
		// Use StdoutCapture to temporarily capture System.out to a string
		//
		StdoutCapture.startCapturing();
		
		// These will not be printed to Stdout and instead will be stored 
		// in a string.
		//
		System.out.println("Hello World.");
		System.out.println("Take me to your leader.");
		
		// When you stop capturing Stdout, you get a string with everything
		// that was printed to it while you were capturing
		//
		String output = StdoutCapture.stopCapturing();
	}

	/*********************************
	 * VERIFICATION TESTS
	 *********************************/
	
	@Test
	public void test__StdoutCapture__HappyPath() {
		StdoutCapture.startCapturing();
		System.out.println("Hello world");
		System.out.println("Take me to your leader");
		String gotOutput = StdoutCapture.stopCapturing();
		String expOutput = "Hello world\nTake me to your leader\n";
		AssertHelpers.assertStringEquals(expOutput, gotOutput);
	}

	@Test
	public void test__StdoutCapture__StopCapturingWhileNotCapturingInTheFirstPlace__ShouldNotCrash() {
		String gotOutput = StdoutCapture.stopCapturing();
		String expOutput = "";
		AssertHelpers.assertStringEquals(expOutput, gotOutput);
	}

	@Test
	public void test__StdoutCapture__StartCapturingTwiceInARowWithoutStop__ShouldNotCrash() {
		StdoutCapture.startCapturing();
		StdoutCapture.startCapturing();
		System.out.println("Hello world");
		String gotOutput = StdoutCapture.stopCapturing();
		String expOutput = "Hello world\n";
		AssertHelpers.assertStringEquals(expOutput, gotOutput);
	}
}
