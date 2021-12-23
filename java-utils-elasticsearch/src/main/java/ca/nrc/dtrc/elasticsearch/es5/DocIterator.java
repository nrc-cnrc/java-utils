package ca.nrc.dtrc.elasticsearch.es5;

import java.util.Iterator;

public class DocIterator<T extends Document> implements Iterator<T> {

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
}
