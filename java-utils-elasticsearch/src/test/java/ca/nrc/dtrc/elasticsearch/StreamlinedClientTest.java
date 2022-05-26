package ca.nrc.dtrc.elasticsearch;

import static ca.nrc.dtrc.elasticsearch.ESFactory.ESOptions;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;
import static ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers.PlayLine;
import ca.nrc.dtrc.elasticsearch.engine.MissingESPluginException;
import ca.nrc.dtrc.elasticsearch.index.AssertIndex;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import ca.nrc.dtrc.elasticsearch.request.Aggs;
import ca.nrc.dtrc.elasticsearch.request.Query;
import ca.nrc.dtrc.elasticsearch.request.Sort;
import ca.nrc.file.ResourceGetter;
import ca.nrc.introspection.Introspection;
import ca.nrc.json.PrettyPrinter;
import ca.nrc.testing.*;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.*;

import static org.junit.Assert.assertTrue;

public abstract class StreamlinedClientTest {
	
	public abstract int esVersion();
	public abstract ESFactory makeESFactory(String indexName) throws ElasticSearchException;

	final static String PERSON_TYPE = "person";
	final static Person personPrototype = new Person();

	private static boolean skipTests = false;

	private static Boolean _carrotInstalled = null;

	@BeforeAll
	public static void beforeAll() throws Exception {
		boolean skip = true;
		org.junit.Assume.assumeFalse(skip);
	}


	@BeforeEach
	public void setUp() throws Exception {
	  try {
		makeClient("es-test").clearIndex();
	  } catch (Exception e) {
		// Nothing to do... probably the index didn't exist in
		// the first place.
	  }
	  return;
	}

	public  StreamlinedClient makeClient() throws ElasticSearchException {
		return makeESFactory("").client();
	}

	public StreamlinedClient makeClient(String indexName) throws ElasticSearchException {
		return makeESFactory(indexName).client();
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
		// To create a client, first create an ESFactory that is appropriate for
		// the versio of ES you are running, and get the client from it.
		//
		//

		String indexName = "es-test";
		ESFactory factory = makeESFactory(indexName);
		StreamlinedClient client = factory.client();

		// By default, the server is on:
		//
		//   Server : localhost
		//   Port   : 9205
		//
		// But you can change those like this:
		//
			client.setServer("www.somewhere.com")
				.setPort(9400);

		// You can then use the client to do all sorts of operations
		// on the index, which will be described in the documentation tests below.
	}

	@Test
	public void test__DataManagementOperations__Documents() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = makeClient(indexName);

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

		// Loop through the IDs of adocuments in the index
		results = client.listAll(personPrototype);
		Iterator<String> idsIter = results.docIDIterator();
		while (idsIter.hasNext()) {
			String docID = idsIter.next();
		}

		// Get a specific document by its ID
		Person person = (Person) client.getDocumentWithID("Homer", Person.class);

		// Delete a document with a specific ID
		client.deleteDocumentWithID("Homer", Person.class);
	}

	@Test
	public void test__Search() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeCartoonTestIndex().client();

		ShowCharacter characterPrototype = new ShowCharacter();
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
		SearchResults<ShowCharacter> hits =
			client.search(query, characterPrototype);

		// You can then find out how many hits fit the query and loop through
		// them.
		Long totalHits = hits.getTotalHits();
		Iterator<Hit<ShowCharacter>> iter = hits.iterator();
		while (iter.hasNext()) {
			Hit<ShowCharacter> scoredHit = iter.next();
			ShowCharacter hitDocument = scoredHit.getDocument();
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
		Query queryBody = new Query(
			new JSONObject()
				.put("bool", new JSONObject()
					.put("must", new JSONObject()
						.put("match", new JSONObject()
							.put("surname", "simpson")
						)
					)
				)
		);
		hits = client.search(queryBody, characterPrototype);


		// You can also ask for some aggregations to be performed on some of
		// the fields of the hits.
		//
		// For example, will compute the average age of people whose surname is
		// simpson
		//
		queryBody = new Query(
			new JSONObject()
			.put("bool", new JSONObject()
				.put("must", new JSONObject()
					.put("match", new JSONObject()
						.put("surname", "simpson")
					)
				)
			)
		);
		Aggs aggsBody =
			new Aggs()
			.aggregate("avgAge", "avg", "age");

		hits = client.search(queryBody, characterPrototype, aggsBody);
		Double averageAge = (Double) hits.aggrResult("avgAge", Double.class);
	}

	@Test
	public void test__Search__WithAggregation() throws Exception {
		// Here is how you can aggregate (ex: sumup, average) the values of
		// fields
		String indexName = "es-test";
		StreamlinedClient client = makeClient(indexName);

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

		Query queryBody = new Query(
			new JSONObject()
			.put("query_string", new JSONObject()
				.put("query", "surname:Simpson")
			)
		);

		Aggs aggsBody = new Aggs();
		aggsBody.aggregate("totalAge", "sum", "age");

		SearchResults<Person> hits =
			client.search(queryBody, personPrototype, aggsBody);
		Long gotTotalAge = (Long) hits.aggrResult("totalAge",Long.class);
		Assert.assertEquals("Aggregated value not as expected",
			new Long(82), gotTotalAge);
	}

	@Test
	public void test__SearchSimilarDocs() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeCartoonTestIndex().client();

		//
		// Find documents that are similar to a particular doc.
		//
		ShowCharacter queryPerson = new ShowCharacter("Lisa", "Simpson", "The Simpsons");
		SearchResults<ShowCharacter> searchResults = client.moreLikeThis(queryPerson);

		//
		// You can also find documents that are similar to a list of
		// documents.
		//
		List<ShowCharacter> queryPeople = new ArrayList<ShowCharacter>();
		queryPeople.add(new ShowCharacter("Lisa", "Simpson", "The Simpsons"));
		queryPeople.add(new ShowCharacter("Bart", "Simpson", "The Simpsons"));
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
		Iterator<Hit<ShowCharacter>> iter = searchResults.iterator();
		while (iter.hasNext()) {
			Hit<ShowCharacter> scoredHit = iter.next();
			ShowCharacter hitDocument = scoredHit.getDocument();
			Double hitScore = scoredHit.getScore();
		}
	}

	@Test
	public void test__ClusterDocuments() throws Exception {
		String indexName = "es-test";

		// Note: Document clustering is only possible if the carrot ES plugin is
		// installed
		if (!carrotPluginInstalled()) {
			return;
		}

		StreamlinedClient client = makeClient(indexName);

		//
		// You can cluster a set of documents.
		// The  set of documents to be clustered is specified by a free-form query.
		//
		// For this example, we will use a streamlined client that is connected
		// to an index containing all the lines from Shakespeare's play 'Hamlet'
		//
		// We will cluster all the lines that are spoken by Hamlet
		//
		StreamlinedClient hamletClient = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
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
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
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

		ESFactory esFactory = new ESTestHelpers(esVersion()).makeEmptyTestIndex();
		IndexAPI indexAPI = esFactory.indexAPI();
		StreamlinedClient client = esFactory.client();
		Person protoPerson = new Person();

		new AssertIndex(esFactory).typeIsEmpty(protoPerson);

		String jsonResponse = client.putDocument(new Person("Homer", "Simpson"));
		new ESTestHelpers(esVersion()).assertNoError(jsonResponse);

		client.putDocument(new Person("Marg", "Simpson"));

		Thread.sleep(2*1000);

		String query = "Homer";
		SearchResults gotResults = client.search(query, personPrototype);

		Person[] expHits = new Person[] {
				new Person("Homer", "Simpson")
		};

		assertUnscoredHitsAre("", expHits, gotResults);
	}


	@Test
	public void test__deleteDocument__HappyPath() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeCartoonTestIndex().client();

		ShowCharacter homer = new ShowCharacter("Homer", "Simpson", "The Simpsons");
		SearchResults<ShowCharacter> gotPeople = client.listAll(homer);
		new AssertSearchResults(gotPeople, "Initial set of people not as expected")
			.containsIDs(homer.getId());

		client.deleteDocumentWithID(homer.getId(), ShowCharacter.class);
		Thread.sleep(1*1000);
		gotPeople = client.listAll(homer);
		new AssertSearchResults(gotPeople, "Initial set of people not as expected")
			.doesNotcontainIDs(homer.getId());
	}

	@Test
	public void test__moreLikeThis__HappyPath() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		Thread.sleep(1*1000);

		ESTestHelpers.PlayLine queryLine = new ESTestHelpers.PlayLine("Something is rotten in the kingdom of England");
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults =
			client.moreLikeThis(queryLine, new IncludeFields("^content$"));
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "longDescription", gotSearchResults);
	}

	@Test
	public void test__moreLikeThese__HappyPath() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		Thread.sleep(1*1000);

		List<ESTestHelpers.PlayLine> queryLines = new ArrayList<ESTestHelpers.PlayLine>();

		queryLines.add(new ESTestHelpers.PlayLine("Something is rotten in the kingdom of England"));
		queryLines.add(new ESTestHelpers.PlayLine("To sing or not to sing that is the question"));

		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.moreLikeThese(queryLines, new IncludeFields("^content$"));
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 30, "content", gotSearchResults);
		assertIsInFirstNHits("To be, or not to be: that is the question:", 30, "content", gotSearchResults);
	}


	@Test
	public void test__searchFreeform__HappyPath() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);

		StreamlinedClient client = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		Thread.sleep(1*1000);

		String query = "denmark AND rotten";
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.search(query, new PlayLine());
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "longDescription", gotSearchResults);
	}

	@Test
	public void test__searchFreeform__NullOrEmptyQuery__ReturnsAllDocuments() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);

		StreamlinedClient client = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		Thread.sleep(1*1000);

		int expTotalHits = 4013;

		String query = null;
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults =
			client.search(query, new PlayLine());
		new AssertSearchResults(gotSearchResults,
			"Null query should have returned all docs")
			.totalHitsEquals(expTotalHits);

		query = "  ";
		gotSearchResults =
			client.search(query, new PlayLine());
		new AssertSearchResults(gotSearchResults,
			"EMPTY query should have returned all docs")
			.totalHitsEquals(expTotalHits);
	}

	@Test
	public void test__searchFreeform__QuotedExpressions() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);

		StreamlinedClient client = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		Thread.sleep(1*1000);

		String query = "\"state of denmark\"";
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = client.search(query, new PlayLine());
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "longDescription", gotSearchResults);
	}

	// Ignore for now because I need to set the 'id' field of the hamlet index to be of type 'keyword'
	@Test
	public void test__searchFreeform__SortOrder() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);

		StreamlinedClient client = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		Thread.sleep(1*1000);

		String query = "denmark AND rotten";

		// For some reason, ES5 can't sort by _id and ES7 can't sort by _uid;
		String sortField = "_id";
		if (esVersion() == 5) {
			sortField = "_uid";
		}
		Sort sortBody =
			new Sort().sortBy(sortField, Sort.Order.desc);
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults =
			client.search(query, new PlayLine(), sortBody);
		assertIsInFirstNHits("Something is rotten in the state of Denmark.", 3, "longDescription", gotSearchResults);
	}

	@Test
	public void test__scrollThroughSearchResults__HappyPath() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
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
			    +".\nValues of first hits were: "+ PrettyPrinter.print(gotFields),
				gotFieldsSet.contains(expValue));

	}

	@Test
	public void test__getFieldTypes__DynamicallyTypedDocsOnly() throws Exception {
		StreamlinedClient esClient = new ESTestHelpers(esVersion()).makeEmptyTestIndex().client();
		Document homer = new Document("homersimpson", "person");
		homer.setAdditionalField("first", "homer");
		homer.setAdditionalField("last", "homer");
		homer.setAdditionalField("birthDay", "1993-01-26");
		String type = "CartoonCharacters";
		esClient.putDocument(type, homer);

		Map<String, String> gotTypes = esClient.indexAPI().fieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("type", "keyword");
			expTypes.put("id", "keyword");
			expTypes.put("idWithoutType", "keyword");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("lang", "keyword");
			expTypes.put("additionalFields.birthDay", "date");
			expTypes.put("additionalFields.first", "text");
			expTypes.put("additionalFields.last", "text");
		}
		AssertObject.assertDeepEquals("Field types not as expected for type: "+type,
				expTypes, gotTypes);
	}

	@Test
	public void test__getFieldTypes__StaticallyTypedDocsOnly() throws Exception {
		// This ESFactory doc type will contain both statically typed and dynamically typed
		// documents representing people.
		String type = "CartoonCharacters";

		// This person is statically typed
		StreamlinedClient esClient = new ESTestHelpers(esVersion()).makeEmptyTestIndex().client();
		Person homer = new Person("homer", "simpson").setBirthDay("1993-01-26");
		esClient.putDocument(type, homer);

		// This other person is dynamically typed
		Document marge = new Document("margesimpson", "person");
		marge.setAdditionalField("birthDay", "1993-01-26");
		esClient.putDocument(type, marge);

		// The field types should be the union of the types for both
		// the dynamically and statically typed docs
		//
		Map<String, String> gotTypes = esClient.indexAPI().fieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("id", "keyword");
			expTypes.put("idWithoutType", "keyword");
			expTypes.put("type", "keyword");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("lang", "keyword");

			expTypes.put("birthDay", "date");
			expTypes.put("additionalFields.birthDay", "date");

			expTypes.put("firstName", "text");
			expTypes.put("surname", "text");

			expTypes.put("age", "long");
		}
		AssertObject.assertDeepEquals("Field types not as expected for type: "+type,
				expTypes, gotTypes);
	}

	@Test
	public void test__getFieldTypes__MixedDynamicallyAndStaticallyTypedDocs() throws Exception {
		StreamlinedClient esClient = new ESTestHelpers(esVersion()).makeEmptyTestIndex().client();
		Person homer = new Person("homer", "simpson").setBirthDay("1993-01-26");
		String type = "CartoonCharacters";
		esClient.putDocument(type, homer);

		Map<String, String> gotTypes = esClient.indexAPI().fieldTypes(type);
		Map<String,String> expTypes = new HashMap<String,String>();
		{
			expTypes.put("id", "keyword");
			expTypes.put("idWithoutType", "keyword");
			expTypes.put("type", "keyword");
			expTypes.put("_detect_language", "boolean");
			expTypes.put("lang", "keyword");
			expTypes.put("birthDay", "date");
			expTypes.put("firstName", "text");
			expTypes.put("surname", "text");
			expTypes.put("age", "long");
		}
		AssertObject.assertDeepEquals("Field types not as expected for type: "+type,
				expTypes, gotTypes);
	}

	@Test
	public void test__put_getDocument__HappyPath() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeCartoonTestIndex().client();

		ShowCharacter gotPerson = (ShowCharacter) client.getDocumentWithID("FredFlintstone", ShowCharacter.class);
		AssertObject.assertDeepEquals(
			"Fred should NOT have been in the index initially",
			null, gotPerson);

		ShowCharacter fred = new ShowCharacter("Fred", "Flintstone", "The Flintstones");
		client.putDocument(fred);

		gotPerson = (ShowCharacter) client.getDocumentWithID("FredFlintstone", ShowCharacter.class);
		AssertObject.assertDeepEquals(
			"Fred SHOULD have been in the index after being added",
			fred, gotPerson);
	}

	@Test
	public void test__put_getDocument__DynTyped() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeEmptyTestIndex().client();

		String esDocType = "car-model";

		String modelID = "YTD24211";
		CarModel gotCar = (CarModel) client.getDocumentWithID(modelID, CarModel.class, esDocType);
		AssertObject.assertDeepEquals("Car model "+modelID+" should NOT have been in the index initially", null, gotCar);

		CarModel corolla2009 =new CarModel(modelID);
		corolla2009.setMaker("Toyota");
		corolla2009.setModel("Corolla");
		corolla2009.setYear(2009);
		client.putDocument(esDocType, corolla2009);

		gotCar = (CarModel) client.getDocumentWithID(modelID, CarModel.class, esDocType);
		AssertObject.assertDeepEquals("Corolla have been in the index after being added", corolla2009, gotCar);
	}

	@Test
	public void test__put_getDocument__DocIDContainsSlash() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeEmptyTestIndex().client();

		String esDocType = "car-model";

		String modelID = "YTD24211/x";
		CarModel gotCar = (CarModel) client.getDocumentWithID(modelID, CarModel.class, esDocType);
		AssertObject.assertDeepEquals("Car model "+modelID+" should NOT have been in the index initially", null, gotCar);

		CarModel corolla2009 = new CarModel(modelID);
		corolla2009.setModel("Toyota");
		corolla2009.setMaker("Corolla");
		corolla2009.setYear(2009);
		client.putDocument(esDocType, corolla2009);

		gotCar = (CarModel) client.getDocumentWithID(modelID, CarModel.class, esDocType);
		AssertObject.assertDeepEquals("Corolla have been in the index after being added", corolla2009, gotCar);
	}

	@Test
	public void test_clearDocType__HappyPath() throws Exception {
		String indexName = "test-index";
		ESTestHelpers helpers = new ESTestHelpers(makeESFactory(indexName));
		ESFactory esFactory = new ESTestHelpers(makeESFactory(indexName)).makeEmptyTestIndex();
		StreamlinedClient client = esFactory.client();

		Person protoPerson = new Person();
		Person homer = new Person("homer", "simpson");
		Person marge = new Person("marge", "simpson");
		String docType1 = "person1";
		String docType2 = "person2";
		AssertIndex asserter = new AssertIndex(esFactory);

		// Enter a doc in two doc types
		// First in "person1" type
		client.putDocument(docType1, homer);
		helpers.sleepShortTime();
		asserter.docsInTypeEqual(docType1, protoPerson, homer.getId());

		// Then in "person2" type
		client.putDocument(docType2, marge);
		helpers.sleepShortTime();
		asserter.docsInTypeEqual(docType2, protoPerson, marge.getId());


		// Now clear one of the two docTypes and check that all is as expected
		client.clearDocType(docType1);
		helpers.sleepExtraLongTime();
		asserter.typeIsEmpty(docType1, protoPerson);
		asserter.typeNotEmpty(docType2, protoPerson);
	}

	@Test
	public void test__updateDocument__HappyPath() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeEmptyTestIndex().client();

		// First, Put a person with an empty date of birth;
		Person homer = new Person("Homer", "Simpson");
		client.putDocument(homer);

		// Then update his birth date
		Map<String,Object> partialDoc = new HashMap<String,Object>();
		partialDoc.put("birthDay", "1993-01-14");
		client.updateDocument(Person.class, "HomerSimpson", partialDoc);

		sleepABit();

		// Retrieve the doc from ESFactory and it should now have a non-null
		// birthDay value
		Person gotPerson = (Person) client.getDocumentWithID("HomerSimpson", Person.class);
		Person expPerson = new Person("Homer", "Simpson").setBirthDay("1993-01-14");
		AssertObject.assertDeepEquals("Birthday was not updated in the ESFactory index",
				expPerson, gotPerson);
	}

	@Test
	public void test__clusterDocumentJsonBody__HappyPath() throws Exception {
		String indexName = "test-index";
		StreamlinedClient client = makeClient(indexName);

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
		AssertString.assertStringEquals(expJson, gotJson);
	}

	// Note: Disabled for now because we can't get a version of carrot cluster
	//   plugin to install
	@Test
	public void test__clusterDocuments__HappyPath() throws Exception {
		if (!carrotPluginInstalled()) {
			return;
		}
		StreamlinedClient hamletClient = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		String query = "additionalFields.speaker:Hamlet";
		Integer maxDocs = 1000;
		String algName = "stc";
		String esDocTypeName = new PlayLine().getClass().getName();
		String[] useFields = new String[] {"content"};
		DocClusterSet clusters = hamletClient.clusterDocuments(query, esDocTypeName, useFields, algName, maxDocs);

		String[] expClusterNamesSuperset = new String[] {
				"Ay",
				"Dost Thou", "Dost Thou Hear",
				"Enter", "Enter King CLAUDIUS", "\"Enter King CLAUDIUS, Queen GERTRUDE\"", "Eyes",
				"Father",
				"Good Friends", "GUILDENSTERN, ROSENCRANTZ",
				"Heaven", "Hold", "Horatio",
				"King", "King CLAUDIUS", "Know",
				"Lord", "Love",
				"Matter", "Mother",
				"Nay",
				"Other Topics",
				"Play", "Players",
				"QUEEN GERTRUDE",
				"ROSENCRANTZ, GUILDENSTERN", "ROSENCRANTZ and GUILDENSTERN",
				"Shall", "Sir", "Soul", "Speak",
				"Thou", "Thee", "Thy", "Tis"
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
		ESFactory esFactory = new ESTestHelpers(esVersion()).makeEmptyTestIndex();
		StreamlinedClient esClient = esFactory.client();

		File jsonFile = ResourceGetter.copyResourceToTempLocation("test_data/ca/nrc/dtrc/elasticsearch/small_index_content.json");
		Boolean force = true;
		esClient.bulkIndex(jsonFile.getAbsolutePath(), ESFactory.ESOptions.CREATE_IF_NOT_EXISTS);

		Thread.sleep(new ESTestHelpers(esVersion()).LONG_WAIT);

		String[] expDocIDs = new String[] {
				"For whom the bell tolls",
				"The old man and the sea"
		};
		new AssertIndex(esFactory)
			.docsInTypeEqual("books", new Document(), expDocIDs);
		;
	}

	@Test
	public void test__bulkIndex__LoadTwoFilesIntoSameIndexWithSameDefinition__DocsShouldBeAdded() throws Exception {
		ESFactory esFactory =
			new ESTestHelpers(esVersion())
				.makeEmptyTestIndex();
		StreamlinedClient esClient = esFactory.client();
		AssertIndex asserter = new AssertIndex(esFactory);
		Document protoDoc = new Document();

		File jsonFile = ResourceGetter.copyResourceToTempLocation(
			"test_data/ca/nrc/dtrc/elasticsearch/small_index_content.json");
		Boolean force = true;

		// Bulk index first file
		esClient.bulkIndex(jsonFile.getAbsolutePath(), ESOptions.CREATE_IF_NOT_EXISTS);
		Thread.sleep(new ESTestHelpers(esVersion()).LONG_WAIT);
		String[] expDocIDs = new String[] {
			"For whom the bell tolls",
			"The old man and the sea"
		};
		asserter.docsInTypeEqual("books", protoDoc, expDocIDs);

		// Bulk index second file into same type of same index, using APPEND mode
		jsonFile = ResourceGetter.copyResourceToTempLocation("test_data/ca/nrc/dtrc/elasticsearch/other_small_index_content.json");
		boolean append = true;
		esClient.bulkIndex(jsonFile.getAbsolutePath(),
			ESOptions.APPEND.CREATE_IF_NOT_EXISTS, ESOptions.APPEND);
		Thread.sleep(new ESTestHelpers(esVersion()).LONG_WAIT);
		expDocIDs = new String[] {
			"For whom the bell tolls",
			"The old man and the sea",
			"Of mice and men",
		};
		asserter.docsInTypeEqual("books", protoDoc, expDocIDs);
	}


	@Test
	public void test__listAll__WrongDocType__RaisesBadDocProtoException() throws Exception {
		StreamlinedClient client = new ESTestHelpers(esVersion()).makeCartoonTestIndex().client();

		String typeToSearch = showCharacterProto.type;

		//
		// Try to listAll that collection, giving it a prototype of
		// the wrong type (Playline)
		//
		PlayLine badProto = new PlayLine();

		Assertions.assertThrows(BadDocProtoException.class, () -> {
			SearchResults<PlayLine> result =
				client.listAll(typeToSearch, badProto);
		});
	}

	@Test
	public void test__indexExists__HappyPath() throws Exception {
		String indexName = "es-test";
		StreamlinedClient client = makeClient(indexName);
		client.deleteIndex();
		Assert.assertFalse(
			"Index "+indexName+" should NOT have existed at the start of test",
			client.indexExists());

		Person homer = new Person("Homer", "Simpson");
		String jsonResponse = client.putDocument(homer);
		Assert.assertTrue(
			"Index "+indexName+" SHOULD have existed after we added a document to it.",
			client.indexExists());
	}

	@Test
	public void test__dumpToFile__DumpAll() throws Exception {
		StreamlinedClient esClient = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		File gotFile =
			Files.createTempFile("test", "json", new FileAttribute[0])
			.toFile();
		esClient.dumpToFile(gotFile, PlayLine.class);

		AssertFile.assertFileContains(
			"Dumped index was missing the first document",
			gotFile, new ESTestHelpers(esVersion()).hamletFirstLine, true, false);
		AssertFile.assertFileContains(
			"Dumped index was missing the last document",
			gotFile, new ESTestHelpers(esVersion()).hamletLastLine, true, false);
	}

	@Test
	public void test__dumpToFile__DumpMatchingDocs() throws Exception {
		StreamlinedClient esClient = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		File gotFile =
			Files.createTempFile("test", "json", new FileAttribute[0])
			.toFile();
		String query = "additionalFields.speaker:FRANCISCO";
		esClient.dumpToFile(gotFile, PlayLine.class,query, (Set)null);

		AssertFile.assertFileDoesNotContain(
			"Dumped index should only have contained lines spoken by FRANCISCO",
			gotFile.getPath(),
			"\"speaker\":\"HAMLET\"",
			false);
		String expLine =
			"{\n" +
			"    \"_detect_language\": true,\n" +
			"    \"additionalFields\": {\n" +
			"        \"line_id\": 32454,\n" +
			"        \"line_number\": \"1.1.18\",\n" +
			"        \"play_name\": \"Hamlet\",\n" +
			"        \"speaker\": \"FRANCISCO\",\n" +
			"        \"speech_number\": 15\n" +
			"    },\n" +
			"    \"content\": \"Give you good night.\",\n" +
			"    \"creationDate\": null,\n" +
			"    \"id\": \"playline:1.1.18\",\n" +
			"    \"idWithoutType\": \"1.1.18\",\n" +
			"    \"lang\": \"en\",\n" +
			"    \"longDescription\": \"Give you good night.\",\n" +
			"    \"shortDescription\": null,\n" +
			"    \"type\": \"playline\"\n" +
			"}";
		expLine = new ESTestHelpers(esVersion()).jsonDeformat(expLine);

		AssertFile.assertFileContains(
			"Dumped index was missing a line spoken by FRANCISCO",
			gotFile, expLine, true, false);
	}

	@Test
	public void test__dumpToFile__NullQuery() throws Exception {
		StreamlinedClient esClient = new ESTestHelpers(esVersion()).makeHamletTestIndex().client();
		File gotFile =
			Files.createTempFile("test", "json", new FileAttribute[0])
			.toFile();
		String nullQuery = null;
		esClient.dumpToFile(gotFile, PlayLine.class, nullQuery, (Set)null);

		AssertFile.assertFileContains(
			"Dumped index is missing the first line of the play",
			gotFile, new ESTestHelpers(esVersion()).hamletFirstLine,
			true, false);
		AssertFile.assertFileContains(
			"Dumped index is missing the last line of the play",
			gotFile, new ESTestHelpers(esVersion()).hamletLastLine,
			true, false);
	}

	/*************************
	 * TEST HELPERS
	 *************************/

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

//	private <T> void assertHitFieldsEqual(String fieldName, Object[] expFieldValuesArray, List<T> hits, Integer onlyNHits) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
//		List<Object> gotValues = new ArrayList<Object>();
//		for (T aHit: hits) {
//			if (onlyNHits <= 0) break;
//			Object fldValue = null;
//			if (aHit instanceof Map<?,?>) {
//				Map<String,Object> aHitMap = (Map<String, Object>) aHit;
//				fldValue = (Object) aHitMap.get(fieldName);
//			} else {
//				Field fld = aHit.getClass().getDeclaredField(fieldName);
//				fld.setAccessible(true);
//				fldValue = (Object) fld.get(aHit);
//			}
//			gotValues.add(fldValue);
//			onlyNHits--;
//		}
//		List<Object> expValues = new ArrayList<Object>();
//		for (int ii=0; ii < expFieldValuesArray.length; ii++) {
//			expValues.add(expFieldValuesArray[ii]);
//			int x = 0;
//		}
//
//		AssertObject.assertDeepEquals("Values of field "+fieldName+" were not as expected", expValues, gotValues);
//	}

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

		AssertObject.assertDeepEquals(message+"\nUnscored hits were not as expected",
				expUnscoredHits, gotUnscoredHits);
	}

	////////////////////////////////////////
	// Document classes used for this test
	////////////////////////////////////////

	public static class Person extends Document {
		public String firstName;
		public String surname;
		public String birthDay;
		public String gender;
		public Integer age = 0;

		public Person() {
			this.type = "person";
		};

		public Person(String _firstName, String _surname) {
			this.type = "person";
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

	private boolean carrotPluginInstalled() throws ElasticSearchException {
		if (_carrotInstalled == null) {
			try {
				// Note: Document clustering is only possible if the carrot ES plugin is
				// installed
				_carrotInstalled = true;
				ESFactory esFactory = makeESFactory("");
				esFactory.engineAPI().ensurePluginInstalled("elasticsearch-carrot2");
			} catch (MissingESPluginException e) {
				_carrotInstalled = false;
			}
		}
		return _carrotInstalled;
	}
}
