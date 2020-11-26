package ca.nrc.testing;

import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This class allows you to create and manipulate a temporary directory
 * for a test.
 */
public class TestDirs {

	TestInfo testInfo = null;

	private static final Pattern pattTestMethodName =
	Pattern.compile("\\.(^\\.)+\\.(^\\.)+$");

	private static Set<Path> clearedDirs = new HashSet<Path>();

	public TestDirs(TestInfo __testInfo) {
		this.testInfo = __testInfo;
	}

	public Path basePath() throws IOException {
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

	public Path inputsPath() throws IOException {
		Path base = basePath();
		Path inputs = Paths.get(base.toString(), "inputs");
		ensureDirExists(inputs);
		ensureWasCleared(inputs);
		return inputs;
	}

	public Path outputsPath() throws IOException {
		Path base = basePath();
		Path outputs = Paths.get(base.toString(), "outputs");
		ensureDirExists(outputs);
		ensureWasCleared(outputs);
		return outputs;
	}

	public Path persistentResourcesPath() throws IOException {
		return persistentResourcesPath((File)null);
	}

	public Path persistentResourcesPath(File relFile) throws IOException {
		Path base = basePath();
		Path resources = Paths.get(base.toString(), "persistent_resources");
		ensureDirExists(resources);
		if (relFile != null) {
			resources = Paths.get(resources.toString(), relFile.toString());
		}
		return resources;
	}

	private void ensureDirExists(Path dirPath) {
		File dir = dirPath.toFile();
		dir.mkdirs();
		return;
	}


	private void ensureWasCleared(Path dir) {
		if (! clearedDirs.contains(dir)) {
			final File[] files = dir.toFile().listFiles();
			for (File aFile: files) {
				aFile.delete();
			}
			clearedDirs.add(dir);
		}
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
