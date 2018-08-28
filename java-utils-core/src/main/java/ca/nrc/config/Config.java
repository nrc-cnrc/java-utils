package ca.nrc.config;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class Config {

	@JsonIgnore
	public static String getConfigProperty(String propName) throws ConfigException {
		String prop = System.getProperty(propName);
		if (prop == null) {
			prop = System.getenv(propName);
		}
		
		if (prop == null) {
			throw new ConfigException("No configuration property or environment variable '"+propName+"'");
		}
	
		return prop;
	}
	
}
