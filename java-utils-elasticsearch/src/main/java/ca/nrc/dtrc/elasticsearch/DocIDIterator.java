package ca.nrc.dtrc.elasticsearch;

import java.util.Iterator;

public class DocIDIterator<T extends Document> implements Iterator<String> {

    Iterator<Hit<T>> hitsIter = null;

    public DocIDIterator(Iterator<Hit<T>> _hitsIter) {
        hitsIter = _hitsIter;
    }

    @Override
    public boolean hasNext() {
        return hitsIter.hasNext();
    }

    @Override
    public String next() {
        return hitsIter.next().getDocument().getId();
    }
}
