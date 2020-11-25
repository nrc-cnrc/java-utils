package ca.nrc.debug;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.junit.jupiter.api.*;

import ca.nrc.testing.AssertHelpers;

public class ExceptionHelpersTest {

	@Test
	public void test__whatFileWasNotFound__HappyPath() {
		String inexistantFilePath = "/inexistandDir/inexistantFile";
		try {
			new FileReader(new File(inexistantFilePath));
		} catch (FileNotFoundException e) {
			String gotMissingFilePath = ExceptionHelpers.whatFileWasNotFound(e);
			AssertHelpers.assertStringEquals(gotMissingFilePath, inexistantFilePath);
		}
	}

}
