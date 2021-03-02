package ca.nrc.ui.commandline;

public class CommandLineException extends Exception {
	public CommandLineException(String message) {
		super(message);
	}

	public CommandLineException(Exception e) {
		super(e);
	}

	public CommandLineException(String message, Exception e) {
		super(message, e);
	}
}
