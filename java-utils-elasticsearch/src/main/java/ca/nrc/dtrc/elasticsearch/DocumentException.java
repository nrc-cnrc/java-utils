package ca.nrc.dtrc.elasticsearch;

public class DocumentException extends Exception {
	public DocumentException(Exception exc) {
		super(exc);
	}
	public DocumentException(String message) {
		super(message);
	}
}
