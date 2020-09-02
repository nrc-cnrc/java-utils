package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.nrc.dtrc.elasticsearch.request.BodyBuilder;
import ca.nrc.dtrc.elasticsearch.request.QueryBody;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.datastructure.Pair;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers.PlayLine;
import ca.nrc.introspection.Introspection;
import ca.nrc.file.ResourceGetter;
import ca.nrc.testing.AssertHelpers;

public class StreamlinedClientTest {
		
	final static String PERSON_TYPE = "person";	
	final static Person personPrototype = new Person();

	public static class Person extends Document {
		public String firstName;
		public String surname;
		public String birthDay;
		public String gender;
		public Integer age = 0;

		public Person() {};
		
		public Person(String _firstName, String _surname) {
			this.firstName = _firstName;
			this.surname = _surname;
			this.setId(_firstName+_surname);
		}
		
		public Person setBirthDay(String bDay) {
			birthDay = bDay;
			return this;
		} 
		
		public Person setGender(String _gender) {
			this.gender = _gender;
			return this;
		}

		public Person setAge(int _age) {
			this.age = _age;
			return this;
		}
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
        try {
        	new StreamlinedClient("es-test").clearIndex();
        } catch (Exception e) {
        	// Nothing to do... probably the index didn't exist in
        	// the first place.
        }
    }
	
	/*********************************
	 * DOCUMENTATION TESTS
	 *********************************/

	@Test
	public void test__StreamlinedClient__Synopsis() throws Exception {
		//
		// StreamlinedClient defines a bunch of easy-to-use, streamlined
		// methods for interacting with an ElasticSearch index
		//
		// To create a client:
		//
		
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
		// on the index, which will be described in the documentation tests below.
	}
	
	@Test
	public void test__DataManagementOperations__Documents() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = new StreamlinedClient(indexName);
		
		//
		// To add some objects to the index...
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
	}
	
	@Test
	public void test__Search() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = new StreamlinedClient(indexName);

		//
		// The easiest way to search is to use a free-form query.
		// You can think of this as the kind query you type into Google.
		//
		// Freeform queries support a bunch of operators that are described
		// on this page:
		//
		//   https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html
		//
		// For example, this will retrieve all people whose surname is 'simpson'
		// who is older than 30.
		//
		String query = "surname:simpson AND age:>30";
		SearchResults<Person> hits = client.searchFreeform(query, personPrototype);

		// You can then find out how many hits fit the query and loop through
		// them.
		Long totalHits = hits.getTotalHits();
		Iterator<Hit<Person>> iter = hits.iterator();
		while (iter.hasNext()) {
			Hit<Person> scoredHit = iter.next();
			Person hitDocument = scoredHit.getDocument();
			Double hitScore = scoredHit.getScore();
		}

		//
		// Note that like Google, ElasticSearch sometimes plays fast and loose
		// with freeform queries and may return documents that do not meet all
		// of its constraints. In the immortal words of Captain Barbossa
		// (Pirates of the Carribean): "Queries are more like GUIDELINES than
		// ACTUAL RULES".
		//
		// Also, the freeform syntax does not support all the operators that are
		// available in ElasticSearch.
		//
		// For more control and flexibility over the query, you should build a
		// structured query using the QueryBuilder. For example:
		//
		//
		QueryBody queryBody = new QueryBody();
		new BodyBuilder<QueryBody>(queryBody)
			.addObject("query")
				.addObject("bool")
					.addObject("must")
						.addObject("match")
							.addObject("surname", "simpson")
			.build();

		hits = client.search(queryBody, personPrototype);


		// You can aslo ask for some aggregations to be performed on some of
		// the fields of the hits.
		//
		// For example, will compute the average age of people whose surname is
		// simpson
		//
		queryBody = new QueryBody();
		new BodyBuilder<QueryBody>(queryBody)
			.addObject("query")
				.addObject("bool")
					.addObject("must")
						.addObject("match")
							.addObject("surname", "simpson")
							.closeObject()
						.closeObject()
					.closeObject()
				.closeObject()
			.closeObject()

			.addObject("aggs")
				.addObject("avgAge")
					.addObject("avg")
						.addObject("field", "age")

			.build()
		;

		hits = client.search(queryBody, personPrototype);
		Double averageAge = (Double) hits.aggrResult("avgAge");
	}

	@Test
	public void test__Search__WithAggregation() throws Exception {
		// Here is how you can aggregate (ex: sumup, average) the values of
		// fields

		String indexName = "es-test";
		StreamlinedClient client = new StreamlinedClient(indexName);

		client.putDocument(
			new Person("Homer", "Simpson")
			.setAge(42)
		);

		client.putDocument(
			new Person("Marge", "Simpson")
			.setAge(40)
		);

		client.putDocument(
			new Person("Moe", "Sizlack")
			.setAge(48)
		);

		Thread.sleep(2*1000);

		QueryBody queryBody = new QueryBody();
		new BodyBuilder<QueryBody>(queryBody)
			.addObject("query")
				.addObject("query_string")
					.addObject("query", "surname:Simpson")
					.closeObject()
				.closeObject()
			.closeObject()
			.addObject("aggs")
				.addObject("totalAge")
					.addObject("sum")
						.addObject("field", "age")

			.build();

		SearchResults<Person> hits = client.search(queryBody, personPrototype);
		Double gotTotalAge = (Double) hits.aggrResult("totalAge");
		Assert.assertEquals("Aggregated value not as expected",
			new Double(82.0), gotTotalAge);
	}
	
	@Test
	public void test__SearchSimilarDocs() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = new StreamlinedClient(indexName);
		
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
		
		
		// You may have noticed that the moreLikeThis and moreLikeThese searches
		// do not allow you to set hard criteria on the similar docs you want.
		//
		// You can however attach a HitFilter to SearchResults, in order 
		// to only get those similar docs that you want.
		//
		// For example, say you only want similar docs whose 'gender' field is
		// set to 'f'...
		//
		
		// See documentation tests of HitFilder for other possiblities
		HitFilter genderFilter = new HitFilter("+ gender:f");
		searchResults.setFilter(genderFilter);
		
		// The iterator will then only loop through the hits that pass
		// the gender filter
		Iterator<Hit<Person>> iter = searchResults.iterator();
		while (iter.hasNext()) {
			Hit<Person> scoredHit = iter.next();
			Person hitDocument = scoredHit.getDocument();
			Double hitScore = scoredHit.getScore();
		}
	}
		
	@Test
	public void test__ExplicitlyDefiningTypeOfFields() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = new StreamlinedClient(indexName);
		
		//
		// When you add documents to an index, the system automatically guesses 
		// the type of each fields. Most of the fields end up being of type 'text'.
		//
		// If you want to explicitly specify the type of one or more fields, make 
		// sure you invoke the setFieldTypes() BEFORE THE VERY FIRST TIME that you
		// add a document with that field to a given index.
		//
		client.deleteIndex(); // Let's delete the index to get rid of the existing types
		Map<String,String> fieldTypes = new HashMap<String,String>();
		{
			fieldTypes.put("birthDay", "date");
		}
		client.defineFieldTypes(fieldTypes);
		
							// Now when we add Person documents, their birthday 
							// field will be treated as a Date
		client.putDocument(new Person("Marg", "Simpson"));
	}
	
	@Test
	public void test__ClusterDocuments() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = new StreamlinedClient(indexName);		
		
		//
		// You can cluster a set of documents.
		// The  set of documents to be clustered is specified by a free-form query.
		// 
		// For this example, we will use a streamlined client that is connected
		// to an index containing all the lines from Shakespeare's play 'Hamlet'
		//
		// We will cluster all the lines that are spoken by Hamlet
		//
		StreamlinedClient hamletClient = ESTestHelpers.makeHamletTestClient();
		String query = "speaker:Hamlet";
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
		String[] useFields = new String[] {"longDescription"};
		DocClusterSet clusters = hamletClient.clusterDocuments(query, esDocTypeName, useFields, algName, maxDocs);
		
		// You can then look at the various clusters...
		for (String clusterLabel: clusters.getClusterNames()) {
			DocCluster aCluster = clusters.getCluster(clusterLabel);
			// Get some info about the cluster
			Set<String> idsOfDocsInCluster = aCluster.getDocIDs();
			int size = aCluster.getSize();
			// and so on...
		}
	}
		
	@Test
	public void test__IndexManagement() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();
		String docType = new PlayLine().getClass().getName();		
			
		//
		// You can delete all documents for a given doc type
		//
		client.clearDocType(docType);
		
		//
		// You can change the settings of the index (assuming it has already been created)
		//
		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put("index.mapping.total_fields.limit", new Integer(2000));
		client.changeIndexSettings(settings);
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
		
		String query = "Homer";
		SearchResults gotResults = client.searchFreeform(query, personPrototype);

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
		
		ESTestHelpers.sleepShortTime();
		List<Person> gotPeople = client.listFirstNDocuments(homer, 3);
		
		Person[] expPeople = new Person[] {homer, marg};
		AssertHelpers.assertDeepEquals("", expPeople, gotPeople);
		
		client.deleteDocumentWithID(homer.getId(), Person.class.getName());		
		expPeople = new Person[] {marg};
		ESTestHelpers.sleepShortTime();
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
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.moreLikeThis(queryLine, new IncludeFields("^content$"));		
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "longDescription", gotSearchResults);
	}	
	
	@Test
	public void test__moreLikeThese__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);

		List<ESTestHelpers.PlayLine> queryLines = new ArrayList<ESTestHelpers.PlayLine>();
		
		queryLines.add(new ESTestHelpers.PlayLine("Something is rotten in the kingdom of England"));
		queryLines.add(new ESTestHelpers.PlayLine("To sing or not to sing that is the question"));
		
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.moreLikeThese(queryLines, new IncludeFields("^content$"));		
		assertIsInFirstNHits("To the ambassadors of England gives", 20, "content", gotSearchResults);
		assertIsInFirstNHits("To be, or not to be: that is the question:", 20, "content", gotSearchResults);
	}	
	
	
	@Test
	public void test__searchFreeform__HappyPath() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);
		
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);

		String query = "denmark AND rotten";
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.searchFreeform(query, new PlayLine());		
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "longDescription", gotSearchResults);
	}		

	@Test
	public void test__searchFreeform__QuotedExpressions() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);
		
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);

		String query = "\"state of denmark\"";
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.searchFreeform(query, new PlayLine());		
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "longDescription", gotSearchResults);
	}	
	
	// Ignore for now because I need to set the 'id' field of the hamlet index to be of type 'keyword'
	@Test @Ignore
	public void test__searchFreeform__SortOrder() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);
		
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);

		String query = "denmark AND rotten";
		List<Pair<String,String>> sortBy = new ArrayList<Pair<String,String>>();
		{
			sortBy.add(Pair.of("id", "desc"));
		}
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.searchFreeform(query, new PlayLine(), sortBy);		
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "longDescription", gotSearchResults);
	}		
	

	@Test
	public void test__escapeQuotes__HappyPath() throws Exception {
		String query = "\"software development\" agile \"ui design\"";
		StreamlinedClient esClient = new StreamlinedClient("some_index");
		String gotQuery = esClient.escapeQuotes(query);
		String expQuery = "\\\"software development\\\" agile \\\"ui design\\\"";
		AssertHelpers.assertStringEquals(expQuery, gotQuery);
	}

	@Test
	public void test__scrollThroughSearchResults__HappyPath() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeHamletTestClient();	
		Thread.sleep(1*1000);
		final String PLAY_LINE = "line";

		ESTestHelpers.PlayLine queryLine = new ESTestHelpers.PlayLine("say");
		SearchResults<ESTestHelpers.PlayLine> hits = client.moreLikeThis(queryLine, new IncludeFields("^content$"));
		
		int hitsCount = 0;
		Iterator<Hit<PlayLine>> iter = hits.iterator();
		while (iter.hasNext() && hitsCount < 26) {
			Hit<ESTestHelpers.PlayLine> scoredHit = iter.next();
			AssertHelpers.assertStringContains("Hit did not fit query.", scoredHit.getDocument().getLongDescription(), "say", false);
			hitsCount++;
		}
		assertTrue("List of hits should have contained at least 25 hits, but only contained "+hitsCount, hitsCount >= 25);
	}		

	
	private <T extends Document> void assertIsInFirstNHits(Object expValue, int nHits, String fieldName, SearchResults<T> gotSearchResults) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, DocumentException {
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
	 * @throws DocumentException 
	***********************************************************/

	private <T extends Document> List<Object> fieldForFirstNHits(int nHits, String fieldName, SearchResults<T> gotHits) 
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, DocumentException {
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
				fldValue = aHit.getField(fieldName);
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
				"{\"query\":{\"more_like_this\":{\"min_term_freq\":1,\"min_doc_freq\":1,\"max_query_terms\":12,\"fields\":[\"lang\",\"id\",\"firstName\",\"surname\"],\"like\":{\"_index\":\"es-test\",\"_type\":\"ca.nrc.dtrc.elasticsearch.StreamlinedClientTest$Person\",\"doc\":{\"lang\":\"en\",\"id\":\"homersimpson\",\"firstName\":\"homer\",\"surname\":\"simpson\"}}}},\"highlight\":{\"order\":\"score\",\"fields\":{\"longDescription\":{\"type\":\"plain\"},\"shortDescription\":{\"type\":\"plain\"}}}}";
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
		{
			expFilteredFields.put("firstName", "homer");
		}
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
			expFilteredFields.put("id", "homersimpson");			
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
			expFilteredFields.put("id", "homersimpson");
			expFilteredFields.put("firstName", "homer");
			expFilteredFields.put("surname", "simpson");
			expFilteredFields.put("lang", "en");
		}
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__HappyPath() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();		
		Document homer = new Document("homersimpson");
		homer.setAdditionalField("first", "Homer");
		homer.setAdditionalField("last", "Simpson");
		homer.setCreationDate("2018-03-19");
		homer.setLongDescription("Homer is a character created created by Matt Groening in etc..");
		homer.setShortDescription("Homer is a the father of the Simpsons family");
		
		esClient.putDocument(homer);
		Map<String, Object> gotFilteredFields = esClient.filterFields(homer);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("id", "homersimpson");
			expFilteredFields.put("lang", "en");

			expFilteredFields.put("content", "Homer is a character created created by Matt Groening in etc..");
			expFilteredFields.put("shortDescription", "Homer is a the father of the Simpsons family");

			expFilteredFields.put("additionalFields.first", "Homer");
			expFilteredFields.put("additionalFields.last", "Simpson");			
		}
		AssertHelpers.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
		
	}
	
	@Test
	public void test__getFieldTypes__DynamicallyTypedDocsOnly() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		Document homer = new Document("homersimpson");
		homer.setAdditionalField("first", "homer");
		homer.setAdditionalField("last", "homer");
		homer.setAdditionalField("birthDay", "1993-01-26");
		String type = "CartoonCharacters";
		esClient.putDocument(type, homer);
		
		Map<String, String> gotTypes = esClient.getIndex().getFieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("id", "text");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("lang", "text");
			expTypes.put("additionalFields.birthDay", "date");
			expTypes.put("additionalFields.first", "text");
			expTypes.put("additionalFields.last", "text");
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
		Document marge = new Document("margesimpson");
		marge.setAdditionalField("birthDay", "1993-01-26");
		esClient.putDocument(type, marge);
		
		// The field types should be the union of the types for both
		// the dynamically and statically typed docs
		//
		Map<String, String> gotTypes = esClient.getIndex().getFieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("id", "text");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("lang", "text");

			expTypes.put("birthDay", "date");
			expTypes.put("additionalFields.birthDay", "date");
			
			expTypes.put("firstName", "text");
			expTypes.put("surname", "text");

			expTypes.put("age", "long");
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
		
		Map<String, String> gotTypes = esClient.getIndex().getFieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("id", "text");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("lang", "text");
			expTypes.put("birthDay", "date");
			expTypes.put("firstName", "text");
			expTypes.put("surname", "text");
			expTypes.put("age", "long");
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
		
		gotPerson = (Person) client.getDocumentWithID("HomerSimpson", Person.class);
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
	public void test__put_getDocument__DocIDContainsSlash() throws Exception {
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();
		
		String esDocType = "car-model";
		
		String modelID = "YTD24211/x";		
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
	public void test_clearDocType__HappyPath() throws Exception {
		String docType1 = "doctype1";
		String docType2 = "doctype2";
		StreamlinedClient client = ESTestHelpers.makeEmptyTestClient();	
		String indexName = client.getIndexName();
		Person homer = new Person("homer", "simpson");
		Person marge = new Person("marge", "simpson");
		
		// Enter a doc in two doc types 
		ESTestHelpers.assertDocTypeIsEmpty("", indexName, docType1, homer);
		client.putDocument(docType1, homer);
		ESTestHelpers.sleepShortTime();
		ESTestHelpers.assertDocTypeContainsDoc("", indexName, docType1, new String[] {"homersimpson"}, homer);
		ESTestHelpers.assertDocTypeIsEmpty("", indexName, docType2, homer);
		client.putDocument(docType2, marge);
		ESTestHelpers.sleepShortTime();
		ESTestHelpers.assertDocTypeContainsDoc("", indexName, docType2, new String[] {"margesimpson"}, homer);
		
		
		// Now clear one of the two docTypes and check that all is as expected
		client.clearDocType(docType1);
		ESTestHelpers.sleepExtraLongTime();
		ESTestHelpers.assertDocTypeIsEmpty("", indexName, docType1, homer);
		ESTestHelpers.assertDocTypeContainsDoc("", indexName, docType2, new String[] {"margesimpson"}, homer);
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
			expFieldTypes.put("age", "long");
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
		client.updateDocument(Person.class, "HomerSimpson", partialDoc);
		
		sleepABit();
		
		// Retrieve the doc from ES and it should now have a non-null
		// birthDay value
		Person gotPerson = (Person) client.getDocumentWithID("HomerSimpson", Person.class);
		Person expPerson = new Person("Homer", "Simpson").setBirthDay("1993-01-14");
		AssertHelpers.assertDeepEquals("Birthday was not updated in the ES index", 
				expPerson, gotPerson);
	}
	
	@Test
	public void test__clusterDocumentJsonBody__HappyPath() throws Exception {
		String indexName = "test-index";
		StreamlinedClient client = new StreamlinedClient(indexName);
		
		String[] useFields = new String[] {"longDescription"};
		String gotJson = client.clusterDocumentJsonBody("speaker:hamlet", "testdoc", useFields, "kmeans", 1000);
		String expJson = 
				"{\"search_request\":{"+
				  "\"_source\":[\"longDescription\"],"+
		          "\"query\":"+
				    "{\"query_string\":"+
		              "{\"query\":\"speaker:hamlet\"}"+
				    "},"+
		            "\"size\":1000"+
				  "},"+
		          "\"query_hint\":\"\","+
				  "\"algorithm\":\"kmeans\","+
				  "\"field_mapping\":{"+
		            "\"content\":[\"_source.longDescription\"]"+
		          "}"+
		        "}";
		AssertHelpers.assertStringEquals(expJson, gotJson);
	}
	
	@Test
	public void test__clusterDocuments__HappyPath() throws Exception {
		StreamlinedClient hamletClient = ESTestHelpers.makeHamletTestClient();
		String query = "additionalFields.speaker:Hamlet";
		Integer maxDocs = 1000; 
		String algName = "stc";
		String esDocTypeName = new PlayLine().getClass().getName();
		String[] useFields = new String[] {"content"};
		DocClusterSet clusters = hamletClient.clusterDocuments(query, esDocTypeName, useFields, algName, maxDocs);
		
		String[] expClusterNamesSuperset = new String[] {
				"Dost Thou Hear", "Nay", "Ay", "Other Topics", "Shall", "King", "Thou", "Sir", "Thee", "Know",
				"Mother", "Speak", "Play", "Love", "Heaven", "Tis", "Horatio", "Father", "Soul",
				"Heaven", "Hold", "Thy", "Eyes", "Matter", "Enter", "Dost Thou", "Lord"
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
		AssertHelpers.assertContainsAll("Cluster names not as expected\nNOTE: The exact clusters produced in this test are non-deterministic. If the test fail, it may be because you need to add new words to the expectation.", expClusterNamesSuperset, gotClusterNames);

		String clusterName = "Heaven";
		String[] expIDs = new String[] {
				"1.2.143", "1.2.144", "1.2.184", "1.4.44", "1.4.94", "1.5.109", "1.5.185", "1.5.97",
				"2.2.434", "2.2.595", "3.2.128", "3.3.81", "3.3.85", "3.3.96", "3.4.165",
				"3.4.55", "4.3.36", "5.2.52", "5.2.344", "5.2.357"	
		};
		String[] gotIDs = clusters.getCluster("Heaven").getDocIDs().toArray(new String[]{});
		AssertHelpers.assertContainsAll("Cluster IDs not as expected", expIDs, gotIDs);
	}
	
	@Test
	public void test__bulkIndex__HappyPath() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		
		File jsonFile = ResourceGetter.copyResourceToTempLocation("test_data/ca/nrc/dtrc/elasticsearch/small_index_content.json");
		Boolean force = true;
		esClient.bulkIndex(jsonFile.getAbsolutePath(), null, null, force);
		
		Thread.sleep(ESTestHelpers.LONG_WAIT);
		
		String[] expDocIDs = new String[] {
				"For whom the bell tolls",
				"The old man and the sea"
		};
		ESTestHelpers.assertDocTypeContainsDoc("Bulk indexed index did not contain the expected documents",
				esClient.getIndexName(), "books", expDocIDs, new Document());
	}
	
	@Test
	public void test__bulkIndex__LoadTwoFilesIntoSameIndexWithSameDefinition__DocsShouldBeAdded() throws Exception {
		StreamlinedClient esClient = ESTestHelpers.makeEmptyTestClient();
		
		File jsonFile = ResourceGetter.copyResourceToTempLocation("test_data/ca/nrc/dtrc/elasticsearch/small_index_content.json");
		Boolean force = true;
		esClient.bulkIndex(jsonFile.getAbsolutePath(), null, null, force);
		
		Thread.sleep(ESTestHelpers.LONG_WAIT);
		
		String[] expDocIDs = new String[] {
				"For whom the bell tolls",
				"The old man and the sea"
		};
		ESTestHelpers.assertDocTypeContainsDoc("Bulk indexed index did not contain the expected documents",
				esClient.getIndexName(), "books", expDocIDs, new Document());

		jsonFile = ResourceGetter.copyResourceToTempLocation("test_data/ca/nrc/dtrc/elasticsearch/other_small_index_content.json");
		esClient.bulkIndex(jsonFile.getAbsolutePath(), null, null, force);
	
		Thread.sleep(ESTestHelpers.LONG_WAIT);
		
		expDocIDs = new String[] {
				"For whom the bell tolls",
				"The old man and the sea",
				"Of mice and men"
		};
		ESTestHelpers.assertDocTypeContainsDoc("Bulk indexed index did not contain the expected documents",
				esClient.getIndexName(), "books", expDocIDs, new Document());
	
	}

	
	@Test(expected=BadDocProtoException.class)
	public void test__listAll__WrongDocType__RaisesBadDocProtoException() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = new StreamlinedClient(indexName);
		
		//
		// Create a collection with an object of type Person
		//
		String collection = "cartoon_character";
		Person homer = new Person("Homer", "Simpson");
		String jsonResponse = client.putDocument(collection, homer);
		Thread.sleep(1*1000);
		
		//
		// Try to listAll that collection, giving it a prototype of
		// the wrong type (Playline)
		//
		PlayLine badProto = new PlayLine();
		
		SearchResults<PlayLine> result =  client.listAll(collection, badProto);
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
