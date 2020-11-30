package ca.nrc.testing;

import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

	public Path baseDir() throws IOException {
		Method method = testInfo.getTestMethod().get();

		String methName = method.getName();
		String className = method.getDeclaringClass().getName();
		String[] classElts = className.split("\\.");

		Path targetDir = targetDir();
		Path allTestsDir = Paths.get(targetDir.toString(), "test-dirs");
		Path testClassDir =
			Paths.get(allTestsDir.toString(), classElts);
		if (!Files.exists(testClassDir)) {
			Files.createDirectories(testClassDir);
		}
		Path testMethodDir = Paths.get(testClassDir.toString(), methName);
		if (!Files.exists(testMethodDir)) {
			Files.createDirectories(testMethodDir);
		}

		return testMethodDir;
	}

	public Path inputsDir() throws IOException {
		return inputsDir(new String[0]);
	}

	public Path inputsDir(String... relPath) throws IOException {
		Path base = baseDir();
		Path inputs = Paths.get(base.toString(), "inputs");
		ensureDirExists(inputs);
		ensureWasCleared(inputs);

		Path inSubdir = inputs;
		for (String aRelPathElt: relPath) {
			inSubdir = Paths.get(inSubdir.toString(), aRelPathElt);
		}

		ensureDirExists(inSubdir);

		return inSubdir;
	}

	public Path inputsFile(String... relPath) throws IOException {
		Path dir = inputsDir(Arrays.copyOfRange(relPath, 0, relPath.length-1));
		Path file = Paths.get(dir.toString(), relPath[relPath.length-1]);

		return file;
	}

	public Path outputsDir() throws IOException {
		return outputsDir(new String[0]);
	}

	public Path outputsDir(String... relPath) throws IOException {
		Path base = baseDir();
		Path outputs = Paths.get(base.toString(), "outputs");
		ensureDirExists(outputs);
		ensureWasCleared(outputs);

		Path outSubdir = outputs;
		for (String aRelPathElt: relPath) {
			outSubdir = Paths.get(outSubdir.toString(), aRelPathElt);
		}

		ensureDirExists(outSubdir);

		return outSubdir;
	}

	public Path outputsFile(String... relPath) throws IOException {
		Path dir = outputsDir(Arrays.copyOfRange(relPath, 0, relPath.length-1));
		Path file = Paths.get(dir.toString(), relPath[relPath.length-1]);

		return file;
	}

	public Path persistentResourcesDir() throws IOException {
		return persistentResourcesDir(new String[0]);
	}

	public Path persistentResourcesDir(String... relPath) throws IOException {
		Path base = baseDir();
		Path resources = Paths.get(base.toString(), "persistent_resources");
		ensureDirExists(resources);

		Path resourcesSubdir = resources;
		for (String aRelPathElt: relPath) {
			resourcesSubdir = Paths.get(resourcesSubdir.toString(), aRelPathElt);
		}

		ensureDirExists(resourcesSubdir);

		return resourcesSubdir;
	}

	public Path persistentResourcesFile(String... relPath) throws IOException {
		Path dir = persistentResourcesDir(Arrays.copyOfRange(relPath, 0, relPath.length-1));
		Path file = Paths.get(dir.toString(), relPath[relPath.length-1]);

		return file;
	}


	private void ensureDirExists(Path dirPath) throws IOException {
		Files.createDirectories(dirPath);
		return;
	}


	private synchronized void ensureWasCleared(Path dir) throws IOException {
		// Note: This method is 'synchronized' in case we run tests in parallel.
		// The method requires access to a static member 'clearedDirs'.
		//
		final File[] files = dir.toFile().listFiles();
		for (File aFile: files) {
			aFile.delete();
		}
		clearedDirs.add(dir);
	}

	private Path targetDir() {

		Class<?> testClass = testInfo.getTestClass().get();

		Path targetClassDir =
			Paths.get(
			testClass.getProtectionDomain().getCodeSource()
			.getLocation().getPath());
		Path dir = targetClassDir.getParent();

		return dir;
	}
}
