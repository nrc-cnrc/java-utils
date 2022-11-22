package ca.nrc.ui.commandline;

import ca.nrc.testing.AssertString;
import ca.nrc.testing.Asserter;
import ca.nrc.io.StdoutCapture;
import org.junit.jupiter.api.Assertions;
import static ca.nrc.ui.commandline.UserIO.Verbosity;

public class AssertUserIO extends Asserter<UserIO> {
	public AssertUserIO(UserIO _gotObject) {
		super(_gotObject);
	}

	public AssertUserIO(UserIO _gotObject, String mess) {
		super(_gotObject, mess);
	}

	UserIO io() {
		return this.gotObject;
	}

	public String doEcho(String mess, Integer indent, Verbosity level,
		Boolean newline) {
		if (indent != null) {
			io().echo(indent);
		}

		StdoutCapture.startCapturing();
		io().echo(mess, level, newline);
		String printed = StdoutCapture.stopCapturing();
		return printed;
	}

	public AssertUserIO echoProduces(String expPrint, String mess) {
		echoProduces(expPrint, mess, null, null, null);
		return this;
	}

	public AssertUserIO echoProduces(String expPrint, String mess,
		Integer indentChange) {
		echoProduces(expPrint, mess, indentChange, null, null);
		return this;
	}

	public AssertUserIO 	echoProduces(
		String expPrint, String mess, Integer indentChange, Verbosity level,
		Boolean newline) {

		String gotPrint = doEcho(mess, indentChange, level, newline);
		AssertString.assertStringEquals(
			baseMessage+"\nEcho did not produce the expected print result",
			"'"+expPrint+"\n'", "'"+gotPrint+"'");
		return this;
	}


	public AssertUserIO messageIsPrinted(
		String errMess, Verbosity level) {

		String randMessage = "Message at "+System.currentTimeMillis()+" msecs";
		String gotMess = doEcho(randMessage, (Integer)null, level, (Boolean)null);
		Assertions.assertFalse(
			gotMess.isEmpty(),
			baseMessage+
			"\nMessage SHOULD have been printed, but it was empty.\n"+errMess);

		return this;
	}

	public AssertUserIO messageNotPrinted(
		String errMess, Verbosity level) {

		String randMessage = "Message at "+System.currentTimeMillis()+" msecs";
		String gotMess = doEcho(randMessage, (Integer)null, level, (Boolean)null);
		Assertions.assertTrue(
			gotMess.isEmpty(),
			baseMessage+"\nNo message should have been printed, but the following WAS printed: '"+
					gotMess+"'.");

		return this;
	}
}
