package ca.nrc.dtrc.elasticsearch.index;

import ca.nrc.dtrc.elasticsearch.ElasticSearchException;

public class IndexDefException extends ElasticSearchException {
	
	public IndexDefException(String mess, Exception e) {super(mess, e);}
	
	public IndexDefException(String mess) {super(mess);}
	
	public IndexDefException(Exception e) {super(e);}
}
