package ca.nrc.config;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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

	@JsonIgnore
	public static String getConfigProperty(String propName) throws ConfigException {
		return getConfigProperty(propName, true);
	}
	
	
	@JsonIgnore
	public static String getConfigProperty(String propName, String defValue) throws ConfigException {
		String prop = getConfigProperty(propName, false);
		if (prop == null) {
			prop = defValue;
		}
	
		return prop;
	}

	
	@JsonIgnore
	public static String getConfigProperty(String propName, boolean failIfNoConfig) throws ConfigException {
		
		propName = convertToUndescore(propName);
		String prop = lookInEnvAndSystemProps(convertToUndescore(propName));
		if (prop == null) {
			prop = lookInPropFiles(propName);
		}
				
		if (prop == null && failIfNoConfig) {
			throw new ConfigException("No configuration property or environment variable '"+propName+"'");
		}
	
		return prop;
	}


	private static String convertToUndescore(String propName) {
		propName = propName.replaceAll("\\.+", "_");
		return propName;
	}


	private static String lookInEnvAndSystemProps(String propName) {
		String prop = System.getenv(propName);
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
				props.load(new FileReader(aPossibleFNamePath));
			} catch (IOException e) {
				throw new ConfigException("Could not open props file "+aPossibleFName+"="+aPossibleFNamePath, e);
			}
			prop = props.getProperty(propName);
			if (prop == null) {
				String propNameOtherFormat = changePropNameFormat(propName);
				prop = props.getProperty(propNameOtherFormat);
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
	
}
