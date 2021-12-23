package ca.nrc.dtrc.elasticsearch.es6;

public class ESDocumentIterator_Null<DOC_TYPE extends Document> extends ESDocumentIterator<DOC_TYPE> {

	public ESDocumentIterator_Null() {
		super(null);
	}

	public ESDocumentIterator_Null(SearchResults _searchResults) {
		super(_searchResults);
	}
	
	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public DOC_TYPE next() {
		return null;
	}
	

}
