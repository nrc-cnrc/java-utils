package ca.nrc.dtrc.elasticsearch.crud;

import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;
import ca.nrc.dtrc.elasticsearch.index.AssertIndex;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import ca.nrc.testing.AssertObject;
import ca.nrc.testing.AssertString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class CrudAPITest {

	protected abstract ESFactory makeES(String indexName) throws Exception;
	protected abstract String expUrl4Doc(String type, String docID);
	protected abstract String expUrl4updateDoc(String type, String docID);

	protected abstract int esVersion();

	ESFactory esFactory = null;

	@BeforeEach
	public void setUp() throws Exception {
		esFactory = makeES("es-test");
	}

	////////////////////////////////////////////
	// DOCUMENTATION TESTS
	////////////////////////////////////////////

	@Test
	public void test__CrudAPI__Synopsis() throws Exception {
		// Use CrudAPI for creating/reading/updating/deleting ESFactory documents
		// You should always obtain a CrudAPI from a concrete ESFactory instance that is
		// designed to interact with the version of ESFactory you are running.
		String indexName = "es-test";
		ESFactory es = makeES(indexName);
		CrudAPI crud = es.crudAPI();

		// This is how you CREATE a document
		ShowCharacter homer = new ShowCharacter("Homer", "Simpson", "The Simpsons");
		crud.putDocument(homer);

		// This is how you READ a document
		ShowCharacter gotHomerBack =
			crud.getDocumentWithID(homer.getId(), ShowCharacter.class);

		// This is how you UPDATE a document
		homer.age = 42;
		crud.updateDocument(homer);

		// This is how you DELETE a document
		crud.deleteDocumentWithID(homer.getId());
	}

	////////////////////////////////////////////
	// VERIFICATION TESTS
	////////////////////////////////////////////

	@Test
	public void test__put_getDocument__HappyPath() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeEmptyTestIndex();
		CrudAPI crudAPI = esFactory.crudAPI();

		ShowCharacter gotPerson = crudAPI
			.getDocumentWithID("FredFlintstone", ShowCharacter.class);
		AssertObject.assertDeepEquals(
			"Fred should NOT have been in the index initially",
			null, gotPerson);

		ShowCharacter fred = new ShowCharacter("Fred", "Flintstone", "The Flintstones");
		crudAPI.putDocument(fred);

		gotPerson = crudAPI
			.getDocumentWithID("FredFlintstone", ShowCharacter.class);
		AssertObject.assertDeepEquals(
			"Fred SHOULD have been in the index after being added",
			fred, gotPerson);
	}

	@Test
	public void test__put_getDocument__DynTyped() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeEmptyTestIndex();
		CrudAPI crudAPI = esFactory.crudAPI();

		String esDocType = "car-model";

		String modelID = "YTD24211";
		CarModel gotCar = (CarModel) crudAPI.getDocumentWithID(modelID, CarModel.class, esDocType);
		AssertObject.assertDeepEquals("Car model "+modelID+" should NOT have been in the index initially", null, gotCar);

		CarModel corolla2009 =new CarModel(modelID);
		corolla2009.setMaker("Toyota");
		corolla2009.setModel("Corolla");
		corolla2009.setYear(2009);
		crudAPI.putDocument(esDocType, corolla2009);

		gotCar = (CarModel) crudAPI.getDocumentWithID(modelID, CarModel.class, esDocType);
		AssertObject.assertDeepEquals("Corolla have been in the index after being added", corolla2009, gotCar);
	}

	@Test
	public void test__put_getDocument__DocIDContainsSlash() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeEmptyTestIndex();
		CrudAPI crudAPI = esFactory.crudAPI();

		String esDocType = "car-model";

		String modelID = "YTD24211/x";
		CarModel gotCar = crudAPI.getDocumentWithID(modelID, CarModel.class, esDocType);
		AssertObject.assertDeepEquals("Car model "+modelID+" should NOT have been in the index initially", null, gotCar);

		CarModel corolla2009 = new CarModel(modelID);
		corolla2009.setModel("Toyota");
		corolla2009.setMaker("Corolla");
		corolla2009.setYear(2009);
		esFactory.crudAPI().putDocument(esDocType, corolla2009);

		gotCar = (CarModel) crudAPI.getDocumentWithID(modelID, CarModel.class, esDocType);
		AssertObject.assertDeepEquals("Corolla have been in the index after being added", corolla2009, gotCar);
	}

	@Test
	public void test__getDocumentWithID__NonExistantIndex() throws Exception {
		esFactory = makeES("nonexistant_index");
		new AssertIndex(esFactory).doesNotExist();

		Assertions.assertThrows(NoSuchIndexException.class, () ->
			{
				esFactory.crudAPI().getDocumentWithID("someid", ShowCharacter.class);
			},
			"Getting a doc from non-existand ID should raise exception "+
			"(unless we pass an argument asking otherwise)");

		ShowCharacter gotDoc =
			esFactory.crudAPI().getDocumentWithID("someid", ShowCharacter.class, false);
		Assertions.assertNull(gotDoc,
			"Retrieving a doc from non-existant index should return null if we provide an argument asking not to fail");
	}


	@Test
	public void test__putDocument__nonExistantDocs() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeEmptyTestIndex();
		CrudAPI crudAPI = esFactory.crudAPI();

		final String PERSON = "person";
		Person homer = new Person("Homer", "Simpson");
		crudAPI.putDocument(homer);
		Person marge = new Person("Marg", "Simpson");
		crudAPI.putDocument(marge);
		Thread.sleep(2*1000);

		Person[] expHits = new Person[] {homer, marge};
		new AssertIndex(esFactory)
			.docsInTypeEqual(expHits);
	}

	@Test
	public void test__updateDocument__HappyPath() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeEmptyTestIndex();
		CrudAPI crudAPI = esFactory.crudAPI();

		// First, Put a person with an empty date of birth;
		Person homer = new Person("Homer", "Simpson");
		crudAPI.putDocument(homer);

		// Then update his birth date
		Map<String,Object> partialDoc = new HashMap<String,Object>();
		partialDoc.put("birthDay", "1993-01-14");
		crudAPI.updateDocument(Person.class, "HomerSimpson", partialDoc);

		esFactory.sleep(1.0);

		// Retrieve the doc from ESFactory and it should now have a non-null
		// birthDay value
		Person gotPerson = (Person) crudAPI.getDocumentWithID("HomerSimpson", Person.class);
		Person expPerson = new Person("Homer", "Simpson").setBirthDay("1993-01-14");
		AssertObject.assertDeepEquals("Birthday was not updated in the ESFactory index",
				expPerson, gotPerson);
	}

	@Test
	public void test__deleteDocument__HappyPath() throws Exception {
		esFactory = new ESTestHelpers(esVersion()).makeCartoonTestIndex();
		CrudAPI crudAPI = esFactory.crudAPI();
		IndexAPI indexAPI = esFactory.indexAPI();

		ShowCharacter homer = new ShowCharacter("Homer", "Simpson", "The Simpsons");
		SearchResults<ShowCharacter> gotPeople = indexAPI.listAll(homer);
		new AssertSearchResults(gotPeople, "Initial set of people not as expected")
			.containsIDs(homer.getId());

		crudAPI.deleteDocumentWithID(homer.getId(), ShowCharacter.class);
		Thread.sleep(1*1000);
		gotPeople = indexAPI.listAll(homer);
		new AssertSearchResults(gotPeople, "Initial set of people not as expected")
			.doesNotcontainIDs(homer.getId());
	}

	@Test
	public void test__url4doc() throws Exception {
		CrudAPI crudAPI =
			new ESTestHelpers(esVersion()).makeEmptyTestIndex().crudAPI();
		String type = "sometype";
		String docID = "somedoc";
		URL gotURL = crudAPI.url4doc(type, docID);
		AssertString.assertStringEquals(
			"URL for document access was not as expected",
			expUrl4Doc(type, docID), gotURL.toString()
		);
	}

	@Test
	public void test__url4updateDocument() throws Exception {
		CrudAPI crudAPI =
			new ESTestHelpers(esVersion()).makeEmptyTestIndex().crudAPI();
		String type = "sometype";
		String docID = "somedoc";
		URL gotURL = crudAPI.url4updateDocument(type, docID);
		AssertString.assertStringEquals(
			"URL for document updating was not as expected",
			expUrl4updateDoc(type, docID), gotURL.toString()
		);
	}
}
