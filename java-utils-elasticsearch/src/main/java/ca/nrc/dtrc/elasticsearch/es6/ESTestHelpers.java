package ca.nrc.dtrc.elasticsearch.es6;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.json.JSONUtils;
import ca.nrc.testing.AssertHelpers;
import ca.nrc.file.ResourceGetter;

public class ESTestHelpers {
	
	private static final PlayLine playLinePrototype = new PlayLine();
	
	public static final long SHORT_WAIT = 1000;
	public static final long LONG_WAIT = 2*SHORT_WAIT;
	public static final long EXTRA_LONG_WAIT = 4*LONG_WAIT;

	private static Boolean skipTests = null;

	public static List<String> indicesToBeCleared = new ArrayList<String>();


	// This test index is guaranteed to be initially empty
	public static final String emptyTestIndex = "es-test";

	public static final String hamletTestIndex = "es-test-hamlet";
	public static final String hamletType = "hamlet_lines";

	public static final String cartoonsTestIndex = "es-test-cartoons";


	public static String hamletFirstLine =
		"{\"_detect_language\":true,\"additionalFields\":{\"line_id\":32435,\"line_number\":\"\",\"play_name\":\"Hamlet\",\"speaker\":\"CYMBELINE\",\"speech_number\":138},\"content\":\"FRANCISCO at his post. Enter to him BERNARDO\",\"creationDate\":null,\"id\":\"null:0.0.0\",\"idWithoutType\":\"0.0.0\",\"lang\":\"en\",\"longDescription\":\"FRANCISCO at his post. Enter to him BERNARDO\",\"shortDescription\":null,\"type\":null}";

	public static String hamletLastLine =
		"{\"_detect_language\":true,\"additionalFields\":{\"line_id\":36676,\"line_number\":\"5.2.425\",\"play_name\":\"Hamlet\",\"speaker\":\"PRINCE FORTINBRAS\",\"speech_number\":147},\"content\":\"A dead march. Exeunt, bearing off the dead bodies; after which a peal of ordnance is shot off\",\"creationDate\":null,\"id\":\"null:5.2.425\",\"idWithoutType\":\"5.2.425\",\"lang\":\"en\",\"longDescription\":\"A dead march. Exeunt, bearing off the dead bodies; after which a peal of ordnance is shot off\",\"shortDescription\":null,\"type\":null}";

	public static void skipTestsUnlessESIsRunning() throws Exception {
		if (skipTests == null) {
		// Check to see if the correct version of ElasticSearch is running.
		Integer expVersion = 6;
		try {
			StreamlinedClient client = new StreamlinedClient("nevermind");
			skipTests = !client.serverIsRunningVersion(6);
		} catch (ElasticSearchException exc) {
			// We weren't even able to ask for the server version.
			// Check to see what is wrong.
			String mess = exc.getMessage();
			if (mess.matches(
				"^(Failed to connect to ElasticSearch server|"+
				"ca.nrc.web.HttpException: Error invoking).*$")) {
				// We were unable to connect to the ElasticSearch server.
				// Probably means that the server is not running, so
				// skip all remaining tests that require ElasticSearch to run
					skipTests = true;
				Assert.fail(
						  "ElasticSearch v"+expVersion+" is either not installed or is not running.\n\n"
						+ "If you want to test the ESFactory v"+expVersion+" utilities, install ESFactory and make sure to start it using the command line 'elasticsearch'.\n\n"
						+ "For now, skipping all remaining ElasticSearch tests.");
			}
		}
		}
		org.junit.Assume.assumeFalse(skipTests);
	}

	public static StreamlinedClient makeCartoonTestIndex() throws InterruptedException, ElasticSearchException, IOException {
		StreamlinedClient client = new StreamlinedClient(cartoonsTestIndex);
		client.clearIndex(false);

		// Sleep a bit to give the ESFactory server to propagate the index to
		// all nodes in its cluster
		Thread.sleep(1000);

		client.putDocument(new TVShow("The Simpsons"));
		client.putDocument(
			new ShowCharacter("Homer", "Simpson", "The Simpsons")
			.setAge(42));
		client.putDocument(
			new ShowCharacter("Marge", "Simpson", "The Simpsons")
			.setAge(40));
		client.putDocument(new ShowCharacter("Mr", "Burns", "The Simpsons"));

		client.putDocument(new TVShow("Peanuts"));
		client.putDocument(new ShowCharacter("Charlie", "Brown", "Peanuts"));
		client.putDocument(new ShowCharacter("Lucy", "", "Peanuts"));

		// Sleep a bit to give the ESFactory server to propagate the index to
		// all nodes in its cluster
		Thread.sleep(2*1000);

		return client;
	}


	public static StreamlinedClient makeEmptyTestClient() throws IOException, ElasticSearchException, InterruptedException {
		StreamlinedClient client = deleteTestIndex();
		client.createIndex(emptyTestIndex);
		return client;
	}

	public static StreamlinedClient deleteTestIndex() throws IOException, ElasticSearchException, InterruptedException {
		// Put a one second delay after each transaction, to give ESFactory time to synchronize all the nodes.
		double sleepSecs = 1.0;
		StreamlinedClient client = new StreamlinedClient(emptyTestIndex, sleepSecs);
		client.deleteIndex();

		return client;
	}


	public static StreamlinedClient makeHamletTestClient() throws IOException, ElasticSearchException, InterruptedException {
		return makeHamletTestClient(null);
	}

	public static StreamlinedClient makeHamletTestClient(String collectionName) throws IOException, ElasticSearchException, InterruptedException {
		// Put a two second delay after each transaction, to give ESFactory time to synchronize all the nodes.
		double sleepSecs = 2.0;
		StreamlinedClient client = new StreamlinedClient(hamletTestIndex, sleepSecs);
		client.clearIndex(false);

		String fPath = ResourceGetter
			.getResourcePath("test_data/ca/nrc/dtrc/elasticsearch/es6/hamlet.json");
		if (collectionName == null) {
			client.bulk(new File(fPath), PlayLine.class);
		} else {
			client.bulk(new File(fPath), collectionName);
		}

		// Sleep a bit to give the ESFactory server to propagate the index to
		// all nodes in its cluster
		Thread.sleep(1000);

		return client;
	}

	public static void assertTypeIsEmpty(String indexName, Document docProto)
		throws IOException, ElasticSearchException, InterruptedException {
		StreamlinedClient client = new StreamlinedClient(indexName);
		assertTypeIsEmpty(client, docProto);
	}

	public static void assertTypeIsEmpty(StreamlinedClient client, Document docProto)
		throws IOException, ElasticSearchException, InterruptedException {

		int numHits = 0;
		try {
			client.listAll(docProto);
		} catch (Exception exc) {
			// If exception thrown, it means the index does not exist.
			// So consider that it's OK (non-existant == empty)
		}

		Assert.assertEquals("Index should have been empty.", 0, numHits);
	}

	private static void assertResponseEquals(Map<String,Object> expRespObject, String gotJsonResp) throws JsonParseException, JsonMappingException, IOException {
		Map<String,Object> gotRespObject = new HashMap<String,Object>();
		gotRespObject = new ObjectMapper().readValue(gotJsonResp, gotRespObject.getClass());
		AssertHelpers.assertDeepEquals("", expRespObject, gotRespObject);
	}

	public static void assertNoError(String jsonResponse) throws JsonParseException, JsonMappingException, IOException {
		Map<String,Object> respObject = JSONUtils.json2ObjectMap(jsonResponse);
		Assert.assertFalse("The jsonResponse should not have been an error.\nBut it was:\n"+jsonResponse, respObject.containsKey("error"));

	}

	public static <T extends Document> void  assertIndexSizeEquals(String indexName, int expNumDocs, T docPrototype) throws IOException, ElasticSearchException, InterruptedException {
		assertIndexSizeEquals(indexName, expNumDocs, docPrototype, docPrototype.getClass().getName());
	}

	public static <T extends Document> void  assertIndexSizeEquals(String indexName, int expNumDocs, T docPrototype, String esTypeName) throws IOException, ElasticSearchException, InterruptedException {
		StreamlinedClient client = new StreamlinedClient(indexName);
		SearchResults<T> gotResults = client.listAll(esTypeName, docPrototype);

		int gotNumDocs = 0;
		Iterator<Hit<T>> iter = gotResults.iterator();
		while (iter.hasNext()) {
			gotNumDocs++;
			iter.next();
		}

		Assert.assertEquals("Number of documents in index "+indexName+" was not as expected", expNumDocs, gotNumDocs);

	}

	public static void assertIndexContainsDoc(String indexName, String esDocType, Document expDoc) throws ElasticSearchException, IOException, InterruptedException {
		StreamlinedClient client = new StreamlinedClient(indexName);
		Document gotDoc = client.getDocumentWithID(expDoc.getId(), expDoc.getClass());
		AssertHelpers.assertDeepEquals("Index "+indexName+" did not contain the expected document", expDoc, gotDoc);
	}

	public static void assertIndexContainsDoc(String indexName, Document expDocument) throws ElasticSearchException, IOException {
		assertIndexContainsDoc(indexName, expDocument, expDocument.getClass().getName());
	}

	public static void assertIndexContainsDoc(String indexName, Document expDocument, String collection) throws ElasticSearchException, IOException {
		StreamlinedClient client = new StreamlinedClient(indexName);
		String id = expDocument.getId();
		Document gotDoc = client.getDocumentWithID(id, expDocument.getClass(), collection);
		Assert.assertTrue("Collection "+collection+" of index "+indexName+" did not contain a document with ID="+id, gotDoc != null);
		AssertHelpers.assertDeepEquals("Document id="+id+" for collection"+collection+" of index "+indexName+" was not as expected" ,
				expDocument, gotDoc);
	}


	public static void assertDocTypeIsEmpty(String message, String indexName,
			String docType, Document protoDoc) throws Exception {
		StreamlinedClient client = new StreamlinedClient(indexName);
		SearchResults<Document> results = client.listAll(docType, protoDoc);
		long totalHits = results.getTotalHits();
		Assert.assertEquals("Doc type "+docType+" of index "+indexName+" should have been empty.", 0, totalHits);

	}

	public static void assertDocTypeContainsDoc(String message, String indexName, String docType,
			String[] expDocIDs, Document protoDoc)  throws Exception {
		StreamlinedClient client = new StreamlinedClient(indexName);
		SearchResults<Document> results = client.listAll(docType, protoDoc);
		Set<String> gotDocIDs = new HashSet<String>();
		Iterator<Hit<Document>> iter = results.iterator();
		while (iter.hasNext()) {
			gotDocIDs.add(iter.next().getDocument().getId());
		}
		AssertHelpers.assertUnOrderedSameElements(message+"Doc type "+docType+" of index "+indexName+" did not contain the expected document IDs.",
				expDocIDs, gotDocIDs);
	}

	public static void sleepShortTime() throws InterruptedException {
		Thread.sleep(SHORT_WAIT);
	}

	public static void sleepLongTime() throws InterruptedException {
		Thread.sleep(LONG_WAIT);
	}

	public static void sleepExtraLongTime() throws InterruptedException {
		Thread.sleep(EXTRA_LONG_WAIT);
	}

	public static void addTestIndicesToBeCleared(String[] indices) {
		indicesToBeCleared.addAll(Arrays.asList(indices));
	}

	public static void clearTestIndices() throws IOException, ElasticSearchException, InterruptedException {
		for (String index: indicesToBeCleared) {
			new StreamlinedClient(index).clearIndex();
		}
	}

	public static void clearIndexCollection(String index, String collection) throws ElasticSearchException {
		new StreamlinedClient(index).clearDocType(collection);

	}

	/////////////////////////////////////////////
	// Classes of Document used for testing
	/////////////////////////////////////////////

	public static class PlayLine extends Document {
        public PlayLine() {
        		init__PlayLine((Integer)null, (String)null);
		  }


		public PlayLine(String _text_entry) {
        	this.init__PlayLine((Integer)null, _text_entry);
        }

		public PlayLine(Integer _line_id, String _text_entry) {
			this.setIdWithoutType(Integer.toString(_line_id));
        	this.setLongDescription(_text_entry);
		}

		private void init__PlayLine(Integer _line_id, String _text_entry) {
        	this.type = "playline";
        	if (_line_id != null) {
				this.setIdWithoutType(Integer.toString(_line_id));
			}
			this.setLongDescription(_text_entry);
		}

	}

	public static class ShowCharacter extends Document {
		public String firstName = null;
		public String surname = null;
		public String show = null;
		public Integer age = null;
		public ShowCharacter() {
			init_ShowCharacter(null, null, null);
		}
		public ShowCharacter(String _firstName, String _surname, String _show) {
			super();
			init_ShowCharacter(_firstName, _surname, _show);
		}
		private void init_ShowCharacter(String _firstName, String _surname, String _show) {
			this.firstName = _firstName;
			this.surname = _surname;
			this.type = "character";
			this.setIdWithoutType(_firstName+_surname);
		}
		public ShowCharacter setAge(Integer _age) {
			this.age = _age;
			return this;
		}
	}

	public static class TVShow extends Document {
		public String name = null;
		public TVShow() {
			init__TVShow(null);
		}
		public TVShow(String _name) {
			init__TVShow(_name);
		}

		private void init__TVShow(String _name) {
			this.name= _name;
			this.setIdWithoutType(name);
			this.type = "show";
		}
	}


	public static class SimpleDoc extends Document {
		public String id = null;
		public String content = null;
		public String category = null;

		public SimpleDoc() {
		}

		public SimpleDoc(String _id, String _content) {
			initialize(_id, _content, null);
		}

		public SimpleDoc(String _id, String _content, String _category) {
			initialize(_id, _content, _category);
		}

		public void initialize(String _id, String _content, String _category) {
			this.id = _id;
			this.content = _content;
			this.category = _category;
		}
	}

	public static class CarModel extends Document {
		public String maker = null;
		public String model = null;
		public Integer year = null;
		public String modelNumber = null;

		public CarModel() {
			init__CarModel(null);
		}

		public CarModel(String _modelNumber) {
			init__CarModel(_modelNumber);
		}

		private void init__CarModel(String _modelNumber) {
			this.type = "car-model";
			this.modelNumber = _modelNumber;
			this.setIdWithoutType(_modelNumber);
		}


		public CarModel setMaker(String _maker) {
			this.maker = _maker;
			return this;
		}

		public CarModel setModel(String _model) {
			this.model = _model;
			return this;
		}

		public CarModel setYear(int year) {
			this.year = year;
			return this;
		}
	}
}
