package ca.nrc.dtrc.elasticsearch;

import ca.nrc.datastructure.CloseableIterator;
import ca.nrc.json.PrettyPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;

public class DocIDIterator<T extends Document> implements CloseableIterator<String> {

	Iterator<Hit<T>> hitsIter = null;

	private Boolean withoutType = false;

	public DocIDIterator(Iterator<Hit<T>> _hitsIter) {
		init__DocIDIterator(_hitsIter, (Boolean)null);
	}

	public DocIDIterator(Iterator<Hit<T>> _hitsIter, Boolean withoutType) {
		init__DocIDIterator(_hitsIter, withoutType);
	}

	private void init__DocIDIterator(Iterator<Hit<T>> _hitsIter, Boolean _withoutType) {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.init__DocIDIterator");
		if (_withoutType != null) {
			this.withoutType = _withoutType;
		}

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
    	T hit =  hitsIter.next().getDocument();
    	String id = hit.getIdWithoutType();
    	if (withoutType) {
    		id = hit.getIdWithoutType();
		}
    	return id;
    }

	@Override
	public void close() throws Exception {
		// Nothing to do in terms of closing.
	}
}
