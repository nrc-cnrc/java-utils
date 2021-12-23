package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import org.junit.jupiter.api.*;

import java.net.URL;

public abstract class ESObserverTest {

	protected abstract ESFactory makeESFactory(String indexName) throws ElasticSearchException;
	protected abstract int esVersion() throws ElasticSearchException;

	private ESFactory esFactory;
	private static final String typePerson = "Person";
	private static final String typeShow = "Show";

	public static class ObserverDummy extends ESObserver {

		public int numObserved = 0;
		private boolean silent = false;

		public ObserverDummy(ESFactory _esFactory, String... typesToObserve) throws ElasticSearchException {
			super(_esFactory, typesToObserve);
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
		esFactory = makeESFactory("test_index");
		CrudAPI crudAPI = esFactory.crudAPI();
		crudAPI.putDocument(typePerson, new Document("Homer", "person"));
		crudAPI.putDocument(typePerson, new Document("Marge", "person"));
		crudAPI.putDocument(typeShow, new Document("The Simpsons", "person"));
	}

	/////////////////////////////////////////////
	// DOCUMENTATION TESTS
	/////////////////////////////////////////////

	@Test
	public void test__ESObserver__Synopsis() throws Exception {
		// To observe what's going on with a particular ESFactory index, attach a
		// ESObserver to its StreamlinedClient_v5
		//
		// For example, this observer will echo back any DELETE requests
		//
		ESObserver observer = new ObserverDummy(esFactory);
		esFactory.attachObserver(observer);
		esFactory.crudAPI().deleteDocumentWithID("Homer", typePerson);

		// If you want the observer to only apply to certain ESFactory types, you can
		// provide them at construction time
		//
		esFactory.detachObservers();
		String[] typesToObserve = new String[]{typeShow};
		esFactory.attachObserver(new ObserverDummy(esFactory, typesToObserve));

		// The observer will NOT be invoked by deletion of "Marge" because it does
		// not affect the typeShow type. However, it WILL be invoked by the
		// deletion of "The Simpsons" because that request does affect the
		// typeShow type
		//
		esFactory.crudAPI().deleteDocumentWithID("Marge", typePerson);
		esFactory.crudAPI().deleteDocumentWithID("The Simpsons", typeShow);
	}

	/////////////////////////////////////////////
	// VERIFICATION TESTS
	/////////////////////////////////////////////

	@Test
	public void test__ESObserver__HappyPath() throws Exception {
		esFactory = new ESTestHelpers(esVersion()).makeCartoonTestIndex();

		ObserverDummy observer =
			new ObserverDummy(esFactory, Document.determineType(ESTestHelpers.TVShow.class));
		observer.silent = true;
		esFactory.attachObserver(observer);

		String id = "HomerSimpson";
		Class<? extends Document> docClass = ESTestHelpers.ShowCharacter.class;
		esFactory.crudAPI().deleteDocumentWithID(id, docClass);
		Assertions.assertEquals(
			0, observer.numObserved,
			"Deletion of '"+id+"' from class '"+docClass+"' should NOT have been observed");


		id = "The Simpsons";
		docClass = ESTestHelpers.TVShow.class;
		esFactory.crudAPI().deleteDocumentWithID(id, docClass);
		Assertions.assertEquals(
			1, observer.numObserved,
			"Deletion of '"+id+"' from type '"+docClass+"' SHOULD have been observed");

		// If you want the observer to only apply to certain ESFactory types, you can
		// provide them at construction time
		//
		esFactory.detachObservers();
		String[] typesToObserve = new String[]{typeShow};
		esFactory.attachObserver(new ObserverDummy(esFactory, typesToObserve));

		// This will not invoke the observer because it does not affect the
		// typePerson type
		//
		esFactory.crudAPI().deleteDocumentWithID(
			"Homer Simpson", ESTestHelpers.ShowCharacter.class);
	}

	@Test
	public void test__type4URL__HappyPath() throws Exception {
		ObserverDummy observer = new ObserverDummy(esFactory);
		URL url = new URL("https://localhost/test_index/sometype");
		String gotType = observer.type4URL(url);
		Assertions.assertEquals("sometype", gotType);
	}
}
