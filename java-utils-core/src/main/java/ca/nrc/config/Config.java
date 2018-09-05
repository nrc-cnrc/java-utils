package ca.nrc.config;

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
		String prop = System.getenv(propName);
		if (prop == null) {
			prop = System.getProperty(propName);			
		}
		
		if (prop == null) {
			prop = defValue;
		}
	
		return prop;
	}

	
	@JsonIgnore
	public static String getConfigProperty(String propName, boolean failIfNoConfig) throws ConfigException {
		String prop = System.getenv(propName);
		if (prop == null) {
			prop = System.getProperty(propName);			
		}
		
		if (prop == null && failIfNoConfig) {
			throw new ConfigException("No configuration property or environment variable '"+propName+"'");
		}
	
		return prop;
	}
	
}
