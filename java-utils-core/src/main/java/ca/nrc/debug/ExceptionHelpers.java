package ca.nrc.debug;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class ExceptionHelpers {
	
	public static String whatFileWasNotFound(FileNotFoundException e) {
		String filePath = null;
		
		String excMessage = e.getMessage();		
		Matcher matcher = 
			Pattern.compile("^([\\s\\S]+) \\(No such file or directory\\)")
					.matcher(excMessage);
		if (matcher.find()) {
			filePath = matcher.group(1);
		}
		
		return filePath;
	}
	
	public static String printExceptionCauses(Exception exc) {
		String causes = ExceptionUtils.getStackTrace(exc);

	    for (Throwable t = exc.getCause(); t != null; t = t.getCause()) {
	        causes += "\n" + ExceptionUtils.getStackTrace(t);
	    }
		return causes;
	}
}
