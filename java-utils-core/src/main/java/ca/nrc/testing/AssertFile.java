package ca.nrc.testing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

public class AssertFile {
	
	public static void assertFileContains(
		String mess, File fPath, String pattern,
		Boolean isCaseSensitive, Boolean isRegexp) throws IOException {

		String fileContent = "";
		List<String> lines = Files.readAllLines(fPath.toPath());
		for (String line: lines) {
			fileContent += "\n"+line;
		}
		AssertString.assertStringContains(
			mess+"\nFile "+fPath+" did not contain the expected string",
			fileContent, pattern, isCaseSensitive, isRegexp);
	}

	public static void assertFileDoesNotContain(String mess, String fPath, String pattern, Boolean isRegexp) throws IOException {
		String fileContent = "";
		List<String> lines = Files.readAllLines(Paths.get(fPath));
		for (String line: lines) {
			fileContent += line;
		}
		AssertString.assertStringDoesNotContain(
			mess+"\nFile "+fPath+" contained an un-expected string", fileContent, pattern, null, isRegexp);
	}
	
	public static void assertFileContentEquals(String mess, File file, String expFileContent) throws IOException {
		String gotFileContent = new String(Files.readAllBytes(file.toPath()));
		AssertString.assertStringEquals(mess+"\nContent of file '"+file+"' was not as expected.",
				expFileContent, gotFileContent);
		
	}
	
	public static void assertFileContentStartsWith(String mess, File file, String expContentStart) throws IOException {
		mess += "Content of file "+file.toString()+" did not start with the expected string.";
		String gotFileContent = new String(Files.readAllBytes(file.toPath()));
		String gotContentStart = gotFileContent.substring(0, expContentStart.length());
		AssertString.assertStringEquals(mess, expContentStart, gotContentStart);
	}
	
	public static void assertFileContentEndsWith(String mess, File file, String expContentEnd) throws IOException {
		mess += "Content of file "+file.toString()+" did not end with the expected string.";
		String gotFileContent = new String(Files.readAllBytes(file.toPath()));
		String gotContentEnd = gotFileContent.substring(gotFileContent.length() - expContentEnd.length(), gotFileContent.length());
		AssertString.assertStringEquals(mess, expContentEnd, gotContentEnd);
	}
	
	public static void assertFilesHaveSameContent(String mess, File file1, File file2) throws IOException {
		mess += "\nFiles did not have the same content. File names are:\n  file1: "+file1+"\n  file2: "+file2;
		String content1 = new String(Files.readAllBytes(file1.toPath()));
		String content2 = new String(Files.readAllBytes(file2.toPath()));
		AssertString.assertStringEquals(mess, content1, content2);
	}
	
   public static void assertDirectoryHasNFiles(String mess, File dir, int expNum) throws IOException {        
        File[] gotFiles = dir.listFiles();
        Assertions.assertEquals(
		  		expNum, gotFiles.length,
        		mess+"\nDirectory "+dir+" did not contain the expected number of files");
    }

	
	public static void assertDirectoryHasFiles(String message, File dir, String[] expFiles) throws IOException {		
		File[] gotFiles = dir.listFiles();
		
		Set<String> gotFilesSet = new HashSet<String>();
		for (File aFile: gotFiles) gotFilesSet.add(aFile.getName());
		
		Set<String> expFilesSet = new HashSet<String>();
		for (String aFileName: expFiles) expFilesSet.add(aFileName);
		
		
		AssertObject.assertDeepEquals(message, expFilesSet, gotFilesSet);	
	}

	public static void assertFilesEqual(String mess, String[] expFilesStr, File[] gotFiles, File rootDir) throws Exception {
		List<String> gotFilesStr = new ArrayList<String>();
		int prefixLength = rootDir.toString().length();
		for (File aFile: gotFiles) {
			String aFileStr = aFile.toString().substring(prefixLength);
			aFileStr = aFileStr.replaceAll("^[\\/]", "");
			gotFilesStr.add(aFileStr);
		}
		AssertObject.assertDeepEquals(mess+"\nList of files not as expected", 
				expFilesStr, gotFilesStr);
	}

    public static void assertDirsHaveSameContent(File expDir, File gotDir) throws IOException {
        assertDirsHaveSameContent("", expDir, gotDir, null);
    }
    
    public static void assertDirsHaveSameContent(String mess, File expDir, File gotDir,
            String[] ignoreFiles) throws IOException {
        Set<String> ignoreSet = new HashSet<String>();
        if (ignoreFiles != null) {
            for (String aFile: ignoreFiles) {
                ignoreSet.add(aFile);
            }
        }
        
        List<File> expFiles = (List<File>) FileUtils.listFiles(expDir, null, true);
        Map<String,File> expFileNames = new HashMap<String,File>();
        for (File aFile: expFiles) {
            if (!ignoreSet.contains(aFile.getName())) {
                expFileNames.put(aFile.getName(), aFile);
            }
        }

        List<File> gotFiles = (List<File>) FileUtils.listFiles(gotDir, null, true);
        Map<String,File> gotFileNames = new HashMap<String,File>();
        for (File aFile: gotFiles) {
            if (!ignoreSet.contains(aFile.getName())) {
                gotFileNames.put(aFile.getName(), aFile);
            }
        }
        
        
        AssertObject.assertDeepEquals(
                mess+"\nThe two directories did not have the same files", 
                expFileNames.keySet(), gotFileNames.keySet());
        
        for (String aFileName: expFileNames.keySet()) {
            File anExpFile = expFileNames.get(aFileName);
            File aGotFile = gotFileNames.get(aFileName);
            AssertFile.assertFilesHaveSameContent("", anExpFile, aGotFile);
        }
    }
}
