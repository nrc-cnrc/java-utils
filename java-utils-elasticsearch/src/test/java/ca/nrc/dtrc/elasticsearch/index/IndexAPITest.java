package ca.nrc.dtrc.elasticsearch.index;

import ca.nrc.dtrc.elasticsearch.*;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;
import static ca.nrc.dtrc.elasticsearch.ESFactory.*;
import static ca.nrc.dtrc.elasticsearch.ESTestHelpers.*;

import ca.nrc.file.ResourceGetter;
import ca.nrc.testing.AssertFile;
import ca.nrc.testing.AssertObject;
import ca.nrc.testing.AssertString;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class IndexAPITest {

	protected abstract ESFactory makeESFactory(String indexName) throws ElasticSearchException;
	protected abstract int esVersion();
	protected abstract String expMappingUrl(String typeName);
	protected abstract String expAllMappingUrl(String typeName);
	protected abstract String expDeleteByQueryUrl(String docType);
	protected abstract JSONObject expTypes(Class<? extends Document> docClass) throws Exception;


	protected ESFactory esFactory = null;

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9207);
		return;
	}


	@BeforeEach
	public void setUp() throws Exception {
		esFactory = makeESFactory("test-index");
	}

	/////////////////////////////////////////
	// DOCUMENTATION TESTS
	/////////////////////////////////////////

	@Test
	public void test__IndexAPI__Synopsis() throws Exception {
		// Use this API to create, configure, and delete ESFactory indices.
		// The class is abstract and you need to instantiate a concrete
		// subclass that is designed specifically for the version of ESFactory
		// that is running
		IndexAPI indexAPI = makeESFactory("test-index").indexAPI();

		// This is how you define an index.
		// Note that this is optional. You can add documents to an index without
		// first defining it. ESFactory will then automatically create an index with
		// default configuration.
		//
		// However if you need an index that is configured differently than the
		// default, you SHOULD define it BEFORE you add any documents to it.
		//
		// If you pass true as second argument, the API will delete the index if
		// it already exists. If second argument is false, the API will prompt
		// before deleting an existing index.
		//
		IndexDef iDef = new IndexDef();
		boolean deleteIfExists = true;
		indexAPI.define(iDef, deleteIfExists);

		// You can check for the existence of an index
		if (indexAPI.exists()) {
			// Do some stuff...
		}

		// Or, you can check if the index is empty
		if (indexAPI.isEmpty()) {
			// Do some stuff....
		}

		// You can get the configuration of the index
		iDef = indexAPI.definition();

		// You can get the types of the fields in an index
		// Note this only works if the type exists
		try {
			Map<String,String> types = indexAPI.fieldTypes("sometype");
			for (String fieldName: types.keySet()) {
				String fieldType = types.get(fieldName);
			}
		} catch (NoSuchTypeException e) {
			// Note: The call to fieldTypes will raise an exception if the type
			//   does not exist.
		}

		//
		// You can change the settings of the index (assuming it has already been created)
		//
		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put("index.mapping.total_fields.limit", new Integer(2000));
		indexAPI.changeIndexSettings(settings);

		// You can list all documents of a particular type.
		// For example, this lists all documents of the type
		// TVShow.
		TVShow docProto = new TVShow();
		SearchResults<TVShow> results = indexAPI.listAll(docProto);
		// You can then iterate through all the hits.
		Iterator<Hit<TVShow>> iter = results.iterator();
		while (iter.hasNext()) {
			TVShow show = iter.next().getDocument();
		}

		// You can also loop through the doc IDs instead of docs
		results = indexAPI.listAll(docProto);
		Iterator<String> idsIter = results.docIDIterator();
		while (idsIter.hasNext()) {
			String docID = idsIter.next();
		}

		// You can clear all documents of a particular type from the index
		indexAPI.clear("character");

		// Or, you can clear all documents from the index altogether.
		// Note that this will not remove the index configuration
		indexAPI.clear();

		// You can delete an index
		indexAPI.delete();

		// You can do a bunch of ES operations in bulk
		String bulkFile = new ESTestHelpers(esFactory).hamletJsonFile();
		indexAPI.bulk(new File(bulkFile), "playline");
	}

	/////////////////////////////////////////
	// VERIFICATION TESTS
	/////////////////////////////////////////

	@Test
	public void test__define_exists_delete__HappyPath() throws Exception {
		String indexName = "test-index";
		IndexAPI index = makeESFactory(indexName).indexAPI();

		// First, make sure the index does not exist
		index.delete();

		// Then define it.
		index.define(new IndexDef(), true);
		Assertions.assertTrue(
			index.exists(),
			"Index SHOULD have existed after creation");

		index.delete();
		Assertions.assertFalse(
			index.exists(),
			"Index should NOT have existed after deletion");
	}

	@Test
	public void test__define_definition__HappyPath() throws Exception {
		String indexName = "test-index";
		IndexAPI indexAPI = makeESFactory(indexName).indexAPI();

		IndexDef definition = new IndexDef(indexName)
			.setTotalFieldsLimit(9999);
		indexAPI.define(definition, true);

		IndexDef gotDef = indexAPI.definition();
		AssertObject.assertDeepEquals(
			"Retrieved index definition was not as expected",
			definition, gotDef
		);
		return;
	}

	@Test
	public void test__fieldTypes__HappyPath() throws Exception {
		ESFactory esFactory =
			new ESTestHelpers(esVersion()).makeCartoonTestIndex(true);
		IndexAPI indexAPI = esFactory.indexAPI();

		String type = new ShowCharacter().type;
		Map<String, String> gotTypes = indexAPI.fieldTypes(type);

		JSONObject expTypes = expTypes(ShowCharacter.class);
		AssertObject.assertEqualsJsonCompare(
			"Field types not as expected",
			expTypes.toString(), gotTypes
		);

	}

	@Test
	public void test__isEmpty__HappyPath() throws Exception {
		String indexName = "test-index";
		ESFactory factory = makeESFactory(indexName);
		ShowCharacter homer = new ShowCharacter("Homer", "Simpson", "The Simpsons");

		IndexAPI index = factory.indexAPI();
		index.delete();
		Assertions.assertTrue(
			index.isEmpty(),
			"Type SHOULD have been empty after index deletion");

		factory.crudAPI().putDocument(homer);

		Thread.sleep(2*1000);

		Assertions.assertFalse(
			index.isEmpty(),
			"Type should NOT have been empty after index deletion");

		index.clear();
		Thread.sleep(2*1000);

		Assertions.assertTrue(
			index.isEmpty(),
			"Index SHOULD have been empty after clearing");
	}

	@Test
	public void test__indexExists__HappyPath() throws Exception {
		String indexName = "test-index";
		ESFactory factory = makeESFactory(indexName);
		IndexAPI index = factory.indexAPI();
		index.delete();
		Assertions.assertFalse(
			index.exists(),
			"Index "+indexName+" should NOT have existed at the start of test");

		ShowCharacter homer = new ShowCharacter("Homer", "Simpson", "The Simpsons");
		String jsonResponse = factory.crudAPI().putDocument(homer);
		Assertions.assertTrue(
			index.exists(),
			"Index "+indexName+" SHOULD have existed after we added a document to it.");
	}

	@Test
	public void test__listAll__HappyPath() throws Exception {
		ESFactory factory = new ESTestHelpers(esVersion()).makeCartoonTestIndex();

		SearchResults<ShowCharacter> characterResults =
			factory.indexAPI().listAll(showCharacterProto);
		new AssertSearchResults(characterResults, "List of ShowCharacter not as expected")
			.hitIDsEqual(
				"CharlieBrown", "HomerSimpson", "Lucy",
				"MargeSimpson", "MrBurns");

		SearchResults<TVShow> showResults =
			factory.indexAPI().listAll(tvShowProto);
		new AssertSearchResults(showResults, "List of TVShow not as expected")
			.hitIDsEqual("Peanuts","The Simpsons");
	}


	@Test
	public void test_clear__HappyPath() throws Exception {

		ESFactory factory = new ESTestHelpers(esVersion()).makeCartoonTestIndex();
		IndexAPI index = factory.indexAPI();

		new AssertIndex(factory)
			.typeNotEmpty(showCharacterProto)
			.typeNotEmpty(tvShowProto);

		// Now clear one of the two docTypes and check that only that one type is
		// empty
		index.clear(showCharacterProto.type);
		new ESTestHelpers(esVersion()).sleepExtraLongTime();
		new AssertIndex(factory)
			.typeIsEmpty(showCharacterProto)
			.typeNotEmpty(tvShowProto);

		// Now clear the other docType and check that both types are now
		// empty
		index.clear(tvShowProto.type);
		new ESTestHelpers(esVersion()).sleepExtraLongTime();
		new AssertIndex(factory)
			.typeIsEmpty(showCharacterProto)
			.typeIsEmpty(tvShowProto);
	}


	@Test
	public void test__props2map__HappyPath() throws Exception {
		Map<String,Object> props = new HashMap<String,Object>();

		Map<String,Object> esSettings = new HashMap<String,Object>();
		Map<String,Object> A = new HashMap<String,Object>();
		esSettings.put("A", A);
		{
			Map<String,Object> A_x = new HashMap<String,Object>();
			A.put("x", A_x);
			{
				A_x.put("hello", "world");
				A_x.put("num", 1000);
			}
			Map<String,Object> A_y = new HashMap<String,Object>();
			A.put("y", A_y);
			{
				A_y.put("greetings", "universe");
			}
		}

		Map<String,Object> gotSettings = IndexDef.tree2props(esSettings);
		Map<String,Object> expSettings = new HashMap<String,Object>();
		{
			expSettings.put("A.y.greetings", "universe");
			expSettings.put("A.x.num", 1000);
			expSettings.put("A.x.hello", "world");
		}

		AssertObject.assertDeepEquals("Converted settings not as expected",
				expSettings, gotSettings);
	}

	@Test
	public void test__bulkIndex__HappyPath() throws Exception {
		ESTestHelpers helpers = new ESTestHelpers(esVersion());
		esFactory = helpers.makeEmptyTestIndex();
		IndexAPI indexAPI = esFactory.indexAPI();

		File jsonFile = ResourceGetter.copyResourceToTempLocation("test_data/ca/nrc/dtrc/elasticsearch/small_index_content.json");
		Boolean force = true;
		indexAPI.bulkIndex(jsonFile.getAbsolutePath(), ESFactory.ESOptions.CREATE_IF_NOT_EXISTS);

		Thread.sleep(ESTestHelpers.LONG_WAIT);

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
		IndexAPI indexAPI = esFactory.indexAPI();
		AssertIndex asserter = new AssertIndex(esFactory);
		Document protoDoc = new Document();

		File jsonFile = ResourceGetter.copyResourceToTempLocation(
			"test_data/ca/nrc/dtrc/elasticsearch/small_index_content.json");
		Boolean force = true;

		// Bulk index first file
		indexAPI.bulkIndex(jsonFile.getAbsolutePath(), ESOptions.CREATE_IF_NOT_EXISTS);
		Thread.sleep(new ESTestHelpers(esVersion()).LONG_WAIT);
		String[] expDocIDs = new String[] {
			"For whom the bell tolls",
			"The old man and the sea"
		};
		asserter.docsInTypeEqual("books", protoDoc, expDocIDs);

		// Bulk index second file into same type of same index, using APPEND mode
		jsonFile = ResourceGetter.copyResourceToTempLocation("test_data/ca/nrc/dtrc/elasticsearch/other_small_index_content.json");
		boolean append = true;
		indexAPI.bulkIndex(jsonFile.getAbsolutePath(),
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
		IndexAPI indexAPI = new ESTestHelpers(esVersion()).makeCartoonTestIndex().indexAPI();

		String typeToSearch = showCharacterProto.type;

		//
		// Try to listAll that collection, giving it a prototype of
		// the wrong type (Playline)
		//
		PlayLine badProto = new PlayLine();

		Assertions.assertThrows(BadDocProtoException.class, () -> {
			SearchResults<PlayLine> result =
			indexAPI.listAll(typeToSearch, badProto);
		});
	}

	@Test
	public void test__dumpToFile__DumpAll() throws Exception {
		esFactory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
		IndexAPI indexAPI = esFactory.indexAPI();
		esFactory.sleep(2);
		File gotFile =
			Files.createTempFile("test", "json", new FileAttribute[0])
				.toFile();
		indexAPI.dumpToFile(gotFile, PlayLine.class);

		AssertFile.assertFileContains(
			"Dumped index was missing the first document",
			gotFile, new ESTestHelpers(esVersion()).hamletFirstLine, true, false);
		AssertFile.assertFileContains(
			"Dumped index was missing the last document",
			gotFile, new ESTestHelpers(esVersion()).hamletLastLine, true, false);
	}

	@Test
	public void test__dumpToFile__DumpMatchingDocs() throws Exception {
		esFactory = new ESTestHelpers(esVersion()).makeHamletTestIndex();
		IndexAPI indexAPI = esFactory.indexAPI();
		File gotFile =
			Files.createTempFile("test", "json", new FileAttribute[0])
			.toFile();
		String query = "additionalFields.speaker:FRANCISCO";
		indexAPI.dumpToFile(gotFile, PlayLine.class,query, (Set)null);

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
		IndexAPI indexAPI =
			new ESTestHelpers(esVersion()).makeHamletTestIndex().indexAPI();
		File gotFile =
			Files.createTempFile("test", "json", new FileAttribute[0])
			.toFile();
		String nullQuery = null;
		indexAPI.dumpToFile(gotFile, PlayLine.class, nullQuery, (Set)null);

		AssertFile.assertFileContains(
			"Dumped index is missing the first line of the play",
			gotFile, new ESTestHelpers(esVersion()).hamletFirstLine,
			true, false);
		AssertFile.assertFileContains(
			"Dumped index is missing the last line of the play",
			gotFile, new ESTestHelpers(esVersion()).hamletLastLine,
			true, false);
	}

	@Test
	public void test__url4singletypeMappings() throws Exception {
		String docType = "sometype";
		URL gotURL = esFactory.indexAPI().url4singletypeMappings(docType);
		AssertString.assertStringEquals(
			"mapping URL not as expected",
			expMappingUrl(docType), gotURL.toString());
	}

	@Test
	public void test__url4indexDef() throws Exception {
		String docType = "sometype";
		URL gotURL = esFactory.indexAPI().url4indexDef();
		AssertString.assertStringEquals(
			"mapping URL not as expected",
			expAllMappingUrl(docType), gotURL.toString());
	}

	@Test
	public void test__url4deleteByQuery() throws Exception {
		String docType = "sometype";
		URL gotURL = esFactory.indexAPI().url4deleteByQuery(docType);
		AssertString.assertStringEquals(
			"mapping URL not as expected",
			expDeleteByQueryUrl(docType), gotURL.toString());
	}

	@Test
	public void test__exists__HappyPath() throws Exception {
		IndexAPI indexAPI = esFactory.indexAPI();

		indexAPI.delete();
		Assertions.assertFalse(indexAPI.exists(),
			"Index should NOT have existed after it was deleted");

		indexAPI.create();
		Assertions.assertTrue(indexAPI.exists(),
			"Index SHOULD have existed after it was created");

		indexAPI.clear();
		Assertions.assertTrue(indexAPI.exists(),
			"Index should STILL have existed even after it was cleared");
	}
}
