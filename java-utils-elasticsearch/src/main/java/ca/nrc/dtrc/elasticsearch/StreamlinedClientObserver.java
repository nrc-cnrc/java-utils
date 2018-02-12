package ca.nrc.dtrc.elasticsearch;

public abstract class StreamlinedClientObserver {
	public abstract void onBulkIndex(int fromLine, int toLine, String indexName, String docTypeName);
}
