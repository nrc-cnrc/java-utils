package ca.nrc.string;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.nrc.datastructure.Pair;

public class StringUtils {
	
	public static String join(Iterator<?> iter) {
		return join(iter, null);
	}

	public static String join(Iterator<?> iter, String delimiter) {
		if (delimiter == null) {
			delimiter = ",";
		}
		StringBuilder joined = new StringBuilder();

		boolean isFirst = true;
		while (iter.hasNext()) {
			if (!isFirst) {
				joined.append(delimiter);
			}
			isFirst = false;
			joined.append(iter.next().toString());
		}
		
		return joined.toString();
	}

	public static String join(Object[] array, String delimiter) {
		StringBuilder str = new StringBuilder();
		boolean isFirst = true;
		for (Object elt: array) {
			if (!isFirst) {
				str.append(elt.toString());
			}
			isFirst = false;
		}
		
		return str.toString();
	}

	public static List<Pair<String, Boolean>> splitWithDelimiters(String regexp, String text) {
		Pattern patt = Pattern.compile(regexp);
		return splitWithDelimiters(patt, text);
	}
	
	public static List<Pair<String, Boolean>> splitWithDelimiters(Pattern patt, String text) {
		List<Pair<String,Boolean>> parts = new ArrayList<Pair<String,Boolean>>();

	    Matcher m = patt.matcher(text);
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

	public static int countMatches(String text, String expr) {
		int count = org.apache.commons.lang3.StringUtils.countMatches(text, expr);
		return count;
	}

}
