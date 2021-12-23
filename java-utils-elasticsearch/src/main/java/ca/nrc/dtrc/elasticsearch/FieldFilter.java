package ca.nrc.dtrc.elasticsearch;

import java.util.regex.Pattern;

public class FieldFilter {
	private boolean isNegative = true;
	Pattern patt = null;

	public FieldFilter(String _regexp, boolean _isNegative) {
		this.isNegative = _isNegative;
		this.patt = Pattern.compile(_regexp);
	}

	public boolean keepField(String fieldName) {
		boolean pattMatched = patt.matcher(fieldName).matches();
		boolean keep = pattMatched;
		if (isNegative) keep = !pattMatched;
		
		return keep;
	}

}
