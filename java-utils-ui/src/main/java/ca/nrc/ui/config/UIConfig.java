package ca.nrc.ui.config;
import java.io.File;

import ca.nrc.config.Config;
import ca.nrc.config.ConfigException;

public class UIConfig extends Config {
	
	public File getChromeDriverPath() throws ConfigException {
		String pathStr = getConfigProperty("ca.nrc.ui.test.chromedriver");
		File path = new File(pathStr);
		
		return path;
	}

	public String getBrowserName() throws ConfigException {
		String name = getConfigProperty("ca.nrc.ui.test.browsername", false);
		if (name == null) {
			name = "chrome";
		}
		
		return name;
	}

}
