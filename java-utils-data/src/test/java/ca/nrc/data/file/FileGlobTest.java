package ca.nrc.data.file;

import java.io.File;

import org.junit.Test;

import ca.nrc.file.ResourceGetter;
import ca.nrc.testing.AssertFile;
import ca.nrc.testing.AssertString;

public class FileGlobTest {
	
	/////////////////////////////////////////
	// DOCUMENTATION TESTS
	/////////////////////////////////////////
	
	@Test
	public void test__FileGlob__Synopsis() throws Exception {
		//
		// Use FileGlob to perform operations on files, using 
		// fileglob patterns.
		//
		
		// List files that match a pattern 
		// For example, files in a directory that have .txt extension
		//
		File testFilesDir = new File(ResourceGetter.getResourcePath("ca/nrc/javautils/data/testfiles/FileGlob_files"));
		String txtFilesPatt = new File(testFilesDir, "*.txt").toString();
		FileGlob.listFiles(txtFilesPatt);
		
		// You can also search using multiple patterns.
		// For example to find files that end with either
		// a txt or dat extension
		//
		String[] filesPatt = new String[] {
				new File(testFilesDir, "*.txt").toString(),
				new File(testFilesDir, "*.dat").toString()
		};
		FileGlob.listFiles(filesPatt);
		
		// You can delete files that match a particular pattern.
		// For example, delete all files that end with a .dat extension
		//
		File testFilesCopy = ResourceGetter.copyResourceToTempLocation("ca/nrc/javautils/data/testfiles/FileGlob_files");
		FileGlob.deleteFiles(new File(testFilesCopy, "*.dat").toString());
	}
	
	/////////////////////////////////////////
	// VERIFICATION TESTS
	/////////////////////////////////////////
	
	@Test
	public void test__listFiles__SinglePattern() throws Exception {
		File testFilesDir = new File(ResourceGetter.getResourcePath("ca/nrc/javautils/data/testfiles/FileGlob_files"));
		String txtFilesPatt = new File(testFilesDir, "*.txt").toString();
		File[] gotFiles = FileGlob.listFiles(txtFilesPatt);
		String[] expFiles = new String[] {
				"greetings.txt", "hello.txt"
		};
		AssertFile.assertFilesEqual("Listing files that end with .txt did not produce the expected result", 
				expFiles, gotFiles, testFilesDir);
	}

	@Test
	public void test__listFiles__MultiplePatterns() throws Exception {
		File testFilesDir = new File(ResourceGetter.getResourcePath("ca/nrc/javautils/data/testfiles/FileGlob_files"));
		
		String[] filesPatt = new String[] {
				new File(testFilesDir, "*.txt").toString(),
				new File(testFilesDir, "greeting*").toString()
		};
		
		File[] gotFiles = FileGlob.listFiles(filesPatt);
		String[] expFiles = new String[] {
				"hello.txt", "greetings.txt", "greetings.dat"
		};
		AssertFile.assertFilesEqual("Listing files that end with .txt did not produce the expected result", 
				expFiles, gotFiles, testFilesDir);
	}
	
	@Test
	public void test__deleteFiles__HappyPath() throws Exception {
		File testFilesCopy = ResourceGetter.copyResourceToTempLocation("ca/nrc/javautils/data/testfiles/FileGlob_files");
		
		File[] gotFiles = FileGlob.listFiles(new File(testFilesCopy, "*.*").toString());
		
		String[] expFiles = new String[] {
				"greetings.txt", "greetings.dat", "hello.dat", "hello.txt" 
		};
		AssertFile.assertFilesEqual("Original list of files not as expected", 
				expFiles, gotFiles, testFilesCopy);
		
		FileGlob.deleteFiles(new File(testFilesCopy, "*.txt").toString());
		
		
		gotFiles = FileGlob.listFiles(new File(testFilesCopy, "*.*").toString());

		expFiles = new String[] {
				"greetings.dat", "hello.dat" 
		};		
		AssertFile.assertFilesEqual("After deleting .txt files, list of files not as expected", 
				expFiles, gotFiles, testFilesCopy);
		
	}
	

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
		AssertString.assertStringEquals(new File(path1).getPath(), new File(path2).getPath());
	}
}
