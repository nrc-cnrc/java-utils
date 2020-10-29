package ca.nrc.config;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.nrc.testing.AssertString;
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

		// However, you can make getConfigProperty() return a default value
		//   if the prop is not found
		propValue =
			Config.getConfigProperty(
		"nonexistantPropName", "this is the default");

		// Note that if you pass null as the default value, getConfigProperty()
		//  assumes there is no default value. To set a property to a default
		//  value of null, do the following instead
		//
		try {
			propValue = Config.getConfigProperty("nonexistantPropName");
			Assert.fail("We should never make it here because the prop name does not exist");
		} catch (ConfigPropNotFoundException e) {
			propValue = null;
		}

		// You can use properties to store JSON serialisations of any data
		// type.
		// For example...
		//
		String propName = "com.acme.someint";
		environmentVariables.set(propName, "13");
//		Integer intPropVal = Config.getConfigProperty(propName, Integer.class);
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
