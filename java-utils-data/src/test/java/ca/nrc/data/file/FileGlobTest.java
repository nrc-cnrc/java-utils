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
	
//	@Test
//	public void test__getStartingDir__DELETE_LATER() {
//		String pattern = "Job_postings_NOC 2011 Uncoded - 2015-02-05 to 2018-02-05/*lines*.dsv";
//		String gotStartingDir = FileGlob.getStartingDir(pattern);
//		String expStartingDir = "BLAH";
//		AssertHelpers.assertStringEquals(expStartingDir, gotStartingDir);
//	}	
}
