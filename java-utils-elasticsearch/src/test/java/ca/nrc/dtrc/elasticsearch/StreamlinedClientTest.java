package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.datastructure.Pair;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers.PlayLine;
import ca.nrc.dtrc.elasticsearch.ExcludeFields;
import ca.nrc.dtrc.elasticsearch.IncludeFields;
import ca.nrc.dtrc.elasticsearch.StreamlinedClient;
import ca.nrc.testing.AssertHelpers;

public class StreamlinedClientTest {
	
	final static String PERSON_TYPE = "person";	
	final static Person personPrototype = new Person();

	public static class Person extends Document {
		public String firstName;
		public String surname;
		public String birthDay;

		public Person() {};
		
		public Person(String _firstName, String _surname) {
			this.firstName = _firstName;
			this.surname = _surname;
			this.setId(_firstName);
		}
		
		public Person setBirthDay(String bDay) {
			birthDay = bDay;
			return this;
		} 

		@Override
		public String keyFieldName() {return "firstName";}

		@Override
		public String getId() {return firstName;}
	}	
	
	public static class Movie {
		public String title;
		public String synopsis;

		public Movie() {};
		
		public Movie(String _title, String _synopsis) {
			this.title = _title;
			this.synopsis = _synopsis;
		}
	}
	
	private static boolean skipTests = false;
	
	@Before
    public void setUp() throws Exception {
        ESTestHelpers.skipTestsUnlessESIsRunning();
    }
	
	/*********************************
	 * DOCUMENTATION TESTS
	 *********************************/
	
	@Test
	public void test__StreamlinedClient__Synopsis() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = new StreamlinedClient(indexName);

		// By default, the server is on:
		// 
		//   Server : localhost
		//   Port   : 9200
		//
		// But you can change those like this:
		//
		//	client.setServer("www.somewhere.com")
		//		.setPort(9400);
		

		// You can then use the client to do all sorts of operations
		// on the index.
		//
		// For example, add some objects to the index...
		//
		Person homer = new Person("Homer", "Simpson");
		String jsonResponse = client.putDocument(homer);
		jsonResponse = client.putDocument(new Person("Marg", "Simpson"));
		jsonResponse = client.putDocument(new Person("Moe", "Szyslak"));
		
		// Loop through all documents in the index
		SearchResults<Person> results = client.listAll(personPrototype);
		Iterator<Hit<Person>> iter = results.iterator();
		while (iter.hasNext()) {
			Person person = iter.next().getDocument();
		}
		
		// Get a specific document by its ID
		Person person = (Person) client.getDocumentWithID("Homer", Person.class);
		
		// Delete a document with a specific ID
		client.deleteDocumentWithID("Homer", Person.class);
		
		
		//
		// Search for some objects. You can specify an arbitrary query as a JSON string
		//
		String jsonQuery = 
				"{\n"
				+ "  \"query\": {\"query_string\": {\"query\": \"Homer\"}}\n"
				+ "}"
				;
		SearchResults<Person> hits = client.search(jsonQuery, personPrototype);
		
		// Total number of hits available
		Long totalHits = hits.getTotalHits();
		
		// Scroll through list of scored hits
		iter = hits.iterator();
		while (iter.hasNext()) {
			Hit<Person> scoredHit = iter.next();
			Person hitDocument = scoredHit.getDocument();
			Double hitScore = scoredHit.getScore();
		}		
		
		//
		// There are also some more streamlined search methods. For example,
		// use this method to search all fields for a given free-form 
		// query.
		//
		String query = "(lisa OR marge) AND simpson";
		hits = client.searchFreeform(query, personPrototype);
		
		// 
		// Find documents that are similar to a particular doc.
		//
		Person queryPerson = new Person("Lisa", "Simpson");
		SearchResults<Person> searchResults = client.moreLikeThis(queryPerson);
		
		//
		// You can also find documents that are similar to a list of
		// documents.
		//
		List<Person> queryPeople = new ArrayList<Person>();
		queryPeople.add(new Person("Lisa", "Simpson"));
		queryPeople.add(new Person("Bart", "Simpson"));
		searchResults = client.moreLikeThese(queryPeople);
		
		// You can then scroll through the hits as described above...
		
		//
		// You can cluster a set of documents.
		// The  set of documents to be clustered is specified by a free-form query.
		// 
		// For this example, we will use a streamlined client that is connected
		// to an index containing all the lines from Shakespear's play 'Hamlet'
		//
		// We will cluster all the lines that are spoken by Hamlet
		//
		StreamlinedClient hamletClient = ESTestHelpers.makeHamletTestClient();
		query = "speaker:Hamlet";
		Integer maxDocs = 1000; // Only cluster the first 1000 hits. We recommend no less than 100
		
		// Specify the clustering algorithm.
		//
		// Possible values are: lingo (default), stc and kmeans.
		//
		// For details on each algorithm, see:
		//    http://doc.carrot2.org/#section.advanced-topics.fine-tuning.choosing-algorithm
		//
		String algName = "kmeans";
		
		String esDocTypeName = new PlayLine().getClass().getName();
		String[] useFields = new String[] {"text_entry"};
		DocClusterSet clusters = hamletClient.clusterDocuments(query, esDocTypeName, useFields, algName, maxDocs);
		
		// You can then look at the various clusters...
		for (String clusterLabel: clusters.getClusterNames()) {
			DocCluster aCluster = clusters.getCluster(clusterLabel);
			// Get some info about the cluster
			Set<String> idsOfDocsInCluster = aCluster.getDocIDs();
			int size = aCluster.getSize();
			// and so on...
		}
		
		// In all of the above, we were using "statically typed" documents whose fields and 
		// structure were known at compile time (ex: Person).
		//
		// But you can also use "dynamically typed" documents whose fields are not determined
		// at compile time.
		// 
		// Here is how you do this.
		//   - First, you define the dynamic document
		//
		String idFieldName = "part_number";		
		Document_DynTyped doc = new Document_DynTyped(idFieldName, "X18D98KL9");
		doc.setField("name", "6in screw");
		doc.setField("weight_grams", 0.4);
		
		// Next, you add the document to ES. Note that contrarily to statically typed
		// docs, you need to specify a document type for ES to store the doc under
		String docType = "ca.nrc.dtrc.Part";
		client.putDocument(docType, doc);
		
		// In general, you always need to specify an ES document type name 
		// to search, put, retrieve, etc... dynamically typed docs in ES
		// For example...
		//
		Document_DynTyped retrievedDoc = (Document_DynTyped) client.getDocumentWithID("X18D98KL9", Document_DynTyped.class, docType);
	}
	
	/*********************************
	 * VERIFICATION TESTS
	 *********************************/
	
	@Test
	public void test__putDocument__nonExistantDoc() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();
		
		ESTestHelpers.assertIndexIsEmpty(client);

		final String PERSON = "person";
		String jsonResponse = client.putDocument(new Person("Homer", "Simpson"));
		ESTestHelpers.assertNoError(jsonResponse);
		
		jsonResponse = client.putDocument(new Person("Marg", "Simpson"));
		
		String jsonQuery = 
				"{\n"
				+ "  \"query\": {\"query_string\": {\"query\": \"Homer\"}}\n"
				+ "}"
				;
		
		SearchResults gotResults = client.search(jsonQuery, personPrototype);
				
		Person[] expHits = new Person[] {
				new Person("Homer", "Simpson")
		};
		
		assertUnscoredHitsAre("", expHits, gotResults);
	}	
	
	
	@Test
	public void test__deleteDocument__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();
		
		ESTestHelpers.assertIndexIsEmpty(client);

		final String PERSON = "person";
		Person homer = new Person("Homer", "Simpson");
		String jsonResponse = client.putDocument(homer);
		Person marg = new Person("Marg", "Simpson");
		jsonResponse = client.putDocument(marg);
		
		List<Person> gotPeople = client.listFirstNDocuments(homer, 3);
		
		Person[] expPeople = new Person[] {homer, marg};
		AssertHelpers.assertDeepEquals("", expPeople, gotPeople);
		
		client.deleteDocumentWithID(homer.getId(), Person.class.getName());		
		expPeople = new Person[] {marg};
		Thread.sleep(500);
		gotPeople = client.listFirstNDocuments(homer, 3);
		AssertHelpers.assertDeepEquals("Homer was not properly deleted from the index.", expPeople, gotPeople);
		
				
		client.deleteDocumentWithID(homer.getId(), Person.class.getName());		
		AssertHelpers.assertDeepEquals("There was a problem when deleting the same document twice", expPeople, gotPeople);

	}	

	@Test
	public void test__moreLikeThis__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);

		ESTestHelpers.PlayLine queryLine = new ESTestHelpers.PlayLine("Something is rotten in the kingdom of England");
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.moreLikeThis(queryLine, new IncludeFields("^text_entry$"));		
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "text_entry", gotSearchResults);
	}	
	
	@Test
	public void test__moreLikeThese__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);

		List<ESTestHelpers.PlayLine> queryLines = new ArrayList<ESTestHelpers.PlayLine>();
		
		queryLines.add(new ESTestHelpers.PlayLine("Something is rotten in the kingdom of England"));
		queryLines.add(new ESTestHelpers.PlayLine("To sing or not to sing that is the question"));
		
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.moreLikeThese(queryLines, new IncludeFields("^text_entry$"));		
		assertIsInFirstNHits("I must to England; you know that?", 20, "text_entry", gotSearchResults);
		assertIsInFirstNHits("To be, or not to be: that is the question:", 20, "text_entry", gotSearchResults);
	}	
	
	
	@Test
	public void test__searchFreeform__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);

		String query = "denmark AND rotten";
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.searchFreeform(query, new PlayLine());		
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "text_entry", gotSearchResults);
	}		
	

	@Test
	public void test__scrollThroughSearchResults__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);
		final String PLAY_LINE = "line";

		ESTestHelpers.PlayLine queryLine = new ESTestHelpers.PlayLine("say");
		SearchResults<ESTestHelpers.PlayLine> hits = client.moreLikeThis(queryLine, new IncludeFields("^text_entry$"));
		
		int hitsCount = 0;
		Iterator<Hit<PlayLine>> iter = hits.iterator();
		while (iter.hasNext() && hitsCount < 26) {
			Hit<ESTestHelpers.PlayLine> scoredHit = iter.next();
			AssertHelpers.assertStringContains("Hit did not fit query.", scoredHit.getDocument().text_entry, "say", false);
			hitsCount++;
		}
		assertTrue("List of hits should have contained at least 25 hits, but only contained "+hitsCount, hitsCount >= 25);
	}		

	
	private <T extends Document> void assertIsInFirstNHits(Object expValue, int nHits, String fieldName, SearchResults<T> gotSearchResults) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		List<Object> gotFields = fieldForFirstNHits(nHits, fieldName, gotSearchResults);
		Set<Object> gotFieldsSet = new HashSet<Object>();
		gotFieldsSet.addAll(gotFields);
		Assert.assertTrue(
				"Values of field "+fieldName+" for the first "+nHits+" hits did not contain expected value of "+expValue
			    +".\nValues of first hits were: "+gotFields,
				gotFieldsSet.contains(expValue));
		
	}
	
	/***********************************************************
	 * TEST HELPERS
	***********************************************************/

	private <T extends Document> List<Object> fieldForFirstNHits(int nHits, String fieldName, SearchResults<T> gotHits) 
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		List<Object> gotValues = new ArrayList<Object>();
		Iterator<Hit<T>> iter = gotHits.iterator();
		while (iter.hasNext()) {
			T aHit = iter.next().getDocument();
			if (nHits <= 0) break;
			Object fldValue = null;
			if (aHit instanceof Map<?,?>) {
				Map<String,Object> aHitMap = (Map<String, Object>) aHit;
				fldValue = (Object) aHitMap.get(fieldName);
			} else {
				Field fld = aHit.getClass().getDeclaredField(fieldName);
				fld.setAccessible(true);
				fldValue = (Object) fld.get(aHit);		
			}
			gotValues.add(fldValue);
			nHits--;
		}	
				
		return gotValues;
	}
	
	private <T> void assertHitFieldsEqual(String fieldName, Object[] expFieldValuesArray, List<T> hits, Integer onlyNHits) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
		List<Object> gotValues = new ArrayList<Object>();
		for (T aHit: hits) {
			if (onlyNHits <= 0) break;
			Object fldValue = null;
			if (aHit instanceof Map<?,?>) {
				Map<String,Object> aHitMap = (Map<String, Object>) aHit;
				fldValue = (Object) aHitMap.get(fieldName);
			} else {
				Field fld = aHit.getClass().getDeclaredField(fieldName);
				fld.setAccessible(true);
				fldValue = (Object) fld.get(aHit);		
			}
			gotValues.add(fldValue);
			onlyNHits--;
		}
		List<Object> expValues = new ArrayList<Object>();
		for (int ii=0; ii < expFieldValuesArray.length; ii++) {
			expValues.add(expFieldValuesArray[ii]);
			int x = 0;
		}
		
		AssertHelpers.assertDeepEquals("Values of field "+fieldName+" were not as expected", expValues, gotValues);		
	}
	
	@Test
	public void test__moreLikeThisJsonBody__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();
		
		
		Person homer = new Person("homer", "simpson");
		
		// Note: We need to put this document into the empty test index
		//   otherwise moreLikeThisJsonBody won't' be able to ask
		//   ElasticSearch what types it automatically assigned to
		//   the various fields.
		client.putDocument(homer);
		
		Map<String, Object> homerMap = new ObjectMapper().convertValue(homer, Map.class);
		String gotJson = client.moreLikeThisJsonBody(homer.getClass().getName(), homerMap);
		String expJson = 
//<<<<<<< HEAD
//				"{\"query\":{\"more_like_this\":{\"min_term_freq\":1,\"min_doc_freq\":1,\"max_query_terms\":12,\"fields\":[\"lang\",\"firstName\",\"surname\"],\"like\":{\"_index\":\"es-test\",\"_type\":\"ca.nrc.dtrc.elasticsearch.StreamlinedClientTest$Person\",\"doc\":{\"lang\":\"en\",\"firstName\":\"homer\",\"surname\":\"simpson\"}}}},\"highlight\":{\"order\":\"score\",\"fields\":{\"description\":{\"type\":\"plain\"},\"short_desc\":{\"type\":\"plain\"}}}}";
//=======
				"{\"query\":{\"more_like_this\":{\"min_term_freq\":1,\"min_doc_freq\":1,\"max_query_terms\":12,\"fields\":[\"lang\",\"id\",\"firstName\",\"surname\"],\"like\":{\"_index\":\"es-test\",\"_type\":\"ca.nrc.dtrc.elasticsearch.StreamlinedClientTest$Person\",\"doc\":{\"lang\":\"en\",\"id\":\"homer\",\"firstName\":\"homer\",\"surname\":\"simpson\"}}}},\"highlight\":{\"order\":\"score\",\"fields\":{\"description\":{\"type\":\"plain\"},\"short_desc\":{\"type\":\"plain\"}}}}";
//>>>>>>> Refactoring Document class to add id field.
		AssertHelpers.assertStringEquals(expJson, gotJson);
	}
	
	@Test
	public void test__filterFields__ObjectWithPositiveFilter() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		IncludeFields filter = new IncludeFields("firstName");
		Person pers = new Person("homer", "simpson");
		esClient.putDocument(pers);
		Map<String, Object> gotFilteredFields = esClient.filterFields(pers, filter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		expFilteredFields.put("firstName", "homer");
		AssertHelpers.assertDeepEquals("Positive filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__ObjectWithNegativeFilter() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		ExcludeFields filter = new ExcludeFields("firstName");
		Person pers = new Person("homer", "simpson");
		esClient.putDocument(pers);
		Map<String, Object> gotFilteredFields = esClient.filterFields(pers, filter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("surname", "simpson");
			expFilteredFields.put("lang", "en");
		}
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__ObjectWithNullFilter() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		ExcludeFields nullFilter = null;
		Person pers = new Person("homer", "simpson");
		esClient.putDocument(pers);
		Map<String, Object> gotFilteredFields = esClient.filterFields(pers, nullFilter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("firstName", "homer");
			expFilteredFields.put("surname", "simpson");
//			expFilteredFields.put("birthDay", null);
//			expFilteredFields.put("_detect_language", true);
			expFilteredFields.put("lang", "en");
		}
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	
//	@Test
//	public void test__filterFields__MapWithPositiveFilter() throws Exception {
//		IncludeFields filter = new IncludeFields("firstName");
//		Map<String,Object> map = new HashMap<String,Object>();
//		{
//			map.put("firstName", "homer");
//			map.put("surname", "simpson");
//		}
//		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(map, filter);
//		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
//		expFilteredFields.put("firstName", "homer");
//		AssertHelpers.assertDeepEquals("Positive filter did not produce expected field names", expFilteredFields, gotFilteredFields);
//	}
//
//	@Test
//	public void test__filterFields__MapWithNegativeFilter() throws Exception {
//		ExcludeFields filter = new ExcludeFields("firstName");
//		Map<String,Object> map = new HashMap<String,Object>();
//		{
//			map.put("surname", "simpson");
//			map.put("firstName", "homer");
//		}
//		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(map, filter);
//		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
//		expFilteredFields.put("surname", "simpson");
//		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
//	}
//
//	@Test
//	public void test__filterFields__MapWithNullFilter() throws Exception {
//		ExcludeFields nullFilter = null;
//		Map<String,Object> map = new HashMap<String,Object>();
//		{
//			map.put("firstName", "homer");
//			map.put("surname", "simpson");
//		}
//		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(map, nullFilter);
//		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
//		expFilteredFields.put("firstName", "homer");
//		expFilteredFields.put("surname", "simpson");
//		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
//	}

	@Test
	public void test__filterFields__DynamicallyTypedDocument() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		ExcludeFields nullFilter = null;
		Document_DynTyped homer = new Document_DynTyped("name", "homer");
		homer.setField("birthDay", "1993-01-26");
		esClient.putDocument(homer);
		Map<String, Object> gotFilteredFields = esClient.filterFields(homer);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("fields.name", "homer");
			expFilteredFields.put("lang", "en");
			expFilteredFields.put("key", "homer");
		}
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}	
	
	@Test
	public void test__getFieldTypes__DynamicallyTypedDocsOnly() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		Document_DynTyped homer = new Document_DynTyped("name", "homer");
		homer.setField("birthDay", "1993-01-26");
		String type = "CartoonCharacters";
		esClient.putDocument(type, homer);
		
		Map<String, String> gotTypes = esClient.getFieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("id", "text");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("idFieldName", "text");
			expTypes.put("lang", "text");
			expTypes.put("fields.birthDay", "date");
			expTypes.put("fields.name", "text");
			expTypes.put("key", "text");
		}
		AssertHelpers.assertDeepEquals("Field types not as expected for type: "+type, 
				expTypes, gotTypes);
	}
	
	@Test
	public void test__getFieldTypes__StaticallyTypedDocsOnly() throws Exception {
		// This ES doc type will contain both statically typed and dynamically typed
		// documents representing people.
		String type = "CartoonCharacters";
		
		// This person is statically typed
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		Person homer = new Person("homer", "simpson").setBirthDay("1993-01-26");
		esClient.putDocument(type, homer);
		
		// This other person is dynamically typed
		Document_DynTyped marge = new Document_DynTyped("name", "marge");
		marge.setField("birthDay", "1993-01-26");
		esClient.putDocument(type, marge);
		
		// The field types should be the union of the types for both
		// the dynamically and statically typed docs
		//
		Map<String, String> gotTypes = esClient.getFieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("id", "text");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("lang", "text");
			expTypes.put("idFieldName", "text");
			expTypes.put("key", "text");

			expTypes.put("birthDay", "date");
			expTypes.put("fields.birthDay", "date");
			
			expTypes.put("firstName", "text");
			expTypes.put("surname", "text");
			expTypes.put("fields.name", "text");
			
		}
		AssertHelpers.assertDeepEquals("Field types not as expected for type: "+type, 
				expTypes, gotTypes);
	}	

	@Test
	public void test__getFieldTypes__MixedDynamicallyAndStaticallyTypedDocs() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		Person homer = new Person("homer", "simpson").setBirthDay("1993-01-26");
		String type = "CartoonCharacters";
		esClient.putDocument(type, homer);
		
		Map<String, String> gotTypes = esClient.getFieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("id", "text");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("lang", "text");
			expTypes.put("birthDay", "date");
			expTypes.put("firstName", "text");
			expTypes.put("surname", "text");
		}
		AssertHelpers.assertDeepEquals("Field types not as expected for type: "+type, 
				expTypes, gotTypes);
	}	


	@Test
	public void test__put_getDocument__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();
		
		Person gotPerson = (Person) client.getDocumentWithID("Homer", Person.class);
		AssertHelpers.assertDeepEquals("Homer should NOT have been in the index initially", null, gotPerson);		

		Person homer = new Person("Homer", "Simpson");
		client.putDocument(homer);
		
		gotPerson = (Person) client.getDocumentWithID("Homer", Person.class);
		AssertHelpers.assertDeepEquals("Homer SHOULD have been in the index after being added", homer, gotPerson);
	}

	@Test
	public void test__put_getDocument__DynTyped() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();
		
		String esDocType = "car-model";
		
		String modelID = "YTD24211";		
		Document_DynTyped gotCar = (Document_DynTyped) client.getDocumentWithID(modelID, Document_DynTyped.class, esDocType);
		AssertHelpers.assertDeepEquals("Car model "+modelID+" should NOT have been in the index initially", null, gotCar);
		
		Document_DynTyped corolla2009 = new Document_DynTyped("model-number", modelID);
		corolla2009.setField("maker", "Toyota");
		corolla2009.setField("model", "Corolla");
		corolla2009.setField("year", "2009");
		client.putDocument(esDocType, corolla2009);
		
		gotCar = (Document_DynTyped) client.getDocumentWithID(modelID, Document_DynTyped.class, esDocType);
		AssertHelpers.assertDeepEquals("Corolla have been in the index after being added", corolla2009, gotCar);
	}
	
	
	@Test
	public void test__getFieldTypes__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();

		// First, Put a person with an empty date of birth;
		Person homer = new Person("Homer", "Simpson");
		client.putDocument(homer);
		
		Map<String,Object> expFieldTypes = new HashMap<String,Object>();
		{
			expFieldTypes.put("id", "text");
			expFieldTypes.put("firstName", "text");
			expFieldTypes.put("surname", "text");
			expFieldTypes.put("_detect_language", "boolean");			
			expFieldTypes.put("lang", "text");			
		}
		Map<String,String> gotFieldTypes = client.getFieldTypes(Person.class);	
		AssertHelpers.assertDeepEquals("ElasticSearch types were wrong for fields of class "+Person.class, 
					expFieldTypes, gotFieldTypes);
		
		// Next, put a person with a birthday in YYYY-MM-DD format.
		// This should cause ES to change its internal type for the "birthDay"
		// field from "String" to "Date".
		Person marge = new Person("Marge", "Simpson").setBirthDay("1993-01-26");
		client.putDocument(marge);
		expFieldTypes.put("birthDay", "date");
		gotFieldTypes = client.getFieldTypes(Person.class);
		AssertHelpers.assertDeepEquals("ElasticSearch types were wrong for fields of class "+Person.class, 
				expFieldTypes, gotFieldTypes);		
	}
	
	@Test
	public void test__updateDocument__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();

		// First, Put a person with an empty date of birth;
		Person homer = new Person("Homer", "Simpson");
		client.putDocument(homer);
		
		// Then update his birth date
		Map<String,Object> partialDoc = new HashMap<String,Object>();
		partialDoc.put("birthDay", "1993-01-14");
		client.updateDocument(Person.class, "Homer", partialDoc);
		
		sleepABit();
		
		// Retrieve the doc from ES and it should now have a non-null
		// birthDay value
		Person gotPerson = (Person) client.getDocumentWithID("Homer", Person.class);
		Person expPerson = new Person("Homer", "Simpson").setBirthDay("1993-01-14");
		AssertHelpers.assertDeepEquals("Birthday was not updated in the ES index", 
				expPerson, gotPerson);
	}
	
	@Test
	public void test__clusterDocumentJsonBody__HappyPath() throws Exception {
		String indexName = "test-index";
		StreamlinedClient client = new StreamlinedClient(indexName);
		
		String[] useFields = new String[] {"text_entry"};
		String gotJson = client.clusterDocumentJsonBody("speaker:hamlet", "testdoc", useFields, "kmeans", 1000);
		String expJson = 
				"{\"search_request\":{"+
				  "\"_source\":[\"text_entry\"],"+
		          "\"query\":"+
				    "{\"query_string\":"+
		              "{\"query\":\"speaker:hamlet\"}"+
				    "},"+
		            "\"size\":1000"+
				  "},"+
		          "\"query_hint\":\"\","+
				  "\"algorithm\":\"kmeans\","+
				  "\"field_mapping\":{"+
		            "\"content\":[\"_source.text_entry\"]"+
		          "}"+
		        "}";
		AssertHelpers.assertStringEquals(expJson, gotJson);
	}
	
	@Test
	public void test__clusterDocuments__HappyPath() throws Exception {
		StreamlinedClient hamletClient = ESTestHelpers.makeHamletTestClient();
		String query = "speaker:Hamlet";
		Integer maxDocs = 1000; 
		String algName = "stc";
		String esDocTypeName = new PlayLine().getClass().getName();
		String[] useFields = new String[] {"text_entry"};
		DocClusterSet clusters = hamletClient.clusterDocuments(query, esDocTypeName, useFields, algName, maxDocs);
		
		String[] expClusterNamesSuperset = new String[] {
				"Other Topics", "Shall", "King", "Thou", "Sir", "Thee", "Know", 
				"Mother", "Speak", "Play", "Love", "Heaven", "Tis", "Horatio", "Father", "Soul",
				"Heaven", "Thy", "Eyes", "Matter"
		};
		Object[] gotClusterNames =  clusters.getClusterNames().toArray();
		
		// The algorithm does not seem to be completely deterministic and the exact
		// set of clusters returned is not always exactly the same from run to run.
		//
		// But they tend to be very similar, so we can just check to make sure that 
		// the clusters we get are a subset of the set of clusters we typically see.
		//
		// If this assertion fails, check if the new cluster that was generated makes sense, 
		// and if it does, then add it to the superset of expected clusters.
		//
		AssertHelpers.assertContainsAll("Cluster names not as expected", expClusterNamesSuperset, gotClusterNames);

		String clusterName = "Heaven";
		String[] expIDs = new String[] {
			"32778","32779","32820","33105","33156","33265","33355",
			"34135","34304", "34871","34879","34890","35071","36280",
			"36590"		
		};
		String[] gotIDs = clusters.getCluster("Heaven").getDocIDs().toArray(new String[]{});
		AssertHelpers.assertDeepEquals("Document IDs for cluster '"+clusterName+"' were not as expected.", 
				expIDs, clusters.getCluster("Heaven").getDocIDs());
		AssertHelpers.assertUnOrderedSameElements("Cluster IDs not as expected", expIDs, gotIDs);
	}

	/*************************
	 * TEST HELPERS
	 *************************/
	
	private void sleepABit() throws InterruptedException {
		double sleepSeconds = 1.0;
		Thread.sleep(Math.round(sleepSeconds * 1000));
	}
	
	private void assertUnscoredHitsAre(String message, Document[] expUnscoredHits, SearchResults gotSearchResults) throws IOException {
		List<Document> gotUnscoredHits = new ArrayList<Document>();
		Iterator<Hit<Document>> iter = gotSearchResults.iterator();
		while (iter.hasNext()) {
			Hit<Document> scoredHit = iter.next();
			gotUnscoredHits.add(scoredHit.getDocument());
		}
		
		AssertHelpers.assertDeepEquals(message+"\nUnscored hits were not as expected", 
				expUnscoredHits, gotUnscoredHits);
	}

}
