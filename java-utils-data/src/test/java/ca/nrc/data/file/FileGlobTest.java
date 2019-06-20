package ca.nrc.data.file;

import java.io.File;

import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class FileGlobTest {
	
	@Test
	public void test__getStartingDir__JustWildcardFilePattern() {
		String pattern = "*.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "./";
		assertSamePath(expStartingDir, gotStartingDir);
	}

	@Test
	public void test__getStartingDir__PatternStartsWithSingleDot() {
		String pattern = "./*.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "./";
		assertSamePath(expStartingDir, gotStartingDir);
	}

	@Test
	public void test__getStartingDir__PatternStartsWithSubdirOfCurrentDir() {
		String pattern = "subdir/*.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "./subdir/";
		assertSamePath(expStartingDir, gotStartingDir);
	}
	
	@Test
	public void test__getStartingDir__PatternStartsWithDoubleDots() {
		String pattern = "../subdir/*.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "../subdir/";
		assertSamePath(expStartingDir, gotStartingDir);
	}

	@Test
	public void test__getStartingDir__PatternHasNoWildcardAndStartsWithFileName() {
		String pattern = "foo.txt";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "./foo.txt";
		assertSamePath(expStartingDir, gotStartingDir);
	}
	
	@Test
	public void test__getStartingDir__WildcardIsNotAtTheStartOfADirectory() {		
		String pattern = "/some/path/x*/to/somewhere";
		String gotStartingDir = FileGlob.getStartingDir(pattern);
		String expStartingDir = "/some/path";
		assertSamePath(expStartingDir, gotStartingDir);
	}
	
	/**
	 * Compare two file paths
	 * Recognizes both / and \\ as equivalent Windows file separators
	 * @param path1
	 * @param path2
	 */
	private void assertSamePath(String path1, String path2) {
		AssertHelpers.assertStringEquals(new File(path1).getPath(), new File(path2).getPath());
	}
}
