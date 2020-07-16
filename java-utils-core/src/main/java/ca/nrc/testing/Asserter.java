package ca.nrc.testing;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;

public class Asserter<T> {
	protected String baseMessage = "";
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
		Assert.assertTrue(
			baseMessage+"Should not have been null", 
			gotObject != null);
		return this;
	}

	public Asserter<T> isNull() {
		Assert.assertTrue(
			baseMessage+"SHOULD have been null, but was: "+gotObject, 
			gotObject == null);
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
