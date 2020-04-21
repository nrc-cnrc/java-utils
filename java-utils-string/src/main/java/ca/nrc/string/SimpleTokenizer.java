package ca.nrc.string;

import ca.nrc.datastructure.Pair;
import ca.nrc.string.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Use this class to tokenize a string into words and word separators, 
 * using a fairly naive approach.
 *   
 * @author desilets
 *
 */

public class SimpleTokenizer  {
	
	public static final String NON_WORD = "[\\p{Punct}\\s]+";
	public static final String SPACES = "\\s+";
	public static final String PUNCTUATION = "\\p{Punct}+";


	public static String[] tokenize(String text)  {
		return tokenize(text, null);
	}

	public static String[] tokenize(String text, Boolean includeSeps)  {
		if (includeSeps == null) {
			includeSeps = false;
		}
		String[] tokens = null;
		if (text != null && text.length() > 0) {
			tokens = tokenStrings(text, includeSeps);
		}
		
		return tokens;
	}
	
	public static List<Pair<String, Boolean>> tokenize_asPairs(String text) {
		List<Pair<String, Boolean>> tokList = StringUtils.splitWithDelimiters(NON_WORD, text);
		
		return tokList;
	}
	

	public static String[] tokenStrings(String text1) {
		return tokenStrings(text1, true);
	}
	
	
	public static String[] tokenStrings(String text, Boolean includeSeps) {
		
		List<Pair<String, Boolean>> tokensInfo = StringUtils.splitWithDelimiters(NON_WORD, text);
		Pattern regexp = Pattern.compile("\\p{Punct}");
		
		List<String> tokensList = new ArrayList<String>();
		for (Pair<String,Boolean> tokInfo: tokensInfo) {
			String token = tokInfo.getFirst();
			boolean isSeparator = tokInfo.getSecond();
			if (includeSeps || !isSeparator) {
					tokensList.add(token);
			}
		}
		

		String[] origTokens = new String[tokensList.size()];
		for (int ii=0; ii<origTokens.length; ii++) {
			origTokens[ii] = tokensList.get(ii);
		}

		// Very first token might include a space and might need to
		// be split into two tokens.
		//
		String[] adjustedTokens = origTokens;
		if (origTokens.length > 0) {
			regexp = Pattern.compile("(\\s+)([^\\s].*)");
			Matcher matcher = regexp.matcher(origTokens[0]);
			if (matcher.matches()) {
				String tok1 = matcher.group(1);
				String tok2 = matcher.group(2);
				adjustedTokens = new String[origTokens.length+1];
				adjustedTokens[0] = tok1;
				adjustedTokens[1] = tok2;
				for (int ii=1; ii < origTokens.length; ii++) adjustedTokens[ii+1] = origTokens[ii];
			}
			
		}

		return adjustedTokens;
	}

	public static boolean isWord(String token) {
		Matcher matcher = Pattern.compile(NON_WORD).matcher(token);
		boolean answer = !matcher.find();
		
		return answer;
	}
}
