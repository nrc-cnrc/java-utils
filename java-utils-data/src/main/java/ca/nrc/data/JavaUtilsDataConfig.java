package ca.nrc.data;

import ca.nrc.config.Config;
import ca.nrc.config.ConfigException;

public class JavaUtilsDataConfig  extends Config {
	
	public static String getBingKey() throws ConfigException {
		String key = getConfigProperty("ca.nrc.javautils.bingKey");
		return key;
	}

}
