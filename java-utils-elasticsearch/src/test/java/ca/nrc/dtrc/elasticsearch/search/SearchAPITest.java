package ca.nrc.dtrc.elasticsearch.search;

import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;
import static ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;

import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import ca.nrc.dtrc.elasticsearch.request.Aggs;
import ca.nrc.dtrc.elasticsearch.request.Query;
import ca.nrc.dtrc.elasticsearch.request.Sort;
import ca.nrc.introspection.Introspection;
import ca.nrc.testing.AssertJson;
import ca.nrc.testing.AssertObject;
import ca.nrc.testing.AssertString;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

public abstract class SearchAPITest {

	private ESFactory esFactory = null;

	protected abstract ESFactory makeESFactory(String _indexName) throws ElasticSearchException;
	protected abstract int esVersion();

	@BeforeEach
	public void setUp() throws Exception {
		esFactory = makeESFactory("UNDEFINED_INDEX");
		esFactory.updatesWaitForRefresh = true;
		new ESTestHelpers(esVersion()).makeCartoonTestIndex();
	}

	/////////////////////////////////////////
	// VERIFICATION TESTS
	/////////////////////////////////////////

	@Test
	public void test__SearchAPI__Synopsis() throws Exception {
		// Use SearchAPI to carry out different types of searches on an ES
		// index.
		//
		// To create a SearchAPI, first create an ESFactory that is appropriate
		// for the ES version you are running, and obtain the SearchAPI from it.
		ESFactory factory = makeESFactory(ESTestHelpers.cartoonsTestIndex);
		SearchAPI searchAPI = factory.searchAPI();

		// Some of the searches require you to provide a prototype for the
		// documents that will be retrieved.
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
			searchAPI.search(query, characterPrototype);

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
		hits = searchAPI.search(queryBody, characterPrototype);


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
		hits = searchAPI.search(queryBody, characterPrototype, aggsBody);
		Double averageAge = (Double) hits.aggrResult("avgAge", Double.class);

		//
		// You can also find documents that are similar to a particular doc.
		//
		ShowCharacter queryPerson =
			new ShowCharacter("Lisa", "Simpson", "The Simpsons");
		SearchResults<ShowCharacter> searchResults =
			searchAPI.moreLikeThis(queryPerson);

		//
		// You can also find documents that are similar to a list of
		// documents.
		//
		List<ShowCharacter> queryPeople = new ArrayList<ShowCharacter>();
		queryPeople.add(new ShowCharacter("Lisa", "Simpson", "The Simpsons"));
		queryPeople.add(new ShowCharacter("Bart", "Simpson", "The Simpsons"));
		searchResults = searchAPI.moreLikeThese(queryPeople);


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
		iter = searchResults.iterator();
		while (iter.hasNext()) {
			Hit<ShowCharacter> scoredHit = iter.next();
			ShowCharacter hitDocument = scoredHit.getDocument();
			Double hitScore = scoredHit.getScore();
		}
	}

	/////////////////////////////////////////
	// VERIFICATION TESTS
	/////////////////////////////////////////

	@Test
	public void test__searchFreeform__HappyPath() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);
		esFactory = new ESTestHelpers(esFactory).makeHamletTestIndex();
		Thread.sleep(1*1000);

		String query = "denmark AND rotten";
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults =
			esFactory.searchAPI().search(query, new PlayLine());
		new AssertSearchResults(gotSearchResults)
			.fieldValueFoundInFirstNHits(
				Pair.of(
					"longDescription",
					"Something is rotten in the state of Denmark."),
				3
			);
	}

	@Test
	public void test__searchFreeform__NullOrEmptyQuery__ReturnsAllDocuments() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);
		esFactory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
		SearchAPI searchAPI = esFactory.searchAPI();
		Thread.sleep(1*1000);

		int expTotalHits = 4013;

		String query = null;
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults =
			searchAPI.search(query, new PlayLine());
		new AssertSearchResults(gotSearchResults,
			"Null query should have returned all docs")
			.totalHitsEquals(expTotalHits);

		query = "  ";
		gotSearchResults =
			searchAPI.search(query, new PlayLine());
		new AssertSearchResults(gotSearchResults,
			"EMPTY query should have returned all docs")
			.totalHitsEquals(expTotalHits);
	}

	@Test
	public void test__searchFreeform__QuotedExpressions() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);

		esFactory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
		SearchAPI searchAPI = esFactory.searchAPI();
		Thread.sleep(1*1000);

		String query = "\"state of denmark\"";
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults = searchAPI.search(query, new PlayLine());
		new AssertSearchResults(gotSearchResults)
			.fieldValueFoundInFirstNHits(
				Pair.of(
					"longDescription",
					"Something is rotten in the state of Denmark."
				), 3
			);
	}

	@Test
	public void test__searchFreeform__SortOrder() throws Exception {
		ESTestHelpers.PlayLine line = new ESTestHelpers.PlayLine("hello world");
		Introspection.getFieldValue(line, "longDescription", true);

		esFactory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
		SearchAPI searchAPI = esFactory.searchAPI();
		Thread.sleep(1*1000);

		String query = "denmark AND rotten";

		// For some reason, ES7 refuses to sort by '_uid', but ES5 refuses
		// to sort by '_id'
		String sortByField = "_id";
		if (esVersion() == 5) {
			sortByField = "_uid";
		}
		Sort sortBody =
			new Sort().sortBy(sortByField, Sort.Order.desc);
		SearchResults<ESTestHelpers.PlayLine> gotSearchResults =
		searchAPI.search(query, new PlayLine(), sortBody);
		new AssertSearchResults(gotSearchResults)
			.fieldValueFoundInFirstNHits(
				Pair.of(
					"longDescription",
					"Something is rotten in the state of Denmark."
				), 3
			);
	}

	@Test
	public void test__Search__WithAggregation() throws Exception {
		// Here is how you can aggregate (ex: sumup, average) the values of
		// fields
		ESFactory esFactory = new ESTestHelpers(esVersion()).makeCartoonTestIndex();
		SearchAPI searchAPI = esFactory.searchAPI();
		Query queryBody = new Query(
			new JSONObject()
			.put("query_string", new JSONObject()
				.put("query", "surname:Simpson")
			)
		);

		Aggs aggsBody = new Aggs();
		aggsBody.aggregate("totalAge", "sum", "age");

		SearchResults<ShowCharacter> hits =
			searchAPI.search(queryBody, showCharacterProto, aggsBody);
		Long gotTotalAge = (Long) hits.aggrResult("totalAge",Long.class);
		Assertions.assertEquals(
			new Long(82), gotTotalAge,
			"Aggregated value not as expected");
	}

	@Test
	public void test__moreLikeThis__HappyPath() throws Exception {
		ESFactory factory = new ESTestHelpers(esFactory).makeHamletTestIndex();
		SearchAPI searchAPI = factory.searchAPI();
		Thread.sleep(1*1000);

		PlayLine queryLine = new PlayLine("Something is rotten in the kingdom of England");
		SearchResults<PlayLine> gotSearchResults =
			searchAPI.moreLikeThis(queryLine, new IncludeFields("^content$"));
		new AssertSearchResults(gotSearchResults)
			.fieldValueFoundInFirstNHits(
				Pair.of(
					"longDescription",
					"Something is rotten in the state of Denmark."),
				3
			);

	}

	@Test
	public void test__moreLikeThis__IndexWithSeveralTypes() throws Exception {
		ESFactory factory = new ESTestHelpers(esFactory).makeCartoonTestIndex();
		SearchAPI searchAPI = factory.searchAPI();
		Thread.sleep(1*1000);

		ShowCharacter queryPerson =
			new ShowCharacter("Lisa", "Simpson", "The Simpsons");
		SearchResults<ShowCharacter> gotSearchResults =
			searchAPI.moreLikeThis(queryPerson);
		new AssertSearchResults(gotSearchResults)
			.fieldValueFoundInFirstNHits(
				Pair.of(
					"id",
					"character:HomerSimpson"),
				3
			);

	}


	@Test
	public void test__moreLikeThese__HappyPath() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeHamletTestIndex();
		SearchAPI searchAPI = esFactory.searchAPI();
		Thread.sleep(1*1000);

		List<PlayLine> queryLines = new ArrayList<PlayLine>();

		queryLines.add(new PlayLine(
			"Something is rotten in the kingdom of England"));
		queryLines.add(new PlayLine(
			"To sing or not to sing that is the question"));

		SearchResults<PlayLine> gotSearchResults =
			searchAPI.moreLikeThese(queryLines, new IncludeFields("^content$"));
		new AssertSearchResults(gotSearchResults)
			.fieldValueFoundInFirstNHits(
				Pair.of(
					"content",
					"Something is rotten in the state of Denmark."),
				30
			);

		new AssertSearchResults(gotSearchResults)
			.fieldValueFoundInFirstNHits(
				Pair.of(
					"content",
					"To be, or not to be: that is the question:"),
				30
			);
	}

	@Test
	public void test__escapeQuotes__HappyPath() throws Exception {
		String query = "\"software development\" agile \"ui design\"";
		SearchAPI searchAPI = makeESFactory("some_index").searchAPI();
		String gotQuery = searchAPI.escapeQuotes(query);
		String expQuery = "\\\"software development\\\" agile \\\"ui design\\\"";
		AssertString.assertStringEquals(expQuery, gotQuery);
	}

	@Test
	public void test__scrollThroughSearchResults__HappyPath() throws Exception {
		esFactory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
		SearchAPI searchAPI = esFactory.searchAPI();
		Thread.sleep(1*1000);
		final String PLAY_LINE = "line";

		ESTestHelpers.PlayLine queryLine = new ESTestHelpers.PlayLine("say");
		SearchResults<ESTestHelpers.PlayLine> hits = searchAPI.moreLikeThis(queryLine, new IncludeFields("^content$"));

		int hitsCount = 0;
		Iterator<Hit<PlayLine>> iter = hits.iterator();
		while (iter.hasNext() && hitsCount < 26) {
			Hit<ESTestHelpers.PlayLine> scoredHit = iter.next();
			AssertString.assertStringContains("Hit did not fit query.", scoredHit.getDocument().getLongDescription(), "say", false);
			hitsCount++;
		}
		Assertions.assertTrue(
			hitsCount >= 25,
			"List of hits should have contained at least 25 hits, but only contained "+hitsCount);
	}

	@Test
	public void test__moreLikeThisJsonBody__HappyPath() throws Exception {
		ESFactory esFactory = new ESTestHelpers(esVersion()).makeEmptyTestIndex();
		SearchAPI searchAPI = esFactory.searchAPI();
		CrudAPI crudAPI = esFactory.crudAPI();
			new ESTestHelpers(esVersion()).makeEmptyTestIndex().searchAPI();
		ShowCharacter homer = new ShowCharacter("homer", "simpson", "The Simpsons");
		crudAPI.putDocument(homer);

		Map<String, Object> homerMap = new ObjectMapper().convertValue(homer, Map.class);
		String gotJson = searchAPI.moreLikeThisJsonBody(homer.type, homerMap);
		JSONObject expJSON = new JSONObject(
			"{\n" +
			"    \"highlight\": {\n" +
			"        \"fields\": {\n" +
			"            \"shortDescription\": {\n" +
			"                \"type\": \"plain\"\n" +
			"            },\n" +
			"            \"content\": {\n" +
			"                \"type\": \"plain\"\n" +
			"            }\n" +
			"        },\n" +
			"        \"order\": \"score\"\n" +
			"    },\n" +
			"    \"query\": {\n" +
			"       \"bool\":{\n"+
			"          \"must\": [\n"+
			"             {\n" +
			"                \"match\": {\n" +
			"                   \"type\": \"character\"\n" +
			"                }\n" +
			"             },\n" +
			"             {\n"+
			"                 \"more_like_this\": {\n" +
			"                     \"max_query_terms\": 12,\n" +
			"                     \"min_doc_freq\": 1,\n" +
			"                     \"like\": {\n" +
			"                         \"_index\": \"es-test\",\n" +
			"                         \"doc\": {\n" +
			"                             \"firstName\": \"homer\",\n" +
			"                             \"surname\": \"simpson\",\n" +
			"                             \"type\": \"character\"\n" +
			"                         }\n" +
			"                     },\n" +
			"                     \"min_term_freq\": 1,\n" +
			"                     \"fields\": [\n" +
			"                         \"firstName\",\n" +
			"                         \"surname\",\n" +
			"                         \"type\"\n" +
			"                     ]\n" +
			"                 }\n" +
			"             }\n"+
			"          ]\n"+
			"       }\n"+
			"    }\n" +
			"}"
		);

		JSONObject mlt = expJSON
			.getJSONObject("query")
				.getJSONObject("bool")
					.getJSONArray("must")
						.getJSONObject(1)
							.getJSONObject("more_like_this");

		if (esVersion() == 5) {
			mlt
				.getJSONObject("like")
					.put("_type", "character");
		} else {
			mlt
				.getJSONObject("like")
					.getJSONObject("doc")
						.put("type", "character");
		}
		AssertJson.assertJsonStringsAreEquivalent("",
			expJSON.toString(), gotJson);
	}

	@Test
	public void test__filterFields__ObjectWithPositiveFilter() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeCartoonTestIndex();
		IncludeFields filter = new IncludeFields("firstName");
		ShowCharacter pers = new ShowCharacter("homer", "simpson", "The Simpsons");
		esFactory.crudAPI().putDocument(pers);
		Map<String, Object> gotFilteredFields = esFactory.searchAPI().filterFields(pers, filter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("firstName", "homer");
		}
		AssertObject.assertDeepEquals("Positive filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__ObjectWithNegativeFilter() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeEmptyTestIndex();
		ExcludeFields filter = new ExcludeFields("firstName");
		ShowCharacter pers = new ShowCharacter("homer", "simpson");
		esFactory.crudAPI().putDocument(pers);
		Map<String, Object> gotFilteredFields = esFactory.searchAPI().filterFields(pers, filter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("type", "character");
			expFilteredFields.put("id", "character:homersimpson");
			expFilteredFields.put("idWithoutType", "homersimpson");
			expFilteredFields.put("surname", "simpson");
			expFilteredFields.put("lang", "en");
		}
		AssertObject.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__ObjectWithNullFilter() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeEmptyTestIndex();
		ExcludeFields nullFilter = null;
		ShowCharacter pers = new ShowCharacter("homer", "simpson");
		esFactory.crudAPI().putDocument(pers);
		Map<String, Object> gotFilteredFields = esFactory.searchAPI().filterFields(pers, nullFilter);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("id", "character:homersimpson");
			expFilteredFields.put("idWithoutType", "homersimpson");
			expFilteredFields.put("type", "character");
			expFilteredFields.put("firstName", "homer");
			expFilteredFields.put("surname", "simpson");
			expFilteredFields.put("lang", "en");
		}
		AssertObject.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}

	@Test
	public void test__filterFields__HappyPath() throws Exception {
		esFactory = new ESTestHelpers(esFactory).makeCartoonTestIndex();
		Document homer = new Document("homersimpson", "person");
		homer.setAdditionalField("first", "Homer");
		homer.setAdditionalField("last", "Simpson");
		homer.setCreationDate("2018-03-19");
		homer.setLongDescription("Homer is a character created created by Matt Groening in etc..");
		homer.setShortDescription("Homer is a the father of the Simpsons family");

		esFactory.crudAPI().putDocument(homer);
		Map<String, Object> gotFilteredFields = esFactory.searchAPI().filterFields(homer);
		Map<String,Object> expFilteredFields = new HashMap<String,Object>();
		{
			expFilteredFields.put("id", "person:homersimpson");
			expFilteredFields.put("idWithoutType", "homersimpson");
			expFilteredFields.put("type", "person");
			expFilteredFields.put("lang", "en");

			expFilteredFields.put("content", "Homer is a character created created by Matt Groening in etc..");
			expFilteredFields.put("shortDescription", "Homer is a the father of the Simpsons family");

			expFilteredFields.put("additionalFields.first", "Homer");
			expFilteredFields.put("additionalFields.last", "Simpson");
		}
		AssertObject.assertDeepEquals("Negative filter did not produce expected field names", expFilteredFields, gotFilteredFields);
	}
}
