package ca.nrc.string.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.nrc.datastructure.Pair;
import ca.nrc.string.SimpleTokenizer;
import difflib.Chunk;
import difflib.Delta;

public class StringTransformation {
	public Integer origTokenPos = null;
	public String[] origTokens = null;
	public Integer revisedTokenPos = null;
	public String[] revisedTokens = null;
	
	public StringTransformation() {
		init_TextualDiff(null, null, null, null);
	}
	
	public StringTransformation(int _origPos, List<String> _origTokens, 
			int _resvisedPos, List<String> _revisedTokens) {
		init_TextualDiff(_origPos, _origTokens, _resvisedPos, _revisedTokens);
	}

	public StringTransformation(int _origPos, String[] _origTokensArr, 
			int _resvisedPos, String[] _revisedTokensArr) {			
		List<String> _origTokens = Arrays.asList(_origTokensArr);
		List<String> _revisedTokens = Arrays.asList(_revisedTokensArr);
		init_TextualDiff(_origPos, _origTokens, _resvisedPos, _revisedTokens);
	}
		
	private void trimSpaceTokens() {
		// Figure out the exact number of tokens to trim from 
		// the Orig and Revised tokens
		//
		Pair<Integer,Integer> numToTrimOrig = TextualDiff.numTokensToTrim(this.origTokens, SimpleTokenizer.SPACES);
		Pair<Integer,Integer> numToTrimRev = TextualDiff.numTokensToTrim(this.revisedTokens, SimpleTokenizer.SPACES);

		Integer numLeft = Math.min(numToTrimOrig.getFirst(), numToTrimRev.getFirst());
		Integer numRight = Math.min(numToTrimOrig.getSecond(), numToTrimRev.getSecond());
		Pair<Integer,Integer> numToTrim = Pair.of(numLeft, numRight);
		
		origTokenPos += numToTrim.getFirst();
		origTokens = TextualDiff.trimTokens(origTokens, numToTrim);

		revisedTokenPos += numToTrim.getFirst();
		revisedTokens = TextualDiff.trimTokens(revisedTokens, numToTrim);

		return;
	}

	public StringTransformation(Delta<String> delta) {
		Chunk<String> orig = delta.getOriginal();
		Chunk<String> rev = delta.getRevised();
		init_TextualDiff(orig.getPosition(), orig.getLines(), rev.getPosition(), rev.getLines());
	}

	private void init_TextualDiff(Integer _origPos, List<String> _origTokens, 
			Integer _revisedPos, List<String> _revisedTokens) {
		
		if (_origPos == null) { _origPos = -1; }
		if (_origTokens == null) { _origTokens = new ArrayList<String>(); }
		if (_revisedTokens == null) { _revisedTokens = new ArrayList<String>(); }
		if (_revisedPos == null) { _revisedPos = -1; }

		this.origTokenPos = _origPos;
		
		if (_origTokens != null) {
			this.origTokens = (String[]) _origTokens.toArray(new String[_origTokens.size()]);
		}
		this.revisedTokenPos = _revisedPos;
		if (_revisedTokens != null) {
			this.revisedTokens = (String[]) _revisedTokens.toArray(new String[_revisedTokens.size()]);
		}		
		
		trimSpaceTokens();
	}
	

	public boolean onlyAffectsSpaces() {
		String origTextNoSpaces = String.join("", this.origTokens).replaceAll("\\s+", "");
		String revTextNoSpaces = String.join("", this.revisedTokens).replaceAll("\\s+", "");
		
		boolean answer = (origTextNoSpaces.equals(revTextNoSpaces));
		
		return answer;
	}

		public String toString() {
		String origTokensStr = "null";
		if (origTokens != null) {
			origTokensStr = "['"+String.join("', '", origTokens)+"']";
		}
		String revTokensStr = "null";
		if (revisedTokens != null) {
			revTokensStr = "['"+String.join("', '", revisedTokens)+"']";
		}
		
		String toS = 
				"{pos="+origTokenPos+", tokens="+origTokensStr+" ==> pos=" +
				revisedTokenPos+", tokens="+revTokensStr+"}";
		
		return toS;
	}
}
