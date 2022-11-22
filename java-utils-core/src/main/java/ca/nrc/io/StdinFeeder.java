package ca.nrc.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StdinFeeder {

	public static InputStream stdinBeforeFeeding = null;
	private static boolean currentlyFeeding = false;
	private static ByteArrayInputStream stringInputStream = null;

	public static synchronized void feedString(String inputsString) {
		if (!currentlyFeeding) {
			stdinBeforeFeeding = System.in;
		}
		currentlyFeeding = true;
		stringInputStream = new ByteArrayInputStream(inputsString.getBytes());
		System.setIn(stringInputStream);
	}

	public static synchronized void stopFeeding() throws IOException {
		if (currentlyFeeding) {
			currentlyFeeding = false;
			System.setIn(stdinBeforeFeeding);
			stdinBeforeFeeding = null;
		}
	}
}
