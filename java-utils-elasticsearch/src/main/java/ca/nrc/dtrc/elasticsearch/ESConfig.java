package ca.nrc.dtrc.elasticsearch;

import ca.nrc.config.Config;
import ca.nrc.config.ConfigException;

public class ESConfig extends Config {
	
	private static String hostCached = null;
	private static String portCached = null;
	
	public static String host() throws ConfigException {
		if (hostCached == null) {
			hostCached = getConfigProperty("ca_nrc_javautils_elasticsearch_host", "localhost");
		}
		return hostCached;
	}

	public static int port() throws ConfigException {
		if (portCached == null) {
			portCached = getConfigProperty("ca_nrc_javautils_elasticsearch_port", "9200");
		}
		
		int port = -1;
		try {
			port = Integer.parseInt(portCached);
		} catch (Exception e) {
			throw new ConfigException("Port "+portCached+" must be an integer");
		}
		
		return port;
	}

}
