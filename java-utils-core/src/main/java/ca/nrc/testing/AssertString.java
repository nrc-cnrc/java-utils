package ca.nrc.testing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.*;

public class AssertString {
	
	public static void assertStringEquals(String message, String expString, String gotString) {
		if (expString == null && gotString == null) {
			return;
		}

		message = message +
				"The two strings differred. Location of the first difference is highlighted below with tag <FIRST_DIFF>.\n";
		
		// Ignore differences in \n versus \r\n
		//  TODO: This should probably be an option
		if (expString != null) {
			expString = expString.replaceAll("\r\n", "\n");
		}
		if (gotString != null) {
			gotString = gotString.replaceAll("\r\n", "\n");
		}
		
		
		int firstDiffOffset = StringUtils.indexOfDifference(expString, gotString);
		
		if (expString == null || gotString == null) {
			Assertions.assertEquals(message, expString, gotString);
		}

		if (firstDiffOffset >= 0) {
			String commonStart = expString.substring(0, firstDiffOffset);
			String expectedRest = expString.substring(firstDiffOffset);
			String gotRest = gotString.substring(firstDiffOffset);

			message = 
					message + 
					"== Expected:\n "+
					commonStart +
					"<<FIRST_DIFF>>" +
					expectedRest + "\n";
			message = 
					message + 
					"== Got         :\n "+
					commonStart +
					"<<FIRST_DIFF>>" +
					gotRest + "\n";
			
			Assertions.fail(message);
		}
	}

	public static void assertStringEquals(String expString, String gotString) {
		assertStringEquals("", expString, gotString);
	}

	public static void assertStringContains(String gotString, String expSubstring) {
		assertStringContains(null, gotString, expSubstring, null, null);
	}

	public static void assertStringContains(String message,
			String gotString, String expSubstring) {
		boolean caseSensitive = true;
		assertStringContains(message, gotString, expSubstring, null, null);
	}

	public static void assertStringContains(String message,
			String gotString, String pattern, Boolean caseSensitive) {
		
		assertStringContains(message, gotString, pattern, caseSensitive, null);
	}
	
	public static void assertStringContains(String message, String gotString, 
			String pattern, Boolean caseSensitive, Boolean isRegexp) {
		
		if (caseSensitive == null) {
			caseSensitive = true;
		}
		
		if (isRegexp == null) {
			isRegexp = false;
		}
		
		if (!caseSensitive && !isRegexp) {
			gotString = gotString.toLowerCase();
			pattern = pattern.toLowerCase();
		}
		
		if (message == null) {
			message = "";
		} else {
			message = message + "\n";
		}
		
		String type = "substring";
		if (isRegexp) {type = "regexp";}
		
		message = message + 
				   "String did not contain an expected "+type+".\n"
						  + "== Expected "+type+": \n"+pattern+"\n\n"
						  + "== Got string : \n"+gotString+"\n\n";

		if (isRegexp) {
			Pattern patt = Pattern.compile(pattern);
			Matcher matcher = patt.matcher(gotString);
			Assertions.assertTrue(matcher.find(),
			message+"\nDid not find any occurence of regepx "+pattern);
		} else {
			Assertions.assertTrue(gotString.contains(pattern),
			message+"\nDid not find any occurence of regepx "+pattern);
		}
	}	
	
	
	public static void assertStringDoesNotContain(String gotString, String unexpSubstring) {
		boolean caseSensitive = true;
		assertStringDoesNotContain("", gotString, unexpSubstring, caseSensitive);
	}
	
	public static void assertStringDoesNotContain(String message,
			String gotString, String unexpSubstring) {
		boolean caseSensitive = true;
		assertStringDoesNotContain(message, gotString, unexpSubstring, caseSensitive);
	}

	public static void assertStringDoesNotContain(String message, String gotString, 
			String unexpSubstring, Boolean caseSensitive) {
		assertStringDoesNotContain(message, gotString, unexpSubstring, caseSensitive, null);
	}

	
	public static void assertStringDoesNotContain(String message, String gotString, 
			String unexpSubstring, Boolean caseSensitive, Boolean isRegexp) {
		
		if (caseSensitive == null) {
			caseSensitive = false;
		}
		
		if (isRegexp == null) {
			isRegexp = false;
		}
		
		if (!caseSensitive && !isRegexp) {
			gotString = gotString.toLowerCase();
			unexpSubstring = unexpSubstring.toLowerCase();
		}
		
		if (message == null) {
			message = "";
		} else {
			message = message + "\n";
		}
		
		String type = "substring";
		if (isRegexp) {type = "regexp";}
		
		message = message + 
				   "String contained an UN-expected "+type+".\n"
						  + "== Un-expected "+type+": \n"+unexpSubstring+"\n\n"
						  + "== Got string : \n"+gotString+"\n\n";

		if (isRegexp) {
			Pattern patt = Pattern.compile(unexpSubstring);
			Matcher matcher = patt.matcher(gotString);
			Assertions.assertFalse(matcher.find(),
			message+"\nFound at least one occurence of regepx "+unexpSubstring);
		} else {
			Assertions.assertFalse(gotString.contains(unexpSubstring), message);
		}
	}

	public static void assertStringEndsWith(String expEnd, String gotString) {
		Assertions.assertTrue(
			gotString.endsWith(expEnd),
			"String did not have the expected end.\n   expEnd : "+expEnd+"\n   gotString :\n"+gotString);
	}
}
