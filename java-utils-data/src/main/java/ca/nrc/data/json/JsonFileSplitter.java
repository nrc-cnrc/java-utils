package ca.nrc.data.json;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class for splitting large JSON data files into smaller chunks.
 * This is particularly useful if you want to put the JSON file under version
 * control using a system like SVN or Git. Most of those systems do not deal
 * well with very large files.
 */
public class JsonFileSplitter {
	Path jsonFile = null;

	public JsonFileSplitter(Path _jsonFile) {
		init_JsonFileSplitter(_jsonFile);
	}

	private void init_JsonFileSplitter(Path _jsonFile) {
		this.jsonFile = _jsonFile;
	}

	public void split() throws JsonFileSplitterException {
		ensureSplitDirExists();
	}

	private void ensureSplitDirExists() throws JsonFileSplitterException {
		Path dir = splitDir();
		if (Files.exists(dir)) {
			if (!Files.isDirectory(dir)) {
				throw new JsonFileSplitterException(
					"Path of the split directory exists but it is a file (not a directory)");
			}
		} else {
			try {
				Files.createDirectories(dir);
			} catch (IOException e) {
				throw new JsonFileSplitterException("Could not create split directory "+dir);
			}
		}
	}

	public void syncFile() {
	}

	public Path splitDir() {
		Path parentDir = jsonFile.getParent();
		String baseName = FilenameUtils.getBaseName(jsonFile.toString());
		Path splitPath = Paths.get(parentDir.toString(), baseName+".split");
		return splitPath;
	}
}
