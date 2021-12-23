package ca.nrc.dtrc.elasticsearch.es6;

public class NoSuchIndexException extends ElasticSearchException {

	private static final long serialVersionUID = 2585278825516127496L;

	public NoSuchIndexException(Exception exc) {
		super(exc);
	}

	public NoSuchIndexException(String mess) {
		super(mess);
	}


}
