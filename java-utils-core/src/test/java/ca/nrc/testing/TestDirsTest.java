package ca.nrc.testing;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;

public class TestDirsTest {

	@Test
	public void test__TestDirs__Synopsis(TestInfo testInfo) throws Exception {
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
		Path inputsPath = testDirs.inputsDir();

		// This a temporary directory where the test can output
		// some files.
		//
		// The directory is guaranteed to be empty upon the
		// start of the test.
		//
		Path outputsPath = testDirs.outputsDir();

		// This is a directory where you can put "persistent" resources, i.e.
		// files that will not be cleared by the test harness..
		//
		// This can be useful to store things like the speed at which a test
		// is expected to run on the current machine (those values obviously
		// cannot be coded in the test, because speed will vary from machine
		// to machine)
		//
		Path resourcesPath = testDirs.persistentResourcesDir();

		// The inputsDir(), outputsDir() and persistentResourcesDir() methods
		// allow you to provide a path that is relative to the base directory
		//
		// In all those cases, the subdirectories will be created if they
		// don't already exist.
		//
		Path inputSubdir =
			testDirs.inputsDir("some", "input", "subdir");
		Path outputSubdir =
			testDirs.outputsDir("some", "output", "subdir");
		Path persistentSubdir =
			testDirs.persistentResourcesDir("some", "persistent", "subdir");

		// You can also get path to specific files.
		// The path leading to the file will be created if it does not already
		// exist, but the file itself will NOT be created.
		//
		Path outputFile =
			testDirs.outputsFile("some", "path", "hello.txt");

		// You can copy a resources file or directory to the inputs dir
		testDirs.copyResourceFileToInputs("test_data/ca/nrc/resource_getter_files/hello.txt");
		testDirs.copyResourceDirToInputs("test_data/ca/nrc/resource_getter_files/some_resource_dir");

		return;
	}

	//////////////////////////////////////////////
	// VERIFICATION TESTS
	//////////////////////////////////////////////

	@Test
	public void test__baseDir__HappyPath(TestInfo testInfo) throws Exception {
		Path dir = new TestDirs(testInfo).baseDir();
		new AssertPath(dir)
			.isDir()
			.endsWith("target/test-dirs/ca/nrc/testing/TestDirsTest/test__baseDir__HappyPath")
			;
		return;
	}

	@Test
	public void test__outputsDir__HappyPath(TestInfo testInfo) throws IOException {
		Path dir = new TestDirs(testInfo).outputsDir("output", "subdir");
		new AssertPath(dir)
			.isDir()
			.endsWith("target/test-dirs/ca/nrc/testing/TestDirsTest/test__outputsDir__HappyPath/outputs/output/subdir")
		;
	}

	@Test
	public void test__outputsFile__HappyPath(TestInfo testInfo) throws IOException {
		Path dir = new TestDirs(testInfo).outputsFile("subdir", "file.txt");
		new AssertPath(dir)
			.isFile()
			.endsWith("target/test-dirs/ca/nrc/testing/TestDirsTest/test__outputsFile__HappyPath/outputs/subdir/file.txt")
		;
	}

	@Test
	public void test__inputsDir__HappyPath(TestInfo testInfo) throws IOException {
		Path dir = new TestDirs(testInfo).inputsDir("input", "subdir");
		new AssertPath(dir)
			.isDir()
			.endsWith("target/test-dirs/ca/nrc/testing/TestDirsTest/test__inputsDir__HappyPath/inputs/input/subdir")
		;
	}

	@Test
	public void test__inputsFile__HappyPath(TestInfo testInfo) throws IOException {
		Path dir = new TestDirs(testInfo).inputsFile("subdir", "file.txt");
		new AssertPath(dir)
		.isFile()
		.endsWith("target/test-dirs/ca/nrc/testing/TestDirsTest/test__inputsFile__HappyPath/inputs/subdir/file.txt")
		;
	}


	@Test
	public void test__persistentResourcesDir__HappyPath(TestInfo testInfo) throws IOException {
		Path dir = new TestDirs(testInfo).persistentResourcesDir("persistent", "subdir");
		new AssertPath(dir)
			.isDir()
			.endsWith("target/test-dirs/ca/nrc/testing/TestDirsTest/test__persistentResourcesDir__HappyPath/persistent_resources/persistent/subdir")
		;
	}

}
