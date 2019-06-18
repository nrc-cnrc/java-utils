package ca.nrc.data.file;

import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class FileGlobTest {
	
	@Test
	public void test__getStartingDir__JustWildcardFilePattern() {
		String pattern = "*.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "./";
		AssertHelpers.assertStringEquals(expStartingDir, gotStartingDir);
	}

	@Test
	public void test__getStartingDir__PatternStartsWithSingleDot() {
		String pattern = "./*.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "./";
		AssertHelpers.assertStringEquals(expStartingDir, gotStartingDir);
	}

	@Test
	public void test__getStartingDir__PatternStartsWithSubdirOfCurrentDir() {
		String pattern = "subdir/*.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "./subdir/";
		AssertHelpers.assertStringEquals(expStartingDir, gotStartingDir);
	}
	
	@Test
	public void test__getStartingDir__PatternStartsWithDoubleDots() {
		String pattern = "../subdir/*.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "../subdir/";
		AssertHelpers.assertStringEquals(expStartingDir, gotStartingDir);
	}

	@Test
	public void test__getStartingDir__PatternHasNoWildcardAndStartsWithFileName() {
		String pattern = "foo.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "./foo.txt";
		AssertHelpers.assertStringEquals(expStartingDir, gotStartingDir);
	}
	
	@Test
	public void test__getStartingDir__WildcardIsNotAtTheStartOfADirectory() {
		String pattern = "/some/path/x*/to/somewhere";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "/some/path";
		AssertHelpers.assertStringEquals(expStartingDir, gotStartingDir);
	}
}
