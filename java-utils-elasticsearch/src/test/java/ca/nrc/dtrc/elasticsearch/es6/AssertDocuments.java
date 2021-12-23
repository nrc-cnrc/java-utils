package ca.nrc.dtrc.elasticsearch.es6;

import ca.nrc.testing.Asserter;

import java.util.Collection;

public class AssertDocuments extends Asserter<Collection<? extends Document>> {
	public AssertDocuments(Collection<? extends Document> expDocuments) {
		super(null);
		init_AssertDocuments(expDocuments);
	}

	private void init_AssertDocuments(Collection<? extends Document> expDocuments) {
		this.gotObject = expDocuments;
	}
}
