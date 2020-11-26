package ca.nrc.testing;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class TempDirTest {

	@Test
	public void test__TempDir__Synopsis(TestInfo testInfo) throws IOException {
		// Use this class to create and manipulate a temporary directory
		// for a given test
		//
		// Note that TempDir only works with JUnit5 tests, that receive a
		//   TestInfo argument
		//
		TempDir tempDir = new TempDir(testInfo);

		// Here is how you get the path of th tempDir
		Path tempDirPath = tempDir.path();
	}

	//////////////////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////////////////

	@Test
	public void test__path__HappyPath(TestInfo testInfo) throws Exception {
		Path dir = new TempDir(testInfo).path();
		AssertString.assertStringEndsWith(
		"TempDirTest/test__path__HappyPath", dir.toString());
		return;
	}

}
