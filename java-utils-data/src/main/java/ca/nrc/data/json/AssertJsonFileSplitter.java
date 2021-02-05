package ca.nrc.data.json;

import ca.nrc.testing.AssertString;
import ca.nrc.testing.Asserter;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.json.Json;

import java.nio.file.Path;

public class AssertJsonFileSplitter extends Asserter<JsonFileSplitter> {
	public AssertJsonFileSplitter(JsonFileSplitter _gotObject) {
		super(_gotObject);
	}

	public AssertJsonFileSplitter(JsonFileSplitter _gotObject, String mess) {
		super(_gotObject, mess);
	}

	private JsonFileSplitter splitter() {
		return this.gotObject;
	}

	public AssertJsonFileSplitter splitDirDoesNotExist(String mess) {
		Assertions.fail("Implement this assertion");
		return this;
	}

	public AssertJsonFileSplitter splitDirExists(String mess) {
		Assertions.fail("Implement this assertion");
		return this;
	}

	public AssertJsonFileSplitter splitDirIs(String expSplitDir) {
		Path gotSplitDir = splitter().splitDir();
		AssertString.assertStringEquals(
			baseMessage+"Split dir path was not as expected",
			expSplitDir, gotSplitDir.toString());
		return this;
	}
}
