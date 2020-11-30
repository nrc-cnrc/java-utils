package ca.nrc.testing;

import org.junit.jupiter.api.Assertions;

import java.nio.file.Files;
import java.nio.file.Path;

public class AssertPath extends Asserter<Path>{
	public AssertPath(Path _gotPath) {
		super(_gotPath, (String)null);
	}
	public AssertPath(Path _gotPath, String _mess) {
		super(_gotPath, _mess);
	}

	public AssertPath isDir() {
		String mess = baseMessage + "\nFile was not a directory: "+path();
		Assertions.assertTrue(Files.isDirectory(path()), mess);
		return this;
	}

	public AssertPath isFile() {
		String mess = baseMessage + "\nFile was a directory: "+path();
		Assertions.assertFalse(Files.isDirectory(path()), mess);
		return this;
	}


	public AssertPath endsWith(String suffix) {
		AssertString.assertStringEndsWith(suffix, path().toString());
		return this;
	}

	protected Path path() {
		return gotObject;
	}
}
