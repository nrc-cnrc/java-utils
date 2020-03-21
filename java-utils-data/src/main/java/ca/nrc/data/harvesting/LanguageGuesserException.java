package ca.nrc.data.harvesting;

import java.io.IOException;

public class LanguageGuesserException extends Exception {

	public LanguageGuesserException(String mess, Exception e) {
		super(mess, e);
	}

	public LanguageGuesserException(String mess) {
		super(mess);
	}
	
	public LanguageGuesserException(Exception e) {
		super(e);
	}
}
