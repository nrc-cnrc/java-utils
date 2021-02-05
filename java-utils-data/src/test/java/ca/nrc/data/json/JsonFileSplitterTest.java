package ca.nrc.data.json;

import ca.nrc.file.ResourceGetter;
import ca.nrc.testing.TestDirs;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileSplitterTest {

	TestDirs testDirs = null;
	Path jsonFile = null;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		testDirs = new TestDirs(testInfo);
		ResourceGetter.copyResourceFilesToDir("testfiles/ca/nrc/json", testDirs.inputsDir());
		jsonFile = testDirs.inputsDir("people.json");
	}

	/////////////////////////
	// DOCUMENTATION TESTS
	/////////////////////////

	@Test
	public void test__JsonFileSplitter__Synopsis(TestInfo testInfo) throws Exception {
		// Say you have a large JSON file that you want to split into smaller
		// chunks
		//
		Path jsonFile = testDirs.inputsDir("people.json");

		// First you create a JsonFileSplitter and use it to split the file
		JsonFileSplitter splitter = new JsonFileSplitter(jsonFile);
		splitter.split();

		// Whenever you need to read the original json file, you should invoke
		// syncFile(), in case some changes were made to the split
		// version of the file and the merged file needs to be regenerated from
		// it.
		//
		// This scenario can happen if you commit the split version to Git (or
		// some other version control system) instead of the original, and later
		// on, some changes to the split version are fetched from Git.
		//
		//
		splitter.syncFile();
	}

	/////////////////////////
	// VERIFICATION TESTS
	/////////////////////////

	@Test @Ignore
	public void test__split__HappyPath() throws Exception {
		JsonFileSplitter splitter = new JsonFileSplitter(jsonFile);
		AssertJsonFileSplitter asserter = new AssertJsonFileSplitter(splitter);
		asserter.splitDirDoesNotExist(
			"The split dir should not have existed before splitting");
		splitter.split();
		asserter
			.splitDirExists(
				"The split dir SHOULD have existed after splitting");

	}

	@Test
	public void test__splitDir__HappyPath() throws Exception {
		Path someJsonFile = Paths.get("/some/path/someFile.json");
		JsonFileSplitter splitter = new JsonFileSplitter(someJsonFile);
		new AssertJsonFileSplitter(splitter)
			.splitDirIs("/some/path/someFile.split");
	}

}
