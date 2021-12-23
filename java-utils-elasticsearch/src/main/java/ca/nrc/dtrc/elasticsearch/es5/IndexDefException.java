package ca.nrc.dtrc.elasticsearch.es5;

public class IndexDefException extends ElasticSearchException {
	
	public IndexDefException(String mess, Exception e) {super(mess, e);}
	
	public IndexDefException(String mess) {super(mess);}
	
	public IndexDefException(Exception e) {super(e);}
}
