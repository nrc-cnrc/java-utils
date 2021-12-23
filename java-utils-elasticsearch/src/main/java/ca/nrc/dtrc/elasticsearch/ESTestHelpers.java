package ca.nrc.dtrc.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ca.nrc.datastructure.Cloner;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import ca.nrc.dtrc.elasticsearch.engine.MissingESPluginException;
import ca.nrc.dtrc.elasticsearch.es5.ES5Factory;
import ca.nrc.dtrc.elasticsearch.es7.ES7Factory;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.json.JSONUtils;
import ca.nrc.file.ResourceGetter;

public class ESTestHelpers {

	protected  ESFactory esFactory = null;

	private static final PlayLine playLinePrototype = new PlayLine();
	
	public static final long SHORT_WAIT = 1000;
	public static final long LONG_WAIT = 2*SHORT_WAIT;
	public static final long EXTRA_LONG_WAIT = 4*LONG_WAIT;
	
	public static List<String> indicesToBeCleared = new ArrayList<String>();

	private static ObjectMapper mapper = new ObjectMapper();

	public static final String emptyTestIndex = "es-test";
	
	public static final String hamletTestIndex = "es-test-hamlet";
	public static final String hamletType = "hamlet_lines";

	public static String hamletFirstLine =
		"{\n" +
		"    \"_detect_language\": true,\n" +
		"    \"additionalFields\": {\n" +
		"        \"line_id\": 32439,\n" +
		"        \"line_number\": \"1.1.4\",\n" +
		"        \"play_name\": \"Hamlet\",\n" +
		"        \"speaker\": \"FRANCISCO\",\n" +
		"        \"speech_number\": 4\n" +
		"    },\n" +
		"    \"content\": \"Bernardo?\",\n" +
		"    \"creationDate\": null,\n" +
		"    \"id\": \"playline:1.1.4\",\n" +
		"    \"idWithoutType\": \"1.1.4\",\n" +
		"    \"lang\": \"en\",\n" +
		"    \"longDescription\": \"Bernardo?\",\n" +
		"    \"shortDescription\": null,\n" +
		"    \"type\": \"playline\"\n" +
		"}";

	public static String hamletLastLine =
			"{\n" +
			"    \"_detect_language\": true,\n" +
			"    \"additionalFields\": {\n" +
			"        \"line_id\": 36676,\n" +
			"        \"line_number\": \"5.2.425\",\n" +
			"        \"play_name\": \"Hamlet\",\n" +
			"        \"speaker\": \"PRINCE FORTINBRAS\",\n" +
			"        \"speech_number\": 147\n" +
			"    },\n" +
			"    \"content\": \"A dead march. Exeunt, bearing off the dead bodies; after which a peal of ordnance is shot off\",\n" +
			"    \"creationDate\": null,\n" +
			"    \"id\": \"playline:5.2.425\",\n" +
			"    \"idWithoutType\": \"5.2.425\",\n" +
			"    \"lang\": \"en\",\n" +
			"    \"longDescription\": \"A dead march. Exeunt, bearing off the dead bodies; after which a peal of ordnance is shot off\",\n" +
			"    \"shortDescription\": null,\n" +
			"    \"type\": \"playline\"\n" +
			"}"
			;

	static {
		hamletFirstLine = jsonDeformat(hamletFirstLine);
		hamletLastLine = jsonDeformat(hamletLastLine);
	}

	public static final String cartoonsTestIndex = "es-test-cartoons";


	public static String jsonDeformat(String json) {
		json = json.replaceAll("\n\\s*", "");
		json = json.replaceAll(",\\s*(\"|\\d|\\{|\\[|null|true|false)", ",$1");
		json = json.replaceAll("\"\\s*:\\s*(\\{|\\[|\"|\\d|null|true|false)", "\":$1");
		return json;
	}

	public ESTestHelpers(ESFactory _esFactory) throws ElasticSearchException {
		this.esFactory = _esFactory;
	}

	public ESTestHelpers(Integer version) throws ElasticSearchException {
		if (version != null) {
			if (version <= 5) {
				esFactory = new ES5Factory("").setPort(9205);
			} else if (version <= 7) {
				esFactory = new ES7Factory("").setPort(9207);
			} else {
				throw new ElasticSearchException("Unsupported ESFactory version "+version);
			}
		}
		return;
	}

	public Integer esVersion() {
		return esFactory.version();
	}

	protected ESFactory es(String indexName) throws ElasticSearchException {
		esFactory.indexName = indexName;
		return esFactory;
	}

	public boolean skipTestsUnlessESIsRunning(int port) throws Exception {
		Boolean skipTests = null;
		String errMess =
			"ElasticSearch v"+esVersion()+" is either not installed or is not running.\n\n"
			+ "If you want to test the ESFactory utilities, install ESFactory and make sure to start it using the command line 'elasticsearch'.\n\n"
			+ "For now, skipping all remaining ElasticSearch tests.";
		// Check to see if the correct version of ElasticSearch is running.
		try {
			skipTests = !new ESVersionChecker("localhost", port).isRunningVersion(esVersion());
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
				Assert.fail(errMess);
			}
		}
		org.junit.Assume.assumeFalse(errMess, skipTests);
		return skipTests;
	}

	public boolean skipTestsUnlessPluginIsInstalled(String plugin) throws Exception {
		boolean skip = false;
		try {
			esFactory.engineAPI().ensurePluginInstalled(plugin);
		} catch (MissingESPluginException e) {
			skip = true;
			org.junit.Assume.assumeFalse(
				"ES plugin not installed: "+plugin, skip);
		}
		return skip;
	}


	public ESFactory makeEmptyTestIndex() throws Exception {
		ESFactory factory = Cloner.clone(esFactory);
		factory.updatesWaitForRefresh = true;
		factory.indexName = emptyTestIndex;

		factory.indexAPI().delete();
		factory.indexAPI().create();
		return factory;
	}

	public static final ShowCharacter showCharacterProto = new ShowCharacter();
	public static final ShowCharacter homer =
		new ShowCharacter("Homer", "Simpson", "The Simpsons")
			.setAge(42);

	public static final TVShow tvShowProto = new TVShow();
	public static final TVShow theSimpsons = new TVShow("The Simpsons");

	public ESFactory makeCartoonTestIndex() throws Exception {
		return makeCartoonTestIndex((Boolean)null);
	}

	public ESFactory makeCartoonTestIndex(Boolean deleteIndex) throws Exception {
		if (deleteIndex == null) {
			deleteIndex = false;
		}
		ESFactory factory = Cloner.clone(esFactory);
		factory.indexName = cartoonsTestIndex;
		if (deleteIndex) {
			factory.indexAPI().delete();
			factory.indexAPI().define(true);
		} else {
			factory.indexAPI().clear();
		}
		CrudAPI crudAPI = factory.crudAPI();

		// Sleep a bit to give the ESFactory server to propagate the index to
		// all nodes in its cluster
		Thread.sleep(1000);

		crudAPI.putDocument(theSimpsons);
		crudAPI.putDocument(homer);
		crudAPI.putDocument(
			new ShowCharacter("Marge", "Simpson", "The Simpsons")
			.setAge(40));
		crudAPI.putDocument(new ShowCharacter("Mr", "Burns", "The Simpsons"));

		crudAPI.putDocument(new TVShow("Peanuts"));
		crudAPI.putDocument(new ShowCharacter("Charlie", "Brown", "Peanuts"));
		crudAPI.putDocument(new ShowCharacter("Lucy", "", "Peanuts"));

		// Sleep a bit to give the ESFactory server to propagate the index to
		// all nodes in its cluster
		Thread.sleep(2*1000);

		return factory;
	}

	public ESFactory makeHamletTestIndex() throws Exception {
		return makeHamletTestIndex((String)null);
	}

	public ESFactory makeHamletTestIndex(String collectionName) throws Exception {
		return makeHamletTestIndex(collectionName, (Boolean)null);
	}

	public ESFactory makeHamletTestIndex(Boolean deleteIndex) throws Exception {
		return makeHamletTestIndex((String)null, deleteIndex);
	}


	public ESFactory makeHamletTestIndex(String collectionName, Boolean deleteIndex) throws Exception {
		if (deleteIndex == null) {
			deleteIndex = false;
		}
		ESFactory factory = Cloner.clone(esFactory);
		factory.indexName = hamletTestIndex;
		// Put a two second delay after each transaction, to give ESFactory time to synchronize all the nodes.
		factory.sleepSecs = 2.0;
		if (deleteIndex) {
			factory.indexAPI().delete();
			factory.indexAPI().define(true);
		} else {
			factory.indexAPI().clear();
		}

		String fPath = hamletJsonFile();
		if (collectionName == null) {
			factory.indexAPI().bulk(new File(fPath), PlayLine.class);
		} else {
			factory.indexAPI().bulk(new File(fPath), collectionName);
		}

		// Sleep a bit to give the ESFactory server to propagate the index to
		// all nodes in its cluster
		Thread.sleep(1000);

		return factory;
	}

	public String testFilesPath(String relPath) throws ElasticSearchException {
		int forVersion = esFactory.version();
		if (forVersion > 7) {
			forVersion = 7;
		}
		if (relPath == null) {
			relPath = "";
		}
		relPath = "test_data/ca/nrc/dtrc/elasticsearch/es"+forVersion+"/"+relPath;
		String fPath = null;
		try {
			fPath = ResourceGetter.getResourcePath(relPath);
		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}
		return fPath;

	}

	public String hamletJsonFile() throws ElasticSearchException {
		return testFilesPath("hamlet.json");
	}

	public static void assertNoError(String jsonResponse) throws JsonParseException, JsonMappingException, IOException {
		Map<String,Object> respObject = JSONUtils.json2ObjectMap(jsonResponse);		
		Assert.assertFalse("The jsonResponse should not have been an error.\nBut it was:\n"+jsonResponse, respObject.containsKey("error"));
		
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

	public void clearTestIndices() throws IOException, ElasticSearchException, InterruptedException {
		for (String index: indicesToBeCleared) {
			es(index).client().clearIndex();
		}
	}

	public void clearIndexCollection(String index, String collection) throws ElasticSearchException {
		esFactory.indexAPI().clear(collection);
	}

	/////////////////////////////////////////
	// Classes of documents used for testing
	/////////////////////////////////////////

	public static class PlayLine extends Document {
		public PlayLine() {
			init__PlayLine(null, null);
		}

		public PlayLine(String _text_entry) {
			init__PlayLine(null, _text_entry);
		}

		public PlayLine(int _line_id, String _text_entry) {
			init__PlayLine(_line_id, _text_entry);
		}

		private void init__PlayLine(Integer _line_id, String _text_entry) {
        	type = "playline";
        	if (_line_id != null) {
				this.setId(Integer.toString(_line_id));
			}
        	if (_text_entry != null) {
				this.setLongDescription(_text_entry);
			}
		}
	}

	public static class SimpleDoc extends Document {
		public String category = null;

        public SimpleDoc() {
        	initialize(null, null, null);
		  }

        public SimpleDoc(String _id, String _content) {
        	initialize(_id, _content, null);
        }

        public SimpleDoc(String _id, String _content, String _category) {
        	initialize(_id, _content, _category);
        }

        public void initialize(String _id, String _content, String _category) {
        	this.type = "simpledoc";
        	setId(_id);
        	setContent(_content);
        	this.category = _category;
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

		public ShowCharacter(String _firstName, String _surname) {
			super();
			init_ShowCharacter(_firstName, _surname, null);
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

}
