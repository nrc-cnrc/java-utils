package ca.nrc.ui.commandline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

public class MockCommandLine extends CommandLine {
	
	String[] posArguments = new String[] {};
	Map<String,String> options = new HashMap<String,String>();

	public MockCommandLine() {
	}
	
	public MockCommandLine(String[] _posArguments, Map<String,String> _options) {
		this.posArguments = _posArguments;
		this.options = _options;		
	}
	
	public MockCommandLine addOption(String name, String value) {
		options.put(name, value);
		return this;
	}
	
	public MockCommandLine setPositionalArguments(String[] _posArgs) {
		this.posArguments = _posArgs;
		return this;
	}
	
	public String getOptionValue(String optName) {
		String value = null;
		if (options.containsKey(optName)) {
			value = options.get(optName);
		}
		return value;
	}
	
}
