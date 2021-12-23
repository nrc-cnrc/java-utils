package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.ElasticSearchException;

public class BadDocProtoException extends ElasticSearchException {

	public BadDocProtoException(String mess, Exception e) {
		super(mess, e);
	}

	public BadDocProtoException(String mess) {
		super(mess);
	}

	public BadDocProtoException(Exception e) {
		super(e);
	}
}
