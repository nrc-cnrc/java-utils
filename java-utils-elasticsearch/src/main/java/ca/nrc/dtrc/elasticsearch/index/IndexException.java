package ca.nrc.dtrc.elasticsearch.index;

import ca.nrc.dtrc.elasticsearch.ElasticSearchException;

public class IndexException extends ElasticSearchException {

	private static final long serialVersionUID = 1L;

	public IndexException(Exception exc) {
		super(exc);
	}

	public IndexException(String mess) {
		super(mess);
	}
}
