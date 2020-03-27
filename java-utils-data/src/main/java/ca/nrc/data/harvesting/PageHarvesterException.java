package ca.nrc.data.harvesting;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PageHarvesterException extends Exception {
	
//	private String errorMessage = "";
	
	public PageHarvesterException(Exception exc) {
		super(exc);
	}
	
	public PageHarvesterException(Exception exc, String message) {
		super(exc);
//		this.errorMessage = message;
	}
	
	
	public PageHarvesterException(String mess, Exception e) {
		super(mess, e);
//		this.errorMessage = mess;
	}

	public PageHarvesterException(String mess) {
		super(mess);
//		this.errorMessage = mess;
	}

//	public String getMessage() {
//		String message = errorMessage + "\n\n" + stackTrace();
//		return message;
//	}
//	
//	private String stackTrace() {
//		StringWriter sw = new StringWriter();
//		new Throwable("").printStackTrace(new PrintWriter(sw));
//		String stackTrace = sw.toString();
//
//		return stackTrace;
//	}
}
