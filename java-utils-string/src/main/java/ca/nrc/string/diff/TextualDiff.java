package ca.nrc.string.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ca.nrc.datastructure.Cloner;
import ca.nrc.datastructure.Cloner.ClonerException;
import ca.nrc.datastructure.Pair;
import ca.nrc.json.PrettyPrinter;
import ca.nrc.string.SimpleTokenizer;
import ca.nrc.testing.AssertHelpers;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class TextualDiff {
	
	public static enum DiffersBy {NOTHING, SPACES, SPACES_AND_PUNCT, OTHER};
	
	private boolean ignoreSpaces = false;
		public TextualDiff setIgnoreSpaces(boolean toSet) {
			ignoreSpaces = toSet;
			return this;
		}
		public boolean shouldIgnoreSpace() { return ignoreSpaces; }
	
	public List<StringTransformation> diff(String[] tokensArr1, String[] tokensArr2) throws StringDiffException {
		DiffResult results = diffResult(tokensArr1, tokensArr2);
		List<StringTransformation> transformations = results.transformations;
		return transformations;
	}
		
	public DiffResult diffResult(String[] tokensArr1, String[] tokensArr2) throws StringDiffException {
		List<StringTransformation> transf = diffTransformations(tokensArr1, tokensArr2);
		DiffResult result =new DiffResult(tokensArr1, tokensArr2, transf);
				
		return result;
	}
	
	public DiffResult diffResult(String text1, String text2) throws StringDiffException {
		String[] tokens1 = SimpleTokenizer.tokenize(text1, true);
		String[] tokens2 = SimpleTokenizer.tokenize(text2, true);
		
		List<StringTransformation> transf = diffTransformations(tokens1, tokens2);
		DiffResult result = new DiffResult(tokens1, tokens2, transf);
				
		return result;
	}
	
	public List<StringTransformation> diffTransformations(String text1, String text2) throws StringDiffException {		
		String[] tokens1 = SimpleTokenizer.tokenize(text1, true);
		String[] tokens2 = SimpleTokenizer.tokenize(text2, true);
		List<StringTransformation> transf = diffTransformations(tokens1, tokens2);
		
		return transf;
	}
	
	public List<StringTransformation> diffTransformations(String[] tokensArr1, String[] tokensArr2) throws StringDiffException {
		List<StringTransformation> transformations = new ArrayList<StringTransformation>();
		
		List<String> tokens1 = Arrays.asList(tokensArr1);
		List<String> tokens2 = Arrays.asList(tokensArr2);
		
		Patch<String> patch = DiffUtils.diff(tokens1, tokens2);
		List<Delta<String>> deltas = patch.getDeltas();
		
		// Generate initial list of transformations
		//
		for (Delta aDelta: deltas) {
			StringTransformation transf = new StringTransformation(aDelta);
			if (shouldIgnoreSpace()) {
				transf = trimTransformation(transf, SimpleTokenizer.SPACES);
			}
			if (transf != null) {
				transformations.add(transf);
			}
		}
		
		transformations = collapseTransformations(transformations, tokensArr1, tokensArr2);
		
		return transformations;
		
	}

	protected List<StringTransformation> collapseTransformations(List<StringTransformation> transformations, String[] tokens1, String[] tokens2) {
		
		// Consider the following situation:
		//
		//    tokens1: ['hello', ' ', 'world']
		//    tokens2: ['greetings', ' ', 'universe']
		//
		// In this situation, DiffUtils.diff() will have generate two
		// deltas:
		//
		//    'hello' --> 'greetings'
		//    'world' --> 'universe'
		//
		// But since the text that separates the two is only made up of spaces, 
		// it would be more "useful" if it only generated one delta:
		//
		//    'hello world' --> 'greetings universe'
		//
		// But it would be more useful if it produced only one:
		//    
		//  
		
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.dedupster.diffdocs.TextualDiff.collapseTransformations");
		
		List<StringTransformation> collapsed = transformations;
		
		StringTransformation transfBeingBuilt = null;
		collapsed = new ArrayList<StringTransformation>();
		for (int ii=0; ii<transformations.size(); ii++) {
			StringTransformation transf_ii = transformations.get(ii);
			
			if (transfBeingBuilt == null) {
				// There is no transformation being built at the moment.
				// Therefore, there is no possibility to collapse the iith 
				// transformation with the transformation being built.
				//
				transfBeingBuilt = transf_ii;
				continue;
			}

			// Check if this the text affected by the iith transformation and 
			// the transformation being built are only separated by spaces
			int inBetweenOrigStartPos = transfBeingBuilt.origTokenPos + transfBeingBuilt.origTokens.length;
			int inBetweenOrigEndPos = transf_ii.origTokenPos;
			String[] origTokensInBetween = Arrays.copyOfRange(tokens1, inBetweenOrigStartPos, inBetweenOrigEndPos);
			String origTextInBetween = String.join("", origTokensInBetween);
			
			if (origTextInBetween.matches("^\\s*$")) {
				// Collapse the iith transformation into the transformation being built
				//
				
				// Collapse the original tokens
				List<String> origTokens = new ArrayList<String>();
				for (String tok: transfBeingBuilt.origTokens) origTokens.add(tok);
				for (String tok: origTokensInBetween) origTokens.add(tok);
				for (String tok: transf_ii.origTokens) origTokens.add(tok);
				transfBeingBuilt.origTokens = (String[]) origTokens.toArray(new String[0]);

				// Collapse the revised tokens
				int inBetweenRevStartPos = transfBeingBuilt.revisedTokenPos + transfBeingBuilt.revisedTokens.length;
				int inBetweenRevEndPos = transf_ii.revisedTokenPos;
				String[] revTokensInBetween = Arrays.copyOfRange(tokens2, inBetweenRevStartPos, inBetweenRevEndPos);
				List<String> revTokens = new ArrayList<String>();
				for (String tok: transfBeingBuilt.revisedTokens) revTokens.add(tok);
				for (String tok: revTokensInBetween) revTokens.add(tok);
				for (String tok: transf_ii.revisedTokens) revTokens.add(tok);
				transfBeingBuilt.revisedTokens = (String[]) revTokens.toArray(new String[0]);
			} else {
				// The iith transformation does NOT need to be collapsed into the
				// transformation being built, so:
				//   - Add transformation being built to the list of collapsed transformations
				//   - Start collapsing afresh from the iith transformation
				//
				if (transfBeingBuilt.onlyAffectsSpaces()) {
					transfBeingBuilt = null;
				} else {
					collapsed.add(transfBeingBuilt);
				}
				transfBeingBuilt = transf_ii;
			}
		}
		
		if (transfBeingBuilt != null && !transfBeingBuilt.onlyAffectsSpaces()) {
			// Add the last transformation
			collapsed.add(transfBeingBuilt);
		}
		
		return collapsed;
	}


	private boolean deltaIsOnlyBlankSpaces(Delta<String> delta) {
		String origText = String.join("", delta.getOriginal().getLines());
		String revisedText = String.join("", delta.getRevised().getLines());
		
		origText = origText.replaceAll("[\\s]+", "");
		revisedText = revisedText.replaceAll("[\\s]+", "");

		boolean answer = (origText.equals(revisedText));
		
		return answer;
	}

	public Pair<String, String> markupStrings(String[] origTokens, String[] revTokens, List<StringTransformation> transformations) throws StringDiffException  {
		String origMarkup = "";
		String revMarkup = "";
		
		if (transformations == null) {
			transformations = new TextualDiff().diffTransformations(origTokens, revTokens);
		}

		if (transformations != null) {
			// Markup the original string content
			int prevEndPos = 0;
			for (StringTransformation delta: transformations) {
				Integer deltaStartPos = delta.origTokenPos;
				String[] changedTokens = delta.origTokens;
				
				// Not sure why that happens, but it causes bugs
				if (deltaStartPos > origTokens.length) { continue; }
				if (delta.onlyAffectsSpaces()) { continue; }
				
				origMarkup += String.join("", Arrays.copyOfRange(origTokens, prevEndPos, deltaStartPos));
				origMarkup += "<orig>";
				origMarkup += String.join("", changedTokens);
				origMarkup += "</orig>";
				prevEndPos = deltaStartPos + changedTokens.length;
			}
			if (prevEndPos < origTokens.length) {
				origMarkup += String.join("", Arrays.copyOfRange(origTokens, prevEndPos, origTokens.length));			
			}
			origMarkup = origMarkup.replaceAll("</orig>([\\W\\d]*)<orig>", "$1");
			
			// Markup the revised fragment content
			prevEndPos = 0;
			for (StringTransformation delta: transformations) {
				Integer deltaStartPos = delta.revisedTokenPos;
				String[] changedTokens = delta.revisedTokens;		
				
				// Not sure why that happens, but it causes bugs
				if (deltaStartPos > revTokens.length) { continue; }
				
				revMarkup += String.join("", Arrays.copyOfRange(revTokens, prevEndPos, deltaStartPos));
				revMarkup += "<revised>";
				revMarkup += String.join("", changedTokens);
				revMarkup += "</revised>";
				prevEndPos = deltaStartPos + changedTokens.length;
			}
			if (prevEndPos < revTokens.length) {
				revMarkup += String.join("", Arrays.copyOfRange(revTokens, prevEndPos, revTokens.length));			
			}
			revMarkup = revMarkup.replaceAll("</revised>([\\W\\d]*)<revised>", "$1");
		}
		
		return Pair.of(origMarkup, revMarkup);
	}
	
	protected DiffersBy differBy(String text1, String text2) {
		DiffersBy what = null;
		
		if (text1.equals(text2)) {
			what = DiffersBy.NOTHING;
		}
		
		if (what == null && identicalExceptFor(text1, text2, "[\\s_]")) {
			what = DiffersBy.SPACES;
		}
		
		if (what == null && identicalExceptFor(text1, text2, "[\\W_]")) {
			what = DiffersBy.SPACES_AND_PUNCT;
		}
		
		if (what == null) {
			what = DiffersBy.OTHER;
		}
		
		return what;
	}

	public boolean areIdentical(String text1, String text2) {
		return identicalExceptFor(text1, text2, null);
	}

	
	public boolean identicalExceptFor(String text1, String text2, String exceptFor) {
		if (exceptFor == null) {
			exceptFor = "([\\W]|_)";
		}
		exceptFor = exceptFor + "+";
		text1 = text1.toLowerCase().replaceAll(exceptFor, "").trim();
		text2 = text2.toLowerCase().replaceAll(exceptFor, "").trim();
		
		boolean identical = (text1.equals(text2));
		
		return identical;
	}
	
	StringTransformation trimTransformation(StringTransformation transf, 
			String toTrimRegex) throws StringDiffException {
		
		StringTransformation trimmed = null;
		if (!transf.onlyAffectsSpaces()) {
			try {
				trimmed = Cloner.clone(transf);
			} catch (ClonerException e) {
				throw new StringDiffException(e);
			}

			// Establish the exact number of tokens to trim on both ends of the
			// Orig and Rev tokens
			//
			Pair<Integer,Integer> maxToTrimOrig = numTokensToTrim(trimmed.origTokens, SimpleTokenizer.SPACES);
			Pair<Integer,Integer> maxToTrimRev = numTokensToTrim(trimmed.revisedTokens, SimpleTokenizer.SPACES);
			
			Pair<Integer,Integer> numToTrimOrig = Pair.of(maxToTrimOrig.getFirst(), maxToTrimOrig.getSecond());
			{
				if (numToTrimOrig.getFirst() > maxToTrimRev.getFirst()) {
					numToTrimOrig.setFirst(maxToTrimRev.getFirst());
				}
				if (numToTrimOrig.getSecond() > maxToTrimRev.getSecond()) {
					numToTrimOrig.setSecond(maxToTrimRev.getSecond());
				}
			}
			
			Pair<Integer,Integer> numToTrimRev = Pair.of(maxToTrimRev.getFirst(), maxToTrimRev.getSecond());
			{
				if (numToTrimRev.getFirst() > maxToTrimOrig.getFirst()) {
					numToTrimRev.setFirst(maxToTrimOrig.getFirst());
				}
				if (numToTrimRev.getSecond() > maxToTrimOrig.getSecond()) {
					numToTrimRev.setSecond(maxToTrimOrig.getSecond());
				}
			}
			
			// Trim original tokens
			trimmed.origTokenPos = trimmed.origTokenPos + numToTrimOrig.getFirst();			
			trimmed.origTokens = trimTokens(trimmed.origTokens, numToTrimOrig);
			
			// Trim revised tokens
			trimmed.revisedTokenPos = trimmed.revisedTokenPos + numToTrimRev.getFirst();			
			trimmed.revisedTokens = trimTokens(trimmed.revisedTokens, numToTrimRev);
		}
		
		return trimmed;
	}

	public static String[] trimTokens(String[] tokens , Pair<Integer, Integer> numToTrim) {
		String[] trimmedTokens  = Arrays.copyOfRange(tokens, numToTrim.getFirst(), tokens.length);
		trimmedTokens = Arrays.copyOfRange(trimmedTokens, 0, trimmedTokens.length - numToTrim.getSecond());

		return trimmedTokens;
	}

	public static Pair<Integer, Integer> numTokensToTrim(String[] tokens, 
			String toTrimRegex) {
		Integer numLeading = 0;
		Integer numTailing = 0;
		
		// Find number of leading tokens to be trimmed
		//
		for (int ii=0; ii < tokens.length; ii++) {		
			String token_ii = tokens[ii];
			
			if (token_ii.length() == 0) break;
			
			if (!token_ii.matches(toTrimRegex)) {
				break;
			} else {
				numLeading++;
			}
		}
		
		// Find number of tailing tokens to be trimmed
		//
		// But only if the leading trimming did not consume
		// the whole list of tokens
		//
		if (numLeading < tokens.length) {
			for (int ii = tokens.length-1; ii >= 0; ii--) {
				String token_ii = tokens[ii];
				
				if (token_ii.length() == 0) break;
	
				if (!token_ii.matches(toTrimRegex)) {
					break;
				} else {
					numTailing++;
				}
			}
		}
			
		return Pair.of(numLeading, numTailing);	
	}

	private static boolean allBlank(String[] tokens) {
		String tokensStr = String.join("", tokens);
		boolean answer = (tokensStr.matches("^\\s*$"));
		
		return answer;
	}
	
	public static String[] tokenize(String text) {
		String[] tokens = SimpleTokenizer.tokenize(text, true);
		return tokens;
	}
}
