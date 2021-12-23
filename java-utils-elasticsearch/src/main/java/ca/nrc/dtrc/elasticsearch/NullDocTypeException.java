package ca.nrc.dtrc.elasticsearch;

public class NullDocTypeException extends ElasticSearchException {
	public NullDocTypeException(String mess) {
		super(mess);
	}
	public NullDocTypeException() {
		super("Document had a null type");
	}
	public NullDocTypeException(Class<? extends Document> docClass) {
		super("Document of type "+docClass+" had a null type.");
	}
}