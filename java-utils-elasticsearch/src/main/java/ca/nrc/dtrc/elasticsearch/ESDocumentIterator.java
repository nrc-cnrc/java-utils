package ca.nrc.dtrc.elasticsearch;

import ca.nrc.datastructure.CloseableIterator;

import java.util.Iterator;


public class ESDocumentIterator<DOC_TYPE extends Document> implements CloseableIterator<DOC_TYPE>
{

	private Iterator<Hit<DOC_TYPE>> iterator = null;
	
	public Long totalDocs = new Long(0);

	public ESDocumentIterator(SearchResults<DOC_TYPE> _searchResults) {
		if (_searchResults != null) {
			this.iterator = _searchResults.iterator();
			this.totalDocs = _searchResults.getTotalHits();
		}
	}
	
	@Override
	public boolean hasNext() {
		if (iterator == null) {
			return false;
		} else {
			return iterator.hasNext();
		}
	}

	@Override
	public DOC_TYPE next() {
		DOC_TYPE nextDoc = null;
		if (iterator != null) {
			Hit<DOC_TYPE> nextHit = iterator.next();
			nextDoc = nextHit.getDocument();
		}
		
		return nextDoc;
	}

	@Override
	public void close() throws Exception {
		// Nothing to close
	}
}
