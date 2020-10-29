package ca.nrc.config;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**********************************************************************
 * 
 * Class that handles system configuration through a combination of
 * OS environment variables and Java properties.
 * 
 * The intent is to provide sysadmins with the ability to configure
 * the software without having to tinker with its internals.
 * 
 * Therefore:
 * 
 *   - System Environment variables take precedence over Java properties
 *     because the sysadmin may have no control over the -D options that
 *     the JVM is invoked with. But pretty much always has control over
 *     OS Environment variables.
 *     
 *   - Java properties take precedence over hard-coded values, because
 *     the sysadmin is more likely to have control over the former.
 *     Note that the reason why we provide the option of configuring through
 *     Java properties instead of OS environment variables, is that some
 *     Java containers only supporte the latter. 
 *      
**********************************************************************/

public class Config {
	private  static Map<Class<? extends Config>,Map<String,String>> propOverrides = new HashMap<Class<? extends Config>,Map<String,String>>();

	@JsonIgnore
	public static String getConfigProperty(String propName) throws ConfigException {
		return getConfigProperty(propName, null, String.class);
	}

	@JsonIgnore
	public static String getConfigProperty(String propName, String defValue) throws ConfigException {
		return getConfigProperty(propName, defValue, String.class);
	}


	@JsonIgnore
	public static String getConfigProperty(String propName, boolean failIfNoConfig) throws ConfigException {
		return getConfigProperty(propName, (String)null, String.class);
	}

	@JsonIgnore
	public static <T> T getConfigProperty(
			String propName, String defaultVal, Class<T> clazz) throws ConfigException {
		Logger tLogger = LogManager.getLogger("ca.nrc.config.Config.getConfigProperty");

		if (propName.contains("_")) {
			throw new ConfigException(
					"Bad property name: '"+propName+"'.\n"+
							"Property names should not include underscores.");
		}

		propName = convertToUndescore(propName);
		String propJson = null;


		if (propJson == null) {
			propJson = lookInEnvAndSystemProps(propName);
		}
		if (propJson == null) {
			propJson = lookInPropFiles(propName);
		}

		if (propJson == null) {
			if (defaultVal != null) {
				propJson = defaultVal;
			} else {
				throwConfigPropNotFound(propName);
			}
		}

		T prop = parsePropValue(propName, propJson, clazz);

		return prop;
	}

	private static void throwInvalidPropertyJson(String propName,
 		String propJson, Class clazz) throws InvalidPropJsonException {
		throw new InvalidPropJsonException(propName, propJson, clazz);
	}


	private static void throwConfigPropNotFound(String propName)
		throws ConfigPropNotFoundException {
		throw new ConfigPropNotFoundException(
			"No configuration property or environment variable '"+
			convertToDots(propName)+"'");
	}

	private static String convertToUndescore(String propName) {
		propName = propName.replaceAll("\\.+", "_");
		return propName;
	}

	private static String convertToDots(String propName) {
		propName = propName.replaceAll("\\_+", ".");
		return propName;
	}


	private static String lookInEnvAndSystemProps(String propName) {
		String propEnvVariable = propName.replaceAll("\\.", "_");
		String prop = System.getenv(propEnvVariable);
		if (prop == null) {
			prop = System.getProperty(propName);			
		}
		return prop;
	}


	private static String lookInPropFiles(String propName) throws ConfigException {
		String prop = null;
		
		List<String> possibleFNames = possiblePropFileNames(propName);
		for (String aPossibleFName: possiblePropFileNames(propName)) {
			String aPossibleFNamePath = lookInEnvAndSystemProps(aPossibleFName);
			if (aPossibleFNamePath == null) continue;
			Properties props = new Properties();
			try {
				FileReader fr = new FileReader(aPossibleFNamePath);
				props.load(fr);
				fr.close();
			} catch (IOException e) {
				throw new ConfigException("Problem reading props file "+aPossibleFName+"="+aPossibleFNamePath, e);
			}
			prop = props.getProperty(propName);
			if (prop == null) {
				String propNameOtherFormat = changePropNameFormat(propName);
				prop = props.getProperty(propNameOtherFormat);
			}
			if (prop != null) {
				break;
			}
		}
		
		return prop;
	}


	public static String changePropNameFormat(String propName) throws ConfigException {
		String propNameOtherFormat = null;
		
		if (propName.contains(".") && propName.contains("_")) {
			throw new ConfigException("Bad property name: "+propName+". Property names cannot contain both '.' and '_' at the same time");
		}
		if (propName.contains(".")) {
			propNameOtherFormat = propName.replaceAll("\\.", "_");
		} else {
			propNameOtherFormat = propName.replaceAll("_", ".");
		}
		return propNameOtherFormat;
	}


	public static List<String> possiblePropFileNames(String propName) {
		String[] parts = propName.split("_");
		String fName = "";
		List<String> possibleFNames = new ArrayList<String>();
		for (int ii=0; ii < parts.length-1; ii++) {
			String aPart = parts[ii];
			if (!fName.equals("")) fName += "_";
			fName += aPart;
			possibleFNames.add(fName);
		}
		Collections.reverse(possibleFNames);
		
		return possibleFNames;
	}
	
	protected static void overrideProperty(String prop, String value, Map<String, String> overridesRegistry) {
		prop = convertToUndescore(prop);
		overridesRegistry.put(prop, value);
	}
	
	protected static void unOverrideProperty(String prop, Map<String, String> overridesRegistry) {
		prop = convertToUndescore(prop);
		if (overridesRegistry.containsKey(prop)) {
			overridesRegistry.remove(prop);
		}
	}

	public static <T> T parsePropValue(
		String propName, String propJson, Class<T> clazz)
		throws ConfigException {
		if (clazz == String.class) {
			propJson = ensureStringValueIsQuoted(propJson);
		}
		ObjectMapper mapper = new ObjectMapper();
		T value = null;
		try {
			value = mapper.readValue(propJson, clazz);
		} catch (IOException e) {
			throwInvalidPropertyJson(propName, propJson, clazz);

			throw new ConfigException(
				"Could not parse property value as type "+clazz+"\n"+
				"Value was: '"+propJson+"'",
				e);
		}
		return value;
	}

	private static String ensureStringValueIsQuoted(String propStr) {
		if (!propStr.startsWith("\"")) {
			propStr = "\""+propStr;
		}
		if (!propStr.endsWith("\"")) {
			propStr = propStr + "\"";
		}
		return propStr;
	}
}
