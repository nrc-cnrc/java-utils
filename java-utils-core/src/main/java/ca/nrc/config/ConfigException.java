package ca.nrc.config;

import java.io.IOException;

public class ConfigException extends Exception {
	public ConfigException(String mess) {
		super(mess);
	}

	public ConfigException(String mess, IOException e) {
		super(mess, e);
	}
}
