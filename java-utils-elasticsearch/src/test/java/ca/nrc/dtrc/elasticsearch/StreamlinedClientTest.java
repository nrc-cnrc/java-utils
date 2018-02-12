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
		}
		
		public Person setBirthDay(String bDay) {
			birthDay = bDay;
			return this;
		} 

		@Override
		public String getKeyFieldName() {return "firstName";}

		@Override
		public String getKey() {return firstName;}
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
	public void test__Synopsis() throws Exception {
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
		String jsonResponse = client.putDocument(new Person("Homer", "Simpson"));
		jsonResponse = client.putDocument(new Person("Marg", "Simpson"));
		jsonResponse = client.putDocument(new Person("Moe", "Szyslak"));
		
		// Loop through all documents in the index
		SearchResults<Person> results = client.listAll(personPrototype);
		Iterator<Pair<Person,Double>> iter = results.iterator();
		while (iter.hasNext()) {
			Person person = iter.next().getFirst();
		}
		
		// Get a specific document by its ID
		Person person = (Person) client.getDocumentWithID("Homer", Person.class);
		
		
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
			Pair<Person,Double> scoredHit = iter.next();
			Person hitDocument = scoredHit.getFirst();
			Double hitScore = scoredHit.getSecond();
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
		
		// You can then scroll through the hits as described above...
		
		// In all of the above, we were using "statically typed" documents whose fields and 
		// structure were known at compile time (ex: Person).
		//
		// But you can also use "dynamically typed" documents whose fields are not determined
		// at compile time.
		// 
		// Here is how you do this.
		//   - First, you define the dynamic document
//		String idFieldName = "part_number";		
//		Document_DynTyped doc = new Document_DynTyped(idFieldName, "X18D98KL9");
//		doc.setField("name", "6in screw");
//		doc.setField("weight_grams", 0.4);
		
		// Next, you add the document to ES. Note that contrarily to statically typed
		// docs, you need to specify a document type for ES to store the doc under
//		String docType = "ca.nrc.dtrc.Part";
//		client.putDocument(docType, doc);
		
		// In general, you always need to specify an ES document type name 
		// to search, put, retrieve, etc... dynamically typed docs in ES
		// For example...
		//
//		Document_DynTyped retrievedDoc = (Document_DynTyped) client.getDocumentWithID("X18D98KL9", docType);
//		
//		Document_DynTyped 
			
	}
	
	/*********************************
	 * VERIFICATINO TESTS
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
	public void test__moreLikeThis__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);

		ESTestHelpers.PlayLine queryLine = new ESTestHelpers.PlayLine("Something is rotten in the kingdom of England");
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.moreLikeThis(queryLine, new IncludeFields("^text_entry$"));		
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "text_entry", gotSearchResults);
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
		Iterator<Pair<PlayLine,Double>> iter = hits.iterator();
		while (iter.hasNext() && hitsCount < 26) {
			Pair<ESTestHelpers.PlayLine,Double> scoredHit = iter.next();
			AssertHelpers.assertStringContains("Hit did not fit query.", scoredHit.getFirst().text_entry, "say", false);
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
		Iterator<Pair<T, Double>> iter = gotHits.iterator();
		while (iter.hasNext()) {
			T aHit = iter.next().getFirst();
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
				"{\"query\":{\"more_like_this\":{\"min_term_freq\":1,\"max_query_terms\":12,\"fields\":[\"firstName\",\"surname\"],\"like\":{\"_index\":\"es-test\",\"_type\":\"ca.nrc.dtrc.elasticsearch.StreamlinedClientTest$Person\",\"doc\":{\"firstName\":\"homer\",\"surname\":\"simpson\"}}}}}";
		AssertHelpers.assertStringEquals(expJson, gotJson);
	}
	
	@Test
	public void test__filterFields__ObjectWithPositiveFilter() throws Exception {
		IncludeFields filter = new IncludeFields("firstName");
		Person pers = new Person("homer", "simpson");
		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(pers, filter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		expFilteredFields.put("firstName", "homer");
		AssertHelpers.assertDeepEquals("Positive filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__ObjectWithNegativeFilter() throws Exception {
		ExcludeFields filter = new ExcludeFields("firstName");
		Person pers = new Person("homer", "simpson");
		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(pers, filter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("surname", "simpson");
			expFilteredFields.put("birthDay", null);
		}
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__ObjectWithNullFilter() throws Exception {
		ExcludeFields nullFilter = null;
		Person pers = new Person("homer", "simpson");
		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(pers, nullFilter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("firstName", "homer");
			expFilteredFields.put("surname", "simpson");
			expFilteredFields.put("birthDay", null);
		}
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	
	@Test
	public void test__filterFields__MapWithPositiveFilter() throws Exception {
		IncludeFields filter = new IncludeFields("firstName");
		Map<String,Object> map = new HashMap<String,Object>();
		{
			map.put("firstName", "homer");
			map.put("surname", "simpson");
		}
		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(map, filter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		expFilteredFields.put("firstName", "homer");
		AssertHelpers.assertDeepEquals("Positive filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__MapWithNegativeFilter() throws Exception {
		ExcludeFields filter = new ExcludeFields("firstName");
		Map<String,Object> map = new HashMap<String,Object>();
		{
			map.put("surname", "simpson");
			map.put("firstName", "homer");
		}
		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(map, filter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		expFilteredFields.put("surname", "simpson");
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__MapWithNullFilter() throws Exception {
		ExcludeFields nullFilter = null;
		Map<String,Object> map = new HashMap<String,Object>();
		{
			map.put("firstName", "homer");
			map.put("surname", "simpson");
		}
		Map<String, Object> gotFilteredFields = StreamlinedClient.filterFields(map, nullFilter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		expFilteredFields.put("firstName", "homer");
		expFilteredFields.put("surname", "simpson");
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
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
		AssertHelpers.assertDeepEquals("Homer SHOULD have been in the index after being added", corolla2009, gotCar);
	}
	
	
	@Test
	public void test__getFieldTypes__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();

		// First, Put a person with an empty date of birth;
		Person homer = new Person("Homer", "Simpson");
		client.putDocument(homer);
		
		Map<String,String> expFieldTypes = new HashMap<String,String>();
		{
			expFieldTypes.put("firstName", "text");
			expFieldTypes.put("surname", "text");
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

	/*************************
	 * TEST HELPERS
	 *************************/
	
	private void sleepABit() throws InterruptedException {
		double sleepSeconds = 1.0;
		Thread.sleep(Math.round(sleepSeconds * 1000));
	}
	
	private void assertUnscoredHitsAre(String message, Document[] expUnscoredHits, SearchResults gotSearchResults) throws IOException {
		List<Document> gotUnscoredHits = new ArrayList<Document>();
		Iterator<Pair<Document,Double>> iter = gotSearchResults.iterator();
		while (iter.hasNext()) {
			Pair<Document,Double> scoredHit = iter.next();
			gotUnscoredHits.add(scoredHit.getFirst());
		}
		
		AssertHelpers.assertDeepEquals(message+"\nUnscored hits were not as expected", 
				expUnscoredHits, gotUnscoredHits);
		// TODO Auto-generated method stub
		
	}

}
