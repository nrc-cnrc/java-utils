package ca.nrc.string.diff;

public class StringDiffException extends Exception {

	private static final long serialVersionUID = 6494345616340405363L;
	
	public StringDiffException(String mess, Exception e) { super(mess, e); }
	public StringDiffException(Exception e) { super(e); }
	public StringDiffException(String mess) { super(mess); }
}
