package ca.nrc.datastructure;

import java.util.Iterator;

/**
 * Interface for iterators that must be closed when you are done with it.
 * For example iterator that iterates through the results of a dataabse query that
 * uses up some SQL resources.
 */
public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {
}
