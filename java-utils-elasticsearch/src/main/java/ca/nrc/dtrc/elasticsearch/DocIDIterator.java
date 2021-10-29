package ca.nrc.dtrc.elasticsearch;

import ca.nrc.json.PrettyPrinter;
import org.apache.log4j.Logger;

import java.util.Iterator;

public class DocIDIterator<T extends Document> implements Iterator<String> {

    Iterator<Hit<T>> hitsIter = null;

    public DocIDIterator(Iterator<Hit<T>> _hitsIter) {
        Logger tLogger = Logger.getLogger("ca.nrc.dtrc.elasticsearch.DocIDIterator");
        if (tLogger.isTraceEnabled()) {
            tLogger.trace("Constructing with _hitsIter="+ PrettyPrinter.print(_hitsIter));
        }

        hitsIter = _hitsIter;
    }

    @Override
    public boolean hasNext() {
    	boolean answer = false;
    	if (hitsIter != null) {
    		answer = hitsIter.hasNext();
		}
    	return answer;
    }

    @Override
    public String next() {
        return hitsIter.next().getDocument().getId();
    }
}
