package ca.nrc.testing;

public class Asserter<T> {
	protected String baseMessage = "";
	protected T gotObject = null;

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


}
