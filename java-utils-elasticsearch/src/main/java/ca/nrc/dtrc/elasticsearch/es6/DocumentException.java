package ca.nrc.dtrc.elasticsearch.es6;

public class DocumentException extends Exception {
	public DocumentException(Exception exc) {
		super(exc);
	}
	public DocumentException(String message) {
		super(message);
	}
	public DocumentException(String message, Exception e) {
		super(message, e);
	}
}
