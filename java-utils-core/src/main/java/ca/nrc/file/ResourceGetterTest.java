package ca.nrc.file;

import ca.nrc.testing.AssertFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourceGetterTest {

	@Test
	public void test__copyResourceToDir__HappyPath() throws Exception {
		Path targDir = Files.createTempDirectory("test");
		File destFile = ResourceGetter.copyResourceToDir(
			"test_data/ca/nrc/resource_getter_files/hello.txt", targDir);

		AssertFile.assertFileContentEquals("", destFile, "Hello world");
	}
}
