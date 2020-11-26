package ca.nrc.testing;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;

public class TestDirsTest {

	@Test
	public void test__TestDirs__Synopsis(TestInfo testInfo) throws IOException {
		// Use this class to create and manipulate directories that can
		// be used for this test
		//
		// Note that TestDirs only works with JUnit5 tests, that receive a
		//   TestInfo argument
		//
		TestDirs testDirs = new TestDirs(testInfo);

		// This a temporary directory where you can put some
		// inputs files that the test requires.
		//
		// The directory is guaranteed to be empty upon the
		// start of the test.
		//
		Path inputsPath = testDirs.inputsPath();

		// This a temporary directory where the test can output
		// some files.
		//
		// The directory is guaranteed to be empty upon the
		// start of the test.
		//
		Path outputsPath = testDirs.outputsPath();

		// This is a directory where you can put "persistent" resources, i.e.
		// files that will not be cleared by the test harness..
		//
		// This can be useful to store things like the speed at which a test
		// is expected to run on the current machine (those values obviously
		// cannot be coded in the test, because speed will vary from machine
		// to machine)
		//
		Path resourcesPath = testDirs.persistentResourcesPath();

	}

	//////////////////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////////////////

	@Test
	public void test__path__HappyPath(TestInfo testInfo) throws Exception {
		Path dir = new TestDirs(testInfo).basePath();
		AssertString.assertStringEndsWith(
		"TestDirsTest/test__path__HappyPath", dir.toString());
		return;
	}

}
