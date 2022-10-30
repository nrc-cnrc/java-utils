package ca.nrc.dtrc.elasticsearch;

import ca.nrc.datastructure.CloseableIterator;

import java.util.Iterator;

public class DocIterator<T extends Document> implements CloseableIterator<T> {

    Iterator<Hit<T>> hitsIter = null;

    public DocIterator(Iterator<Hit<T>> _hitsIter) {
        hitsIter = _hitsIter;
    }

    @Override
    public boolean hasNext() {
        return hitsIter.hasNext();
    }

    @Override
    public T next() {
        return hitsIter.next().getDocument();
    }

	@Override
	public void close() throws Exception {
		// Nothing to close
	}
}
