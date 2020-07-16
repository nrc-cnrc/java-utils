package ca.nrc.debug;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.nrc.testing.outputcapture.CapturedPrintWriter;

public class Debug {
	
	public static boolean on = true;

	public static String printCallStack(Exception e) {
		return printCallStack(e, null);
	}
	
	public static String printCallStack() {
		return printCallStack(null, null);
	}
	
	public static String printCallStack(Exception e, long sleepMsecs) {
		return printCallStack(e, new Long(sleepMsecs));
	}

	public static String printCallStack(Exception e, Long sleepMsecs) {
		if (!on) return "Callstack printing is DISABLED!";
		StringBuilder output = new StringBuilder();
		if (e != null) {
			output.append(e.getMessage());
		}
		CapturedPrintWriter capture = null;
		String outString = "Could not print callstack.";
		try {
			capture = new CapturedPrintWriter(output);
			new Exception().printStackTrace(capture);
			outString = output.toString();
			
			// Reformat the captured output
			
			// Remove first line (which reports the exception)
			outString = outString.replace("java.lang.Exception", "");
			
			// Insert newlines between each part of the stack. Dunno why they weren't
			// inserted by Exception.printCallStack(writer)
			outString = outString.replaceAll(":(\\d+)\\)	at ", ":$1\\)\n	at ");
			
			// Remove parts of the stack that are versions of Debug.printCallStack
			outString = outString.replaceAll("\\s+at ca.nrc.debug.Debug.printCallStack[^\n]*(\n|$)", "");
			
			// For some reason, Exception.printCallStack(writer) duplicates every line
			String[] linesWithDups = outString.split("\n");
			String[] lines = new String[linesWithDups.length/2];
			for (int ii=0; ii < lines.length; ii++) {
				lines[ii] = linesWithDups[ii*2];
			}
			
			outString = String.join("\n", lines);
			
			// Sleep a bit to make sure call stack will be printed before anyting else (because
			// new Exception().printStackTrace() runs in a separate thread.
//			if (sleepMsecs != null) Thread.sleep(sleepMsecs);
		} catch (Exception e2) {
			e2.printStackTrace();
		}		
		
		return outString;
	}

}
