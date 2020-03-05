package ca.nrc.string.diff;

import java.util.List;

public class DiffResult {
	public String[] origTokens = null;
	public String[] revTokens = null;
	public List<StringTransformation> transformations = null;

	public DiffResult(String[] _origTokens, String[] _revTokens,
			List<StringTransformation> _transformations) {
		this.origTokens = _origTokens;
		this.revTokens = _revTokens;
		this.transformations = _transformations;
	}
	
	public String origStr() {
		String _origStr = null;
		if (this.origTokens != null) {
			_origStr = String.join("", origTokens);
		}
		
		return _origStr;
	}
	
	public String revStr() {
		String _revStr = null;
		if (this.revTokens != null) {
			_revStr = String.join("", revTokens);
		}
		
		return _revStr;
	}
	
	public int numAffectedTokens() {	
		int numAffected = 0;
		for (StringTransformation aTransf: this.transformations) {
			numAffected += aTransf.numAffectedTokens();
		};
		
		return numAffected;
	}
	
}
