package ca.nrc.dtrc.elasticsearch.es5;

import org.junit.jupiter.api.*;

import java.net.URL;

public class StreamlinedClientObserverTest {

	private StreamlinedClient esClient;
	private static final String typePerson = "Person";
	private static final String typeShow = "Show";

	public static class ObserverDummy extends StreamlinedClientObserver {

		public int numObserved = 0;
		private boolean silent = false;

		public ObserverDummy() {
			super();
		}

		public ObserverDummy(String... typesToObserve) {
			super(typesToObserve);
		}

		@Override
		protected void onBulkIndex(int fromLine, int toLine, String indexName,
			String docTypeName) {}

		@Override
		protected void beforePUT(URL url, String json) {}

		@Override
		protected void afterPUT(URL url, String json) {}

		@Override
		protected void beforePOST(URL url, String json) {}

		@Override
		protected void afterPOST(URL url, String json) {}

		@Override
		protected void beforeGET(URL url) {}

		@Override
		protected void afterGET(URL url) {}

		@Override
		protected void beforeDELETE(URL url, String json) {}

		@Override
		public void afterDELETE(URL url, String json) {
			numObserved++;
			if (!silent) {
				System.out.println("-- ObserverDummy.afterDELETE: url=" + url + ", json=" + json);
			}
		}

		@Override
		public void observeBeforeHEAD(URL url, String json) throws ElasticSearchException {

		}

		@Override
		public void observeAfterHEAD(URL url, String json) throws ElasticSearchException {

		}

	}

	@BeforeEach
	public void setUp() throws Exception {
		this.esClient = new StreamlinedClient("test_index");
		esClient.putDocument(typePerson, new Document("Homer"));
		esClient.putDocument(typePerson, new Document("Marge"));
		esClient.putDocument(typeShow, new Document("The Simpsons"));
	}

	/////////////////////////////////////////////
	// DOCUMENTATION TESTS
	/////////////////////////////////////////////

	@Test
	public void test__StreamlinedClientObserver__Synopsis() throws Exception {
		// To observe what's going on with a particular ES index, attach a
		// StreamlinedClientObserver to its StreamlinedClient
		//
		// For example, this observer will echo back any DELETE requests
		//
		StreamlinedClientObserver observer = new ObserverDummy();
		esClient.attachObserver(observer);
		esClient.deleteDocumentWithID("Homer", typePerson);

		// If you want the observer to only apply to certain ES types, you can
		// provide them at construction time
		//
		esClient.detachObservers();
		String[] typesToObserve = new String[]{typeShow};
		esClient.attachObserver(new ObserverDummy(typesToObserve));

		// The observer will NOT be invoked by deletion of "Marge" because it does
		// not affect the typeShow type. However, it WILL be invoked by the
		// deletion of "The Simpsons" because that request does affect the
		// typeShow type
		//
		esClient.deleteDocumentWithID("Marge", typePerson);
		esClient.deleteDocumentWithID("The Simpsons", typeShow);
	}

	/////////////////////////////////////////////
	// VERIFICATION TESTS
	/////////////////////////////////////////////

	@Test
	public void test__StreamlinedClientObserver__HappyPath() throws Exception {
		ObserverDummy observer = new ObserverDummy(typeShow);
		observer.silent = true;
		esClient.attachObserver(observer);

		String id = "Homer";
		String type = typePerson;
		esClient.deleteDocumentWithID(id, type);
		Assertions.assertEquals(
		0, observer.numObserved,
		"Deletion of '"+id+"' from type '"+type+"' should NOT have been observed");


		id = "The Simpsons";
		type = typeShow;
		esClient.deleteDocumentWithID(id, typeShow);
		Assertions.assertEquals(
	1, observer.numObserved,
		"Deletion of '"+id+"' from type '"+type+"' SHOULD have been observed");

		// If you want the observer to only apply to certain ES types, you can
		// provide them at construction time
		//
		esClient.detachObservers();
		String[] typesToObserve = new String[]{typeShow};
		esClient.attachObserver(new ObserverDummy(typesToObserve));

		// This will not invoke the observer because it does not affect the
		// typePerson type
		//
		esClient.deleteDocumentWithID("Homer Simpson", Document.class);
	}

	@Test
	public void test__type4URL__HappyPath() throws Exception {
		ObserverDummy observer = new ObserverDummy();
		URL url = new URL("https://localhost/test_index/sometype");
		String gotType = observer.type4URL(url);
		Assertions.assertEquals("sometype", gotType);
	}
}
