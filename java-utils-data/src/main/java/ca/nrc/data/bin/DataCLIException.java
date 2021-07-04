package ca.nrc.data.bin;

public class DataCLIException extends Exception {

	DataCLIException(String mess, Exception e) {
		super(mess, e);
	}
	DataCLIException(String mess) {
		super(mess);
	}
	DataCLIException(Exception e) {
		super(e);
	}
}
