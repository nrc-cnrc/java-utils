package ca.nrc.dtrc.elasticsearch;

public class BadDocProtoException extends ElasticSearchException {
	public BadDocProtoException(Exception e) {
		super(e);
	}
}
