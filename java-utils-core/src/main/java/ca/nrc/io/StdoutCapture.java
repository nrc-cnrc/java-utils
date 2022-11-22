package ca.nrc.io;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class StdoutCapture {
	
	private static PrintStream oldStdout = null;
	static ByteArrayOutputStream baos = null;
	static PrintStream ps = null;
	
	public static void startCapturing() {
		if (oldStdout == null) {
			 oldStdout = System.out;
			 baos = new ByteArrayOutputStream();
			 ps = new PrintStream(baos);
			 System.setOut(ps);
		}
	}
	
	public static String stopCapturing() {
		String output = "";
		if (oldStdout != null) {
			System.out.flush();
			System.setOut(oldStdout);
			oldStdout = null;
			output = baos.toString();
		}
		
		return output;
	}
}
