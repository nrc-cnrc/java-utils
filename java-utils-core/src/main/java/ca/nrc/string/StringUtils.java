package ca.nrc.string;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.nrc.datastructure.Pair;

public class StringUtils {
	
	public static List<Pair<String, Boolean>> splitWithDelimiters(String regex, String text) {
		List<Pair<String,Boolean>> parts = new ArrayList<Pair<String,Boolean>>();

	    Matcher m = Pattern.compile(regex).matcher(text);
	    int lastEnd = 0;
	    while(m.find()) {
	    	int start = m.start();
	    	if(lastEnd != start) {
	    		String nonDelim = text.substring(lastEnd, start);
	    		parts.add(Pair.of(nonDelim, false));
	    	}
	    	String delim = m.group();
	    	parts.add(Pair.of(delim, true));

	    	int end = m.end();
	    	lastEnd = end;
	    }

	    if(lastEnd != text.length()) {
	      String nonDelim = text.substring(lastEnd);
	      parts.add(Pair.of(nonDelim, false));
	    }
	    
		return parts;
	}	
	
	public static List<Pair<String, Boolean>> tokenizeNaively(String text) {
		String regexp = "(\\p{Punct}|\\s)+";
		List<Pair<String,Boolean>> tokens = StringUtils.splitWithDelimiters(regexp, text);
		
		return tokens;
	}

}