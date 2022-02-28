package ca.nrc.data.bin;

import ca.nrc.debug.ExceptionHelpers;

public class BadJsonInputException extends Exception {
	String badInput = null;
	Exception exc = null;
	public BadJsonInputException(Exception exc, String input) {
		super();
		this.badInput = input;
	}

	@Override
	public String getMessage() {
		String errMess =
			"Input was not a valid JSON object.\n"+
			"Input was: "+badInput+"\n"
			;

		return errMess;
	}

	@Override
	public synchronized Throwable getCause() {
		return exc;
	}
}
