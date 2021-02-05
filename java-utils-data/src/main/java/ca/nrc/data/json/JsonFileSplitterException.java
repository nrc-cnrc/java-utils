package ca.nrc.data.json;

public class JsonFileSplitterException extends Exception {
	public JsonFileSplitterException(String mess, Throwable e) {
		super(mess, e);
	}
	public JsonFileSplitterException(String mess) {
		super(mess);
	}
	public JsonFileSplitterException(Throwable e) {
		super(e);
	}
}
