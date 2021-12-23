package ca.nrc.dtrc.elasticsearch.es6;

public class NullDocTypeException extends ElasticSearchException {
	public NullDocTypeException() {
		super("Document had a null type");
	}
	public NullDocTypeException(Class<? extends Document> docClass) {
		super("Document of type "+docClass+" had a null type.");
	}
}
