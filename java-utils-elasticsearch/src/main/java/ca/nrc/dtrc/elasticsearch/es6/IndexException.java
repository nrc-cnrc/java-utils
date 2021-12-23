package ca.nrc.dtrc.elasticsearch.es6;

public class IndexException extends ElasticSearchException {

	private static final long serialVersionUID = 1L;

	public IndexException(Exception exc) {
		super(exc);
	}

	public IndexException(String mess) {
		super(mess);
	}
}
