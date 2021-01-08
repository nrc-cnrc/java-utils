package ca.nrc.web;

public class HttpException extends Exception {
	public HttpException(String mess, Exception e) {
		super(mess, e);
	}
	public HttpException(String mess) {
		super(mess);
	}
	public HttpException(Exception e) {
		super(e);
	}
}
