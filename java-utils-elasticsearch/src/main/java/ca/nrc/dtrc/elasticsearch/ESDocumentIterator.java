package ca.nrc.dtrc.elasticsearch;

import java.util.Iterator;


public class ESDocumentIterator<DOC_TYPE extends Document> implements Iterator<DOC_TYPE>
{

	private Iterator<Hit<DOC_TYPE>> iterator = null;

	public ESDocumentIterator(SearchResults<DOC_TYPE> _searchResults) {
		this.iterator = _searchResults.iterator();
	}
	
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public DOC_TYPE next() {
		Hit<DOC_TYPE> nextHit = iterator.next();
		DOC_TYPE nextDoc = nextHit.getDocument();
		
		return nextDoc;
	}

}
