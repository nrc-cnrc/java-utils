package ca.nrc.config;

import java.io.IOException;

public class ConfigException extends Exception {
	public ConfigException(String mess) {
		super(mess);
	}

	public ConfigException(String mess, Exception e) {
		super(mess, e);
	}

	public ConfigException(Exception e) { super(e); }
}
