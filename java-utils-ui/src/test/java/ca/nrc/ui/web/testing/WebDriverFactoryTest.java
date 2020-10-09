package ca.nrc.ui.web.testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WebDriverFactoryTest {

	@Before
	public void setUp() throws Exception {
		AssumeChromeDriverIsDefined.assume();
	}

	@After
	public void tearDown() throws Exception {
	}
	

	/****************************************
	 * DOCUMENTATION TESTS
	 ****************************************/
	
	@Test
	public void test__WebDriverFactory__Synopsis() throws Exception {
		
		// This is how you get a WebDriver instance.
		// By default, the returned instance will be a FirefoxDriver.
		
		// If you want a different kind of driver, you need to create a
		// file WebDriverFactoryConfig.json somewhere on the project's
		// source path, and put the following code in it:
		//
		// {"browserName": "chrome"}
		//
		// You can also set other propersties of the WebDriverFactory
		// in that JSON file (basically, any public non-static attribute
		// of the factory can be set that way)
		//
		WebDriverFactory.getDriver();
		
		//
		// By default, the browser will clase automatically once 
		// we exit the process.
		//
		// If you want to keep the browser open upon exit (for example
		// if you want to visually inspect the state of the browser after
		// a given failing test that you run on its own), you can set
		// the browserAutoClose property to true in the WebDriverFactoryConfig.json file:
		//

	}
	

}
