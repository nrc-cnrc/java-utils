package ca.nrc.config;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.nrc.testing.AssertString;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import ca.nrc.testing.AssertHelpers;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class ConfigTest {

	@Rule
	public final EnvironmentVariables environmentVariables
			= new EnvironmentVariables();

	@Before
	public void setUp() throws Exception {
		File propFile =
				createTempPropFileWithContent(
					"com.acme.prop1=Hello World\n"+
					"com.acme.prop2=Greetings Universe\n"+
					"com.acme.prop3=Salutations to the planet"
				);
		environmentVariables.set("com_acme", propFile.toString());
	}

	///////////////////////////////////////////////
	// DOCUMENTATION TESTS
	///////////////////////////////////////////////

	@Test
	public void test__Config__Synopsis() throws Exception {
		// Config provides a lightweight yet flexible way of specifying
		// configuration properties.
		//
		// Among other things, it allows you to specify a bunch of "related"
		// properties in a same file.
		//
		// For example, say you have a bunch of properties whose names start
		// with "com.acme". You can create a .properties file that contains
		// those properties...
		//
		// Step 1: create the file with those propeties
		//
		File propFile =
			createTempPropFileWithContent(
				"com.acme.prop1=Hello World\n"+
				"com.acme.prop2=Greetings Universe\n"+
				"com.acme.prop3=Salutations to the planet"
			);

		// Step 2: set environment variable or Java -D option whose value
		// points to the path of the props file.
		//
		// IMPORTANT: If you use environment variables, then the name of the
		// variable should use _ instead of dots because some shells don't
		// allow dots in environment variable names.
		//
		environmentVariables.set("com_acme", propFile.toString());

		// From here on, any property whose name starts with 'com.acme' may
		// be retrieved from the provided prop file, unless is it found in
		// some other place that takes precedence over that file (more on that
		// later)
		//
		String prop1Value = Config.getConfigProperty("com.acme.prop1");
		String prop2Value = Config.getConfigProperty("com.acme.prop2");
		String prop3Value = Config.getConfigProperty("com.acme.prop3");

		// You can also set individual config properties more directly by
		// providing their value as an environment variable or java -D runtime
		// option.
		//
		// IMPORTANT: Again here, if you use an environment variable, you must
		// use _ instead of dots in the variable's name.
		//
		environmentVariables.set("com_acme_prop4", "yo earth!");
		String prop4Value = Config.getConfigProperty("com.acme.prop4");

		// Note that in the above, the directly set value would take precedence
		//   over any valuet that might be specified in a properties file.
		//
		// For example, this will override the value of prop3  defined
		//   in the properties file that we defined earlier.
		//
		environmentVariables.set("com_acme_prop3", "S'up Gaia?");

		// By default, if a property cannot be found, a
		// ConfigPropNotFoundException will be raised
		String propValue = null;
		try {
			propValue = Config.getConfigProperty("nonexistantPropName");
			Assert.fail("We should never make it here because the prop name does not exist");
		} catch (ConfigPropNotFoundException e) {
			// This is OK. We expect the exception to be raised.
		}

		// However, you can make getConfigProperty() return a null value
		//   if the prop is not found
		//
		boolean failIfAbsent = false;
		propValue =
			Config.getConfigProperty("nonexistantPropName", failIfAbsent);

		// You can also provide a non-null default value which will be returned
		//   if the property is not found.
		//
		String defValue = "hello";
		propValue = Config.getConfigProperty("nonexistantPropName", defValue);

		// You can use properties to store JSON serialisations of any data
		// type.
		// For example...
		//
		// Integer type:
		//
		String propName = "com.acme.someint";
		environmentVariables.
			set(propName.replaceAll("\\.", "_"), "13");
		Integer intPropVal = Config.getConfigProperty(propName, Integer.class);

		// Map type:
		//
		propName = "com.acme.somemap";
		environmentVariables.
				set(propName.replaceAll("\\.", "_"),
					"{\"firstname\": \"Homer\", \"surname\": \"Simpson\"}");
		Map<String,String> mapPropVal =
			Config.getConfigProperty(propName, Map.class);

		// Note: If you provide a default value, then you don't need to
		//   specify the class of the return value, as it will be assumed
		//   to be the same as the class of the provided default
		//
		String[] defNames = new String[] {"home", "marge"};
		String[] names = Config.getConfigProperty("com.acme.names", defNames);
	}

	///////////////////////////////////////////////
	// VERIFICATION TESTS
	///////////////////////////////////////////////

	@Test
	public void test__getConfigProperty__ExistingProp() throws Exception {
		new AssertConfig("")
			.assertConfigPropertyEquals(
				"Hello World", "com.acme.prop1");
	}

	@Test(expected = ConfigPropNotFoundException.class)
	public void test__getConfigProperty__NonExistantProp__NODefaultValue__RaisesException() throws Exception {
		new AssertConfig("")
			.assertConfigPropertyEquals(
					null, "com.acme.nonexistantprop");
	}

	@Test
	public void test__getConfigProperty__NonExistantProp__WITHDefaultValue__RaisesException() throws Exception {
		String defaultVal = "defaultval";
		new AssertConfig("")
			.assertConfigPropertyEquals(
				defaultVal, "com.acme.nonexistantprop", defaultVal);
	}

	@Test
	public void test__getConfigProperty__IntegerValue() throws Exception {
		String propName = "com.acme.someint";
		environmentVariables.
			set(propName.replaceAll("\\.", "_"), "13");
		Integer expValue = new Integer(13);
		new AssertConfig("")
			.assertConfigPropertyEquals(expValue, propName, Integer.class);
	}

	@Test
	public void test__getConfigProperty__MapValue() throws Exception {
		String propName = "com.acme.somemap";
		Map<String,String> expValue = new HashMap<String,String>();
		{
			expValue.put("name", "homer simpson");
		}

		String propJson = new ObjectMapper().writeValueAsString(expValue);
		environmentVariables.
			set(propName.replaceAll("\\.", "_"), propJson);
		new AssertConfig("")
			.assertConfigPropertyEquals(expValue, propName, Map.class);
	}

	@Test
	public void test__getConfigProperty__ObjectArrayValue() throws Exception {
		String propName = "com.acme.somemap";
		String[] expValue = new String[] {"homer", "simpson"};
		String propJson = new ObjectMapper().writeValueAsString(expValue);
		environmentVariables.
				set(propName.replaceAll("\\.", "_"), propJson);
		new AssertConfig("")
			.assertConfigPropertyEquals(expValue, propName, String[].class);
	}

	@Test
	public void test__getConfigProperty__PrimitiveTypeArrayValue() throws Exception {
		String propName = "com.acme.somemap";
		int[] expValue = new int[] {1, 2, 3};
		String propJson = new ObjectMapper().writeValueAsString(expValue);
		environmentVariables.
			set(propName.replaceAll("\\.", "_"), propJson);
		new AssertConfig("")
			.assertConfigPropertyEquals(expValue, propName, int[].class);
	}

	@Test
	public void test__getConfigProperty__DefaultValueIsAString() throws Exception {
		String propName = "com.acme.nonexistantstring";
		String defValue = "hello";
		String expValue = defValue;
		new AssertConfig("")
			.assertConfigPropertyEquals(expValue, propName, defValue);
	}

	@Test
	public void test__possiblePropFileNames__HappyPath() throws Exception {
		
		String propName = "ca_nrc_javautils_someprop";
		List<String> gotPossibleFNames = Config.possiblePropFileNames(propName);
		String[] expPossibleFNames = new String[] {"ca_nrc_javautils", "ca_nrc", "ca"};
		AssertHelpers.assertDeepEquals("", expPossibleFNames, gotPossibleFNames);
	}
	
	@Test(expected = ConfigException.class)
	public void test__getConfigProperty__UndefinedProp__ThrowsConfigException() throws Exception {
		String propName = "does_not_exist_prop";
		String gotProp = Config.getConfigProperty(propName);
	}
	
	@Test
	public void test__changePropNameFormat__HappyPath() throws Exception {
		AssertString.assertStringEquals("Proerty name not correctly converted from '_' to '.' format",
				"ca.nrc.javautils.bingKey", Config.changePropNameFormat("ca_nrc_javautils_bingKey"));
		AssertString.assertStringEquals("Proerty name not correctly converted from '.' to '_' format",
				"ca_nrc_javautils_bingKey", Config.changePropNameFormat("ca.nrc.javautils.bingKey"));
	}

	@Test(expected=ConfigException.class)
	public void test__changePropNameFormat__PropNameContainsBothDotsAndUndscores_ThrowsConfigException() throws Exception {
		String propName = "ca.nrc.javautils.some_prop";
		Config.changePropNameFormat(propName); // Should raise exception
	}

	@Test
	public void test__parsePropValue__StringNotQuoted() throws Exception {
		String propStr = "hello";
		String expValue = propStr;
		new AssertConfig("")
			.assertParsedPropValueIs(expValue, propStr, String.class);
	}

	@Test
	public void test__parsePropValue__StringISQuoted() throws Exception {
		String propStr = "\"hello\"";
		String expValue = "hello";
		new AssertConfig("")
			.assertParsedPropValueIs(expValue, propStr, String.class);
	}

	@Test
	public void test__parsePropValue__Integer() throws Exception {
		String propStr = "12";
		Integer expValue = new Integer(12);
		new AssertConfig("")
			.assertParsedPropValueIs(expValue, propStr, Integer.class);
	}

	@Test
	public void test__parsePropValue__MapOfMaps() throws Exception {
		String propStr =
			"{\"homer\": {\"gender\": \"m\"}, \"marge\": {\"gender\": \"f\"}}";
		Map<String, Map<String,String>> expValue =
			new HashMap<String, Map<String,String>>();
		{
			Map<String,String> elt = new HashMap<String,String>();
			elt.put("gender", "m");
			expValue.put("homer", elt);

			elt = new HashMap<String,String>();
			elt.put("gender", "f");
			expValue.put("marge", elt);
		}

		new AssertConfig("")
			.assertParsedPropValueIs(expValue, propStr, Map.class);
	}

	@Test(expected = ConfigException.class)
	public void test__parsePropValue__StringDoesNotCorrespondToTheType__RaisesException() throws Exception {
		String propStr = "hello";
		new AssertConfig("")
			.assertParsedPropValueIs(null, propStr, Integer.class);
	}

	@Test(expected = ConfigException.class)
	public void test__parsePropValue__PropStringIsNotValidJson__RaisesException() throws Exception {
		String propStr = "{xyz}";
		Map<String,String> expValue = null;
		new AssertConfig("")
			.assertParsedPropValueIs(expValue, propStr, Map.class);
	}

	///////////////////////////////////////////////
	// TEST HELPERS
	///////////////////////////////////////////////

	private File createTempPropFileWithContent(String content) throws Exception {
		File tempFile = File.createTempFile("testprops", ".properties");
		FileUtils.writeStringToFile(tempFile, content, Charset.defaultCharset());
		return tempFile;
	}

}
