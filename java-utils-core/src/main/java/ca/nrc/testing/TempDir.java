package ca.nrc.testing;

import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * This class allows you to create and manipulate a temporary directory
 * for a test.
 */
public class TempDir {

	TestInfo testInfo = null;

	private static final Pattern pattTestMethodName =
	Pattern.compile("\\.(^\\.)+\\.(^\\.)+$");

	public TempDir(TestInfo __testInfo) {
		this.testInfo = __testInfo;
	}

	public Path path() throws IOException {
		Method method = testInfo.getTestMethod().get();

		String methName = method.getName();
		String className = method.getDeclaringClass().getName();

		File targetDir = targetDir();

		File testClassDir = new File(targetDir, className);
		if (!testClassDir.exists()) {
			testClassDir.mkdirs();
		}
		File testMethodDir = new File(testClassDir, methName);
		if (!testMethodDir.exists()) {
			testMethodDir.mkdirs();
		}

		return testMethodDir.toPath();
	}

	private File targetDir() {
		File targetClassesDir =
			new File(
				this.getClass().getProtectionDomain().getCodeSource()
				.getLocation().getPath());
		File dir = targetClassesDir.getParentFile();

		return dir;
	}

}
