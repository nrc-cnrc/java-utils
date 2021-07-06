package ca.nrc.testing;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.*;

public class Asserter<T> {
	public String baseMessage = "";
	protected T gotObject = null;
	protected Set<String> ignoreFields = new HashSet<String>();

	public Asserter(T _gotObject) {
		initializeAsserter(_gotObject, null);
	}
	
	public Asserter(T _gotObject, String mess) {
		initializeAsserter(_gotObject, mess);
	}

	private void initializeAsserter(T _gotObject, String mess) {
		if (mess != null) {
			this.baseMessage = mess;
		}
		this.gotObject = _gotObject;
	}

	public Asserter<T> isNotNull() {
		Assertions.assertTrue(
			gotObject != null,
			baseMessage+"Should not have been null");
		return this;
	}

	public Asserter<T> isNull() {
		Assertions.assertTrue(
			gotObject == null,
			baseMessage+"SHOULD have been null, but was: "+gotObject);
		return this;
	}
	
	public void assertEqual(T expObject) throws IOException {
		String[] ignoreFieldsArr = new String[ignoreFields.size()];
		int pos = 0;
		for (String field: ignoreFields) {
			ignoreFieldsArr[pos] = field;
			pos++;
		}
		AssertObject.assertDeepEquals(
			baseMessage+"\nThe two objects differed.", 
			expObject, gotObject, ignoreFieldsArr);
	}
}
