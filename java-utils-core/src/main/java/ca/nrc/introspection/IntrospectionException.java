package ca.nrc.introspection;

public class IntrospectionException extends Exception {

	public IntrospectionException(String mess, Exception e) {
		super(mess, e);
	}

	public IntrospectionException(String mess) {
		super(mess);
	}

	public IntrospectionException(Exception e) {
		super(e);
	}
	
}
