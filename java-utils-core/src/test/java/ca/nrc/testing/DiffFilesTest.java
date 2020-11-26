package ca.nrc.testing;

import org.junit.jupiter.api.*;

import ca.nrc.file.ResourceGetter;

public class DiffFilesTest {
	
	/********************************
	 * DOCUMENTATION TESTS
	 ********************************/
	
	@Test
	public void test__DiffFiles__Synopsis() throws Exception {
		// Use DiffFiles to invoke the Unix diff (or windows equivalent) on 
		// a pair of files or directories.
		//
		// You can use it to do a diff of two files...
		//
		DiffFiles diff = new DiffFiles();
		String file1 = ResourceGetter.getResourcePath("test_data/ca/nrc/testing/diff/file1.txt");
		String file2 = ResourceGetter.getResourcePath("test_data/ca/nrc/testing/diff/file2.txt");
		String diffoutput = diff.diff(file1, file2);
		
		// You can also do a diff of two directories.
		// By default, this will do a recursive diff.
		String dir1 = ResourceGetter.getResourcePath("test_data/ca/nrc/testing/diff/dir1/");
		String dir2 = ResourceGetter.getResourcePath("test_data/ca/nrc/testing/diff/dir2/");
		diffoutput = diff.diff(dir1, dir2, DiffFiles.DIRECTORIES);
		
		// If you want the dir diff to be non-recursive, here is how you do it.
		diffoutput = diff.diff(dir1, dir2, DiffFiles.DIRECTORIES, DiffFiles.NON_RECURSIVE);
		
		
	}
	
}
