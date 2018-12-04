package ca.nrc.config;

import java.util.List;

import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class ConfigTest {
	
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
		AssertHelpers.assertStringEquals("Proerty name not correctly converted from '_' to '.' format",
				"ca.nrc.javautils.bingKey", Config.changePropNameFormat("ca_nrc_javautils_bingKey"));
		AssertHelpers.assertStringEquals("Proerty name not correctly converted from '.' to '_' format",
				"ca_nrc_javautils_bingKey", Config.changePropNameFormat("ca.nrc.javautils.bingKey"));
	}

	@Test(expected=ConfigException.class)
	public void test__changePropNameFormat__PropNameContainsBothDotsAndUndscores_ThrowsConfigException() throws Exception {
		String propName = "ca.nrc.javautils.some_prop";
		Config.changePropNameFormat(propName); // Should raise exception
	}
}
