package ca.nrc.dtrc.elasticsearch;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ca.nrc.config.Config;
import ca.nrc.config.ConfigException;

public class ESConfig extends Config {
	
	private static String hostCached = null;
	private static String portCached = null;
	
	public static String host() throws ConfigException {
		if (hostCached == null) {
			hostCached = getConfigProperty("ca.nrc.javautils.elasticsearch.host", "localhost");
		}
		return hostCached;
	}

	public static int port() throws ConfigException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.ESConfig.port");
		if (portCached == null) {
			portCached = getConfigProperty("ca.nrc.javautils.elasticsearch.port", "9200");
		}
		
		int port = -1;
		try {
			port = Integer.parseInt(portCached);
		} catch (Exception e) {
			throw new ConfigException("Port "+portCached+" must be an integer");
		}
		
		tLogger.trace("** returning port="+port);
		
		return port;
	}

}
