package ca.nrc.dtrc.elasticsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		return port(null);
	}

	public static int port(Integer esVersion) throws ConfigException {
		Logger tLogger = LogManager.getLogger("ca.nrc.dtrc.elasticsearch.ESConfig.port");
		if (esVersion == null) {
			esVersion = 6;
		}
		String defaultPort = Integer.toString(9200);

		if (portCached == null) {
			portCached = getConfigProperty("ca.nrc.javautils.elasticsearch"+esVersion+".port", defaultPort);
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
