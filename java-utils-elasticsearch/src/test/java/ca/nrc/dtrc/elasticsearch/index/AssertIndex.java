package ca.nrc.dtrc.elasticsearch.index;

import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.json.PrettyPrinter;
import ca.nrc.testing.AssertSet;
import ca.nrc.testing.Asserter;
import org.junit.jupiter.api.Assertions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AssertIndex extends Asserter<ESFactory> {
	public AssertIndex(ESFactory _gotObject) {
		super(_gotObject);
	}

	public AssertIndex(ESFactory _gotObject, String mess) {
		super(_gotObject, mess);
	}

	protected <T extends Document> Set<String> getTypeElementIDs(T docProto)
		throws Exception {
		return getTypeElementIDs(docProto.type, docProto);
	}

	protected <T extends Document> Set<String> getTypeElementIDs(
		String typeName, T docProto) throws Exception {
		SearchResults<T> hits = index().listAll(typeName, docProto);
		Set<String> hitIDs = new HashSet<String>();
		Iterator<String> iter = hits.docIDIterator();
		while (iter.hasNext()) {
			hitIDs.add(iter.next());
		}
		return hitIDs;
	}

	protected IndexAPI index() throws Exception {
		return gotObject.indexAPI();
	}

	public <T extends Document> AssertIndex typeNotEmpty(T docProto) throws Exception {
		return typeNotEmpty(docProto.type, docProto);
	}

	public <T extends Document> AssertIndex typeNotEmpty(String docTypeName, T docProto) throws Exception {
		Set<String> ids = getTypeElementIDs(docTypeName, docProto);
		Assertions.assertTrue(
			ids.size() > 0,
			baseMessage+"\nType "+docProto.type+" should NOT have been empty"
		);
		return this;
	}

	public <T extends Document> AssertIndex typeIsEmpty(T docProto) throws Exception {
		return typeIsEmpty(docProto.type, docProto);
	}

	public <T extends Document> AssertIndex typeIsEmpty(String typeName, T docProto) throws Exception {
		Set<String> ids = getTypeElementIDs(typeName, docProto);
		Assertions.assertTrue(
			ids.isEmpty(),
			baseMessage+"\nType "+docProto.type+" SHOULD have been empty, but contained IDs:\n"+
			PrettyPrinter.print(ids)
		);
		return this;
	}

	public AssertIndex docsInTypeEqual(Document... docs) throws Exception {
		String[] docIDs = new String[docs.length];
		String docType = null;
		Document protoDoc = null;
		for (int ii=0; ii < docs.length; ii++) {
			Document iithDoc = docs[ii];
			docIDs[ii] = iithDoc.getId();
			if (docType != null && !docType.equals(iithDoc.type)) {
				throw new ElasticSearchException(
					"List of expected documents did not have the same type.\n"+
					"Found: "+docType+" and "+ iithDoc.type
				);
			}
			if (protoDoc != null && !protoDoc.getClass().equals(iithDoc.getClass())) {
				throw new ElasticSearchException(
				"List of expected documents were not of the same class.\n"+
				"Found: "+protoDoc.getClass()+" and "+ iithDoc.getClass()
				);
			}
			docType = iithDoc.type;
			protoDoc = iithDoc;
		}
		return docsInTypeEqual(docType, protoDoc, docIDs);
	}

	public AssertIndex docsInTypeEqual(String docTypeName, Document docProto,
		String... expDocIDs) throws Exception {
		for (int ii=0; ii < expDocIDs.length; ii++) {
			expDocIDs[ii] = Document.changeIDType(expDocIDs[ii], docTypeName);
		}
		SearchResults<Document> results = index().listAll(docTypeName, docProto);
		Set<String> gotDocIDs = new HashSet<String>();
		Iterator<Hit<Document>> iter = results.iterator();
		while (iter.hasNext()) {
			gotDocIDs.add(iter.next().getDocument().getId());
		}
		AssertSet.assertEquals(
			baseMessage+"\nDoc type "+docTypeName+" of index "+index().indexName()+" did not contain the expected document IDs.",
			expDocIDs, gotDocIDs
		);
		return this;
	}
}
