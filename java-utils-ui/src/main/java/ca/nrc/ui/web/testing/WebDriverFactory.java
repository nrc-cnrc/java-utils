package ca.nrc.ui.web.testing;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.nrc.file.ResourceGetter;


/*
 * A class used to generate properly configured WebDriver instances that can be used
 * to do acceptance testing of WebApps.
 * 
 * For details on how to use this class, see DOCUMENTATION TEST section of
 * WebDriverFactoryTest.
 */
public class WebDriverFactory {	
	private static WebDriver driverSingleton = null;
	private static WebDriverCloser driverCloser = null;
	
	//Public values can be set in WebDriverFactoryConfig.json
	public String browserName = "firefox";
	public String pathToChromeDriver = ".";
	public boolean driverAutoClose = true;

	public static WebDriver getDriver() {
		WebDriverFactory factory = getFactory();
		return factory.makeDriver();
	}
	
	public WebDriver makeDriver() {
		if (driverSingleton == null) {
			if (browserName.equals("chrome")) {
				driverSingleton = makeChromeDriver(pathToChromeDriver);
			} else if (browserName.equals("firefox")) {
				driverSingleton = makeFirefoxDriver();
			} else {
				throw new RuntimeException("Unknown browser name: "+browserName);
			}
			driverSingleton.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

			// Wrap the driver singleton into a "closer" that will
			// automatically close the driver when we exit the process
			if (driverAutoClose) {
				driverCloser = new WebDriverCloser(driverSingleton);
			}
		}
		

		
		return driverSingleton;
	}
	
	private static WebDriverFactory getFactory() {
		WebDriverFactory factory = configFactoryFromFile();
		if (factory == null) {
			factory = new WebDriverFactory();
		}
		return factory;
	}
	
	private static WebDriverFactory configFactoryFromFile() {

		//		System.out.println("-- configFromFile: invoked");
		WebDriverFactory factory = null;
		String configFPath = null;
		String configFName = "WebDriverFactoryConfig.json";
		try {
			configFPath = ResourceGetter.getResourcePath(configFName);
		} catch (IOException e) {
			// No tracer config file found. Just leave everything at their default values.
			System.out.println("Could not find a "+configFName +" config file on the source path.");
			throw new RuntimeException(e);
//			logger.info("-- logger: Could not find a "+configFName +" config file on the source path. Using default settings.");
		}

		if(configFPath!=null)
		{
			try {
				if (configFPath != null) {
					System.out.println("Configuring WebDriverFactory from file: "+configFPath);					
					ObjectMapper mapper = new ObjectMapper();
					File configFile = new File(configFPath);
					factory = mapper.readValue(configFile, WebDriverFactory.class);
				}
			} catch (IOException e) {
				System.out.println("Problem reading config file: "+configFPath+". Will be using default settings.\n"+e.getMessage());
			}
		}
		return factory;
	}
	
	private static WebDriver makeChromeDriver(String driverPath) {
		System.setProperty("webdriver.chrome.driver", driverPath);
		WebDriver driver = new ChromeDriver();
		return driver;
	}	
	
	private WebDriver makeFirefoxDriver() {
		WebDriver driver = new FirefoxDriver();
		
		// Don't think this is necessary actually.
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        return driver;
	}
	
	// Used to automatically close the WebDriver single instance
	// when we exit the process
	class WebDriverCloser {
		
		WebDriver wdInstance;
		
		// Note: Constructor is private because it's only meant
		// to be invoked internally by static method getDriver()
		private WebDriverCloser(WebDriver driverInstance) {
			this.wdInstance = driverInstance;
		}
		
		protected void finalize() throws Throwable {
			  // Invoke the finalizer of our superclass
			  // We haven't discussed superclasses or this syntax yet
			  super.finalize();

			  // Delete a temporary file we were using
			  // If the file doesn't exist or tempfile is null, this can throw
			  // an exception, but that exception is ignored. 
			  wdInstance.quit();
			}
	}

}
